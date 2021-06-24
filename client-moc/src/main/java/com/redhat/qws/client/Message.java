package com.redhat.qws.client;

import java.util.Date;

import javax.json.bind.annotation.JsonbProperty;
import javax.validation.constraints.NotBlank;

public class Message {
    @JsonbProperty("from")
    public final String from;

    @JsonbProperty("date")
    public final Date date;

    @JsonbProperty("body")
    public final String body;

    public Message(@NotBlank String from, @NotBlank String body) {
        this.from = from;
        this.body = body;
        this.date = new Date();
    }

    @Override
    public String toString() {
        return String.format("{ from: %s, date: %s, body: %s }", from, date, body);
    }
}
