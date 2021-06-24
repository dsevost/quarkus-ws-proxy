package com.redhat.qws.client;

import javax.json.bind.annotation.JsonbProperty;
import javax.json.bind.annotation.JsonbTransient;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class SmartClientContext {
    @JsonbProperty("id")
    public final String id;

    @JsonbProperty("user")
    public final UserContext user;

    final String key;

    public SmartClientContext(@NotBlank String id, @NotNull UserContext user) {
        this.id = id;
        this.user = user;
        key = getSmartClientKey(this);
    }

    public SmartClientContext(@NotBlank String id, @NotBlank String user) {
        this(id, new UserContext(user));
    }

    @JsonbTransient
    public String getSmartCleintKey() {
        return key;
    }

    public static String getSmartClientKey(SmartClientContext client) {
        return client.id.hashCode() + ":" + client.user.name.hashCode();
    }
}
