package com.redhat.qws.sender.model;

import java.util.Date;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class Message {
    @JsonbProperty("from")
    public final String from;

    @JsonbProperty("date")
    public final Date date;

    @JsonbProperty("body")
    public final String body;
    
    @JsonbCreator
    public Message(@NotBlank @JsonbProperty("from") String from, @JsonbProperty("date") Date date, @JsonbProperty("body") String body) {
        this.from = from;
        this.body = body;
        this.date = date;
    }
    
    public Message(@NotBlank String from, @NotBlank String body) {
        this.from = from;
        this.body = body;
        this.date = new Date();
    }

    @Override
    public String toString() {
        return String.format("{ from: %s, date: %s, body: %s }", from, date, body);
    }

    public static String from(@NotNull Object thiz) {
        return new StringBuilder(32).append(thiz.getClass().getSimpleName()).append('@').append(Integer.toHexString(thiz.hashCode())).toString();
    }

    public static Message newFrom(Object thiz, @NotBlank String body) {
        return new Message(from(thiz), body);
    }
}
