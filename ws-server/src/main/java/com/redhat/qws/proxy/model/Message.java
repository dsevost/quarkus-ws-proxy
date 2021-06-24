package com.redhat.qws.proxy.model;

import java.util.Date;

import javax.json.bind.JsonbBuilder;
import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.validation.constraints.NotBlank;

public class Message {

    @JsonbProperty("from")
    public final String from;

    @JsonbProperty("date")
    public final Date date;

    @JsonbProperty("body")
    public final String body;

    @JsonbCreator
    public Message(@NotBlank @JsonbProperty("from") String from, @JsonbProperty("date") Date date,
            @JsonbProperty("body") String body) {
        this.from = from;
        this.body = body;
        this.date = date;
    }

    public Message(@NotBlank String from, @NotBlank String body) {
        this(from, new Date(), body);
    }

    public Message(@NotBlank String from, long datetime, @NotBlank String body) {
        this(from, new Date(datetime), body);
    }

    public String toJson() {
        return JsonbBuilder.create().toJson(this);
    }

    @Override
    public String toString() {
        return String.format("{ from: %s, date: %s, body: %s }", from, date, body);
    }

    public static Message from(Object obj, String body) {
        return new Message(obj.getClass().getSimpleName() + "@" + Integer.toHexString(obj.hashCode()), body);
    }
}
