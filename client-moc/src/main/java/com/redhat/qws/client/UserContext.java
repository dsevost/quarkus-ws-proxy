package com.redhat.qws.client;

import javax.json.bind.annotation.JsonbProperty;
import javax.validation.constraints.NotEmpty;

public class UserContext {

    @JsonbProperty("name")
    public final String name;

    public UserContext(@NotEmpty String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("{ name: %s }", name);
    }
}
