package com.redhat.qws.sender;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import com.redhat.qws.model.grpc.Message;
import com.redhat.qws.model.grpc.MessageExchange;
import com.redhat.qws.model.grpc.Respond;
import com.redhat.qws.model.grpc.SmartClientContext;
import com.redhat.qws.model.grpc.UserContext;
import com.redhat.qws.proxy.grpc.WSProxyGrpc.WSProxyBlockingStub;
import com.redhat.qws.sender.grpc.SmartClientContextWithIP;

import org.jboss.logging.Logger;

import io.grpc.stub.StreamObserver;
import io.quarkus.grpc.runtime.annotations.GrpcService;

@Singleton
public class TestGrpcSmartClientManagerResourceStub extends com.redhat.qws.sender.grpc.SmartClientGrpc.SmartClientImplBase {
    private static final Logger LOGGER = Logger.getLogger(TestGrpcSmartClientManagerResourceStub.class);

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Inject
    @GrpcService("ws-proxy")
    WSProxyBlockingStub wsProxy;

    public void subscribe(SmartClientContextWithIP request, StreamObserver<Respond> observer) {
        subscription(true, request, observer);
    }

    @Override
    public void unsubscribe(SmartClientContextWithIP request, StreamObserver<Respond> observer) {
        subscription(false, request, observer);
    }

    private Message newMessage(String body) {
        return Message.newBuilder().setFrom(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()))
                .setDate(System.currentTimeMillis()).setBody(body).build();
    }

    private MessageExchange newMMX(String clientId, String user, String messageBody) {
        return MessageExchange.newBuilder().setMessage(newMessage(messageBody)).setClient(SmartClientContext
                .newBuilder().setClientId(clientId).setUser(UserContext.newBuilder().setName(user).build()).build())
                .build();
    }

    private void subscription(boolean subscription, SmartClientContextWithIP request,
            StreamObserver<Respond> observer) {
        final String cid = request.getClient().getClientId();
        final String ip = request.getIp();
        final String user = request.getClient().getUser().getName();
        LOGGER.debugf("Got GRPC requset to %s client with parameters: client[%s/%s], IP [%s]",
                (subscription ? "SUBSCRIBE" : "UNSUBSCRIBE"), user, cid, ip);
        Respond.Builder b = Respond.newBuilder();
        if ("".equals(cid) || "".equals(user) || "".equals(ip)) {
            final String msg = String
                    .format("User name '%s', client ID '%s', IP address '%s' must not be null or empty", user, cid, ip);
            LOGGER.debug(msg, new RuntimeException(msg));
            final Message message = newMessage(msg);
            b.setHttpReturnCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            b.setMessage(message);
        } else {
            final boolean subscribed = user.contains("already");
            LOGGER.debugf("user.contains('already'): %s, %s", user, user.contains("already"));
            if (subscribed) {
                b.setHttpReturnCode(Response.Status.NO_CONTENT.getStatusCode());
                // b.setMessage(Message.newBuilder().setFrom("STUB").setBody("BODY").setDate(10000000000L));
            } else {
                final String msg = String.format("SmartClient(%s) for user(%s) with ip(%s) %s", user, cid, ip,
                        subscription ? "subscribed" : "unsubscribed");
                b.setHttpReturnCode(Response.Status.OK.getStatusCode());
                final Message message = newMessage(msg);
                b.setMessage(message);
                scheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        final MessageExchange mmx = newMMX(cid, user, "message -000-");
                        LOGGER.infof("::subscribe()::Runnable()::run() [%s]", mmx);
                        wsProxy.handleMessage(mmx);
                    }
                }, 5, TimeUnit.SECONDS);
            }
        }
        LOGGER.debugf("Sending response [%s]", b);
        observer.onNext(b.build());
        observer.onCompleted();
    }
}
