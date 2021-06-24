package com.redhat.qws.sender.model;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.validation.constraints.NotNull;

public class MessageExchange {
    @JsonbProperty("smart-client")
    public final SmartClientContext client;

    @JsonbProperty("message")
    public final Message message;

    @JsonbCreator
    public MessageExchange(@NotNull @JsonbProperty("smart-client") SmartClientContext client,
            @NotNull @JsonbProperty("message") Message message) {
        this.client = client;
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("{ client: %s, message: %s }", client, message);
    }
}
