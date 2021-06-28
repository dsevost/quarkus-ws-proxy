package com.redhat.qws.proxy.service;

import java.util.concurrent.CompletableFuture;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import com.redhat.qws.proxy.model.Message;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.configuration.ClientIntelligence;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.infinispan.commons.dataconversion.MediaType;
import org.jboss.logging.Logger;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
@Startup
public class DatagridService {
    private static final Logger LOGGER = Logger.getLogger(DatagridService.class);

    @ConfigProperty(name = "datagrid.cache.name", defaultValue = "quarkus")
    String cacheName;

    @ConfigProperty(name = "datagrid.use", defaultValue = "false")
    boolean datagridUsage;

    @ConfigProperty(name = "datagrid.debug.listener", defaultValue = "false")
    boolean debugListener;

    @ConfigProperty(name = "quarkus.infinispan-client.auth-realm", defaultValue = "default")
    String authRealm;

    @ConfigProperty(name = "quarkus.infinispan-client.auth-server-name", defaultValue = "localhost")
    String authServerName;

    @ConfigProperty(name = "quarkus.infinispan-client.auth-username", defaultValue = "developer")
    String authUser;

    @ConfigProperty(name = "quarkus.infinispan-client.auth-password", defaultValue = "developer")
    String authPassword;

    @ConfigProperty(name = "quarkus.infinispan-client.client-intelligence", defaultValue = "BASIC")
    String clientIntelligence;

    @ConfigProperty(name = "quarkus.infinispan-client.sasl-mechanism", defaultValue = "SCRAM-SHA-256")
    String saslMech;

    @ConfigProperty(name = "quarkus.infinispan-client.server-list", defaultValue = "localhost:11222")
    String serverList;

    @ConfigProperty(name = "quarkus.infinispan-client.trust-store", defaultValue = "trustStore.jks")
    String trustStore;

    @ConfigProperty(name = "quarkus.infinispan-client.trust-store-password", defaultValue = "changeme")
    String trustStorePassword;

    @ConfigProperty(name = "quarkus.infinispan-client.trust-store-type", defaultValue = "jks")
    String trustStoreType;

    // @javax.inject.Inject
    RemoteCacheManager rcm;

    RemoteCache<String, CachedMessage> cache = null;

    public void store(String user, String clientId, Message message) {
        final CachedMessage cachedMessage = new CachedMessage(user, clientId, message);
        getCache().put(cachedMessage.getKey(), cachedMessage);
    }

    public CompletableFuture<CachedMessage> storeAsync(String user, String clientId, Message message) {
        final CachedMessage cachedMessage = new CachedMessage(user, clientId, message);
        return getCache().putAsync(cachedMessage.getKey(), cachedMessage);
    }

    public CompletableFuture<CachedMessage> getAsync(String key) {
        return getCache().getAsync(key);
    }

    public CompletableFuture<CachedMessage> removeAsync(String key) {
        LOGGER.debugf("Removing message(key=[%s]) from datagrid", key);
        return getCache().removeAsync(key);
    }

    public void register(Object listener) {
        getCache().addClientListener(listener);
    }

    public void unregister(Object listener) {
        getCache().removeClientListener(listener);
    }

    void onStart(@Observes StartupEvent ev) throws InterruptedException {
        if (datagridUsage == false) {
            LOGGER.infof("Datagrid will not be used, to use it set property '%s' to true", "datagrid.use");
            return;
        }
        if (rcm == null) {
            getRemoteCacheManager();
        }
        if (cache == null) {
            org.infinispan.configuration.cache.ConfigurationBuilder cb = new org.infinispan.configuration.cache.ConfigurationBuilder();
            cb.encoding().mediaType(MediaType.APPLICATION_PROTOSTREAM_TYPE).build();
            try {
                cache = rcm.administration().getOrCreateCache(cacheName, cb.build());
            } catch (Exception e) {
                LOGGER.error("Error while starting RemoteCacheManager", e);
                throw e;
            }
        }
        if (debugListener) {
            cache.addClientListener(new EventPrintListener());
        }
    }

    private synchronized RemoteCacheManager getRemoteCacheManager() {
        if (rcm != null) {
            return rcm;
        }
        final ConfigurationBuilder cb = new ConfigurationBuilder();
        ClientIntelligence ci = ClientIntelligence.getDefault();
        switch (clientIntelligence) {
            case "BASIC":
                ci = ClientIntelligence.BASIC;
                break;
            case "TOPOLOGY_AWARE":
                ci = ClientIntelligence.TOPOLOGY_AWARE;
                break;
        }
        cb.addContextInitializer(new InfinispanMessageContextInitializerImpl()).addServers(serverList);
        cb.security().authentication().realm(authRealm).serverName(authServerName).username(authUser)
                .password(authPassword).saslMechanism(saslMech).clientIntelligence(ci);
        cb.security().ssl().trustStoreFileName(trustStore).trustStorePassword(trustStorePassword.toCharArray())
                .trustStoreType(trustStoreType);
        try {
            rcm = new RemoteCacheManager(cb.build(), true);
        } catch (Exception e) {
            LOGGER.fatalf(e, "Error while connect to Datagrid [%s]", cb.build());
            Quarkus.blockingExit();
        }
        LOGGER.infof("HotRod Client: [%s]", rcm);
        return rcm;
    }

    private RemoteCache<String, CachedMessage> getCache() {
        if (cache == null) {
            cache = getRemoteCacheManager().getCache(cacheName);
        }
        return cache;
    }

    @ClientListener
    static class EventPrintListener {

        @ClientCacheEntryCreated
        public void handleCreatedEvent(ClientCacheEntryCreatedEvent<String> e) {
            LOGGER.debugf("Entity(%s) created", e.getKey());
        }

        @ClientCacheEntryModified
        public void handleModifiedEvent(ClientCacheEntryModifiedEvent<String> e) {
            LOGGER.debugf("Entity(%s) modified, it should not be happened!!!", e.getKey());
        }

        @ClientCacheEntryRemoved
        public void handleRemovedEvent(ClientCacheEntryRemovedEvent<String> e) {
            LOGGER.debugf("Entity(%s) successfully handled, removing from cache", e.getKey());
        }
    }
}
