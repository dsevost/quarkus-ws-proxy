package com.redhat.qws.proxy.model;

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
    public String getSmartCleintKey() {
        // if (key == null) {
        //     key = getSmartClientKey(this);
        // }
        return key;
    }

    public static String getSmartClientKey(@NotNull SmartClientContext client) {
        return getSmartClientKey(client.user.name, client.id);
    }

    public static String getSmartClientKey(@NotBlank String user, @NotBlank String clientId) {
        return user.hashCode() + ":" + clientId.hashCode();
    }

    @Override
    public String toString() {
        return String.format("{ id: %s, user: %s }", id, user);
    }
}
