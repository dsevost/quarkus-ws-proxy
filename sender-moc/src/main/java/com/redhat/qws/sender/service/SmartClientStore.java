package com.redhat.qws.sender.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.redhat.qws.sender.model.Message;
import com.redhat.qws.sender.model.SmartClientContext;

public class SmartClientStore {
    // private static final Logger LOGGER = Logger.getLogger(SmartClientStore.class);

    final SmartClientContext client;
    final List<Message> events = Collections.synchronizedList(new ArrayList<>());
    final Set<String> alive = Collections.synchronizedSet(new HashSet<>());

    public SmartClientStore(@NotNull SmartClientContext client) {
        this.client = client;
    }

    public void addEvent(@NotNull Message e) {
        events.add(e);
    }

    public boolean subscribe(@NotBlank String ip) {
        return alive.add(ip);
    }

    public boolean unsubscribe(@NotBlank String ip) {
        return alive.remove(ip);
    }

    public Set<String> getAlive() {
        return Collections.unmodifiableSet(alive);
    }

    public void storeMessage(Message message) {
        events.add(message);
    }

    public List<Message> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public String toString() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode());
    }
}
