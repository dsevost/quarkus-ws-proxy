package com.redhat.qws.proxy.service;

import java.util.concurrent.CompletableFuture;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.redhat.qws.proxy.model.Message;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class DatagridService {
    private static final Logger LOGGER = Logger.getLogger(DatagridService.class);

    @ConfigProperty(name = "datagrid.cache.name", defaultValue = "quarkus")
    String cacheName;

    @ConfigProperty(name = "datagrid.use", defaultValue = "false")
    boolean datagridUsage;

    @ConfigProperty(name = "datagrid.debug.listener", defaultValue = "false")
    boolean debugListener;

    @ConfigProperty(name = "datagrid.cache.force-create", defaultValue = "false")
    boolean cacheCreateForce;

    @Inject
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

    void onStart(@Observes StartupEvent ev) {
        if (cacheCreateForce) {
            LOGGER.infof("Trying to create cache: %s (datagrid.cache.force-create = true)", cacheName);
            cache = rcm.administration().getOrCreateCache(cacheName, DefaultTemplate.DIST_SYNC);
            LOGGER.debugf("RemoteCacheManager configuration: %s", rcm.getConfiguration());
        } else {
            if (datagridUsage) {
                if (rcm == null) {
                    RuntimeException e = new RuntimeException(
                            "Property 'datagrid.use' set to 'true', but RemoteCache is NULL, it should never happaened in prod, just local testing enviroment case");
                    LOGGER.error("Configuration error", e);
                    throw e;
                } else {
                    cache = rcm.getCache(cacheName);
                    if (cache == null) {
                        throw new RuntimeException("Remote cache does not exist on server: " + cacheName);
                    }
                }
            }
        }
        if (debugListener && datagridUsage) {
            cache.addClientListener(new EventPrintListener());
        }
    }

    RemoteCache<String, CachedMessage> getCache() {
        if (cache == null) {
            if (rcm == null) {
                RuntimeException e = new RuntimeException("Remote Cache Manager is NULL");
                LOGGER.warn(e);
            }
            cache = rcm.getCache(cacheName);
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
