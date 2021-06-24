package com.redhat.qws.proxy.service;

import com.redhat.qws.proxy.model.Message;

import org.infinispan.protostream.annotations.ProtoField;

public class CachedMessage {
    private String user;

    private String clientId;

    private String from;

    private long datetime;

    private String body;

    public CachedMessage(String user, String clientId, Message message) {
        this(user, clientId, message.from, message.date.getTime(), message.body);
    }

    public CachedMessage() {
    }

    // @ProtoFactory()
    public CachedMessage(String user, String clientId, String from, long datetime, String body) {
        this.clientId = clientId;
        this.user = user;
        this.from = from;
        this.datetime = datetime;
        this.body = body;
    }

    public String getKey() {
        final StringBuffer b = new StringBuffer(64).append(user.hashCode()).append(':').append(clientId.hashCode())
                .append(':').append(datetime);
        return b.toString();
    }

    @Override
    public String toString() {
        return String.format("{ from: %s, date: %s, body %s }", from, datetime, body);
    }

    @ProtoField(number = 1)
    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @ProtoField(number = 2)
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @ProtoField(number = 3)
    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    @ProtoField(number = 4, defaultValue = "0")
    public long getDatetime() {
        return datetime;
    }

    public void setDatetime(long datetime) {
        this.datetime = datetime;
    }

    @ProtoField(number = 5)
    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
