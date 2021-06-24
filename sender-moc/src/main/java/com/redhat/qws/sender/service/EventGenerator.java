package com.redhat.qws.sender.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.qws.proxy.grpc.WSProxyGrpc.WSProxyBlockingStub;
import com.redhat.qws.sender.model.Message;
import com.redhat.qws.sender.model.MessageExchange;
import com.redhat.qws.sender.model.SmartClientContext;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class EventGenerator {

    private static final Logger LOGGER = Logger.getLogger(EventGenerator.class);

    @Inject
    MessageStore store;

    @Inject
    @RestClient
    RestWSProxyService restWsp;

    @Inject
    @GrpcService("ws-proxy")
    WSProxyBlockingStub grpcWsp;

    @Inject
    MeterRegistry registry;

    @ConfigProperty(name = "rpc.protocol", defaultValue = "rest")
    String rpcProtocol;

    boolean peerAvailability = false;

    int count = 0;

    @Scheduled(every = "{scheduler.every}")
    void generateEvent() {
        final SmartClientContext context = store.getRandom();
        if (context == null) {
            return;
        }
        final Message message = new Message(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()),
                "message " + (count++));
        store.storeMessage(context, message);
        if (isPeerEndpointUnreachable()) {
            return;
        }
        try {
            for (String ip : store.getSmartClientStore(context).getAlive()) {
                LOGGER.debugf("Sending message [%s] to client [%s]@[%s] via '%s'", message, context, ip, rpcProtocol);
                registry.counter("proxy_send_message_counter").increment();
                registry.timer("proxy_send_message").record(() -> {
                    if (rpcProtocol.equalsIgnoreCase("GRPC")) {
                        grpcWspSend(context, message);
                    } else {
                        restWspSend(context, message);
                    }
                });
            }
        } catch (Exception e) {
            setPeerEndpointUnreachable();
            LOGGER.warnf("WSProxyService unreachable: [%s]/[%s]", e, e.getCause().getMessage());
            LOGGER.debugf(e, "WSProxyService Endpoint [%s] unreachable", grpcWsp);
        }
    }

    void grpcWspSend(SmartClientContext client, Message message) {
        final com.redhat.qws.model.grpc.SmartClientContext.Builder clientBuilder = com.redhat.qws.model.grpc.SmartClientContext
                .newBuilder();
        final com.redhat.qws.model.grpc.Message.Builder messageBuilder = com.redhat.qws.model.grpc.Message.newBuilder();
        final com.redhat.qws.model.grpc.UserContext.Builder userBuilder = com.redhat.qws.model.grpc.UserContext
                .newBuilder();
        messageBuilder.setBody(message.body).setDate(message.date.getTime()).setFrom(message.from);
        clientBuilder.setClientId(client.id).setUser(userBuilder.setName(client.user.name));
        grpcWsp.handleMessage(com.redhat.qws.model.grpc.MessageExchange.newBuilder().setClient(clientBuilder)
                .setMessage(messageBuilder).build());
    }

    void restWspSend(SmartClientContext client, Message message) {
        restWsp.send(new MessageExchange(client, message));
    }

    @Scheduled(every = "60s")
    synchronized void handleHostUnreachable() {
        peerAvailability = false;
    }

    synchronized void setPeerEndpointUnreachable() {
        peerAvailability = true;
    }

    synchronized boolean isPeerEndpointUnreachable() {
        return peerAvailability;
    }
}
