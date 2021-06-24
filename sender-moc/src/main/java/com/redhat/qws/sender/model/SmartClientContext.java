package com.redhat.qws.sender.model;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class SmartClientContext {
    @JsonbProperty("id")
    public final String id;

    @JsonbProperty("user")
    public final UserContext user;

    private final String key;

    @JsonbCreator
    public SmartClientContext(@NotBlank @JsonbProperty("id") String id,
            @NotNull @JsonbProperty("user") UserContext user) {
        this.id = id;
        this.user = user;
        key = getSmartClientKey(this);
    }

    public SmartClientContext(@NotBlank String id, @NotBlank String user) {
        this(id, new UserContext(user));
    }

    @JsonbTransient
    public String getSmartClientKey() {
        return key;
    }

    public static String getSmartClientKey(SmartClientContext client) {
        return getSmartClientKey(client.id, client.user.name);
    }

    public static String getSmartClientKey(String clientId, String user) {
        return clientId.hashCode() + ":" + user.hashCode();
    }

    @Override
    public String toString() {
        return String.format("{ id: %s, user: %s }", id, user);
    }
}
