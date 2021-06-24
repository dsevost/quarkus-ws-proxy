package com.redhat.qws.sender.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.enterprise.context.ApplicationScoped;
import javax.validation.constraints.NotNull;

import com.redhat.qws.sender.model.Message;
import com.redhat.qws.sender.model.SmartClientContext;

import org.jboss.logging.Logger;

@ApplicationScoped
public class MessageStore {
    private static final Logger LOGGER = Logger.getLogger(MessageStore.class);

    final Map<String, SmartClientStore> messages = Collections.synchronizedMap(new HashMap<>());

    public boolean subscribe(SmartClientContext client, String ip) {
        LOGGER.debugf("Subscribing SmartClient [%s] with key [%s] for IP [%s]", client, client.getSmartClientKey(), ip);
        final boolean subscribed = getSmartClientStore(client).subscribe(ip);
        if (subscribed) {
            LOGGER.infof("SmartClient with key [%s] subscribed with IP [%s]", client.getSmartClientKey(), ip );
        } else {
            LOGGER.warnf("SmartClient with key [%s:%s/%s] already subscribed with IP [%s], new subscription ignored", client.user.name, client.id, client.getSmartClientKey(), ip );
        }
        return subscribed;
    }

    public boolean unsubscribe(SmartClientContext client, String ip) {
        LOGGER.debugf("Unsubscribing SmartClient [%s] with key [%s] for IP [%s]", client, client.getSmartClientKey(), ip);
        final boolean unsubscribed = getSmartClientStore(client).unsubscribe(ip);
        if (unsubscribed) {
            LOGGER.infof("SmartClient with key [%s] unsubscribed with IP [%s]", client.getSmartClientKey(), ip );
        } else {
            LOGGER.warnf("Subscription for SmartClient with key [%s:%s/%s] and IP [%s] not found, request ignored", client.user.name, client.id, client.getSmartClientKey(), ip );
        }
        return unsubscribed;
    }

    public void storeMessage(SmartClientContext client, Message message) {
        LOGGER.debugf("Store message [%s] for SmartClient [%s]", client, message);
        getSmartClientStore(client).storeMessage(message);

    }

    public SmartClientContext getRandom() {
        final Random random = new Random();
        final int size = messages.size();
        if (size < 1) {
            LOGGER.debug("No clients are registered yet");
            return null;
        }
        final int position = random.nextInt(size);
        final String key = messages.keySet().toArray(new String[size])[position];
        final SmartClientStore store = messages.get(key);
        if( store == null) {
            LOGGER.warnf("SmartClientSore not found for key: [%]", key);
            return null;
        }
        return store.client;
    }

    SmartClientStore getSmartClientStore(@NotNull SmartClientContext client) {
        final SmartClientStore store;
        final String key = client.getSmartClientKey();
        LOGGER.debugf("Looking for SmartClient [%s]", key);
        if (messages.containsKey(key)) {
            store = messages.get(key);
            LOGGER.debugf("Found existing SmartClient [%s]", store);
        } else {
            store = new SmartClientStore(client);
            messages.put(key, store);
            LOGGER.debugf("Registering new SmartClient [%s]", store);
        }
        return store;
    }
}
