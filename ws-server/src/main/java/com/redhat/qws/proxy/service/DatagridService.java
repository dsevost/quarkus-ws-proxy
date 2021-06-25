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
        if (datagridUsage == false) {
            LOGGER.infof("Datagrid will not be used, to use it set property '%s' to true", "datagrid.use");
            return;
        }
        cache = getCache();
        if (cache == null) {
            cache = rcm.administration().createCache(cacheName, DefaultTemplate.DIST_SYNC);
        }
        if (debugListener) {
            cache.addClientListener(new EventPrintListener());
        }
    }

    private RemoteCache<String, CachedMessage> getCache() {
        if (cache == null) {
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
