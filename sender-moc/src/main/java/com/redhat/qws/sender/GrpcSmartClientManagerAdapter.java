package com.redhat.qws.sender;

import javax.inject.Inject;

import com.redhat.qws.model.grpc.Respond;
import com.redhat.qws.sender.grpc.SmartClientGrpc;
import com.redhat.qws.sender.model.Message;
import com.redhat.qws.sender.service.MessageStore;

import io.grpc.stub.StreamObserver;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;

// @Singleton
public class GrpcSmartClientManagerAdapter extends SmartClientGrpc.SmartClientImplBase {
    // private static final Logger LOGGER = Logger.getLogger(GrpcSmartClientManagerAdapter.class);

    @Inject
    MessageStore store;

    @Inject
    MeterRegistry registry;

    @Override
    @Counted("smartclient_subscribe_blocking_counter")
    @Timed("smartclient_subscribe_blocking")
    public void subscribe(com.redhat.qws.sender.grpc.SmartClientContextWithIP request,
            StreamObserver<Respond> observer) {

        Respond respond = GrpcSmartClientManagerHelper.subscription(request, store, true, Message.from(this),
                GrpcSmartClientManagerHelper.BLOCKING);
        observer.onNext(respond);
        observer.onCompleted();
    }

    @Override
    @Counted("smartclient_unsubscribe_blocking_counter")
    @Timed("smartclient_unsubscribe_blocking")
    public void unsubscribe(com.redhat.qws.sender.grpc.SmartClientContextWithIP request,
            StreamObserver<Respond> observer) {
                Respond respond = GrpcSmartClientManagerHelper.subscription(request, store, false, Message.from(this),
                GrpcSmartClientManagerHelper.BLOCKING);
        observer.onNext(respond);
        observer.onCompleted();
    }

    // private void subscription(boolean subscription,
    // com.redhat.qws.sender.grpc.SmartClientContextWithIP request,
    // StreamObserver<Respond> observer) {
    // final String cid = request.getClient().getClientId();
    // final String ip = request.getIp();
    // final String user = request.getClient().getUser().getName();
    // LOGGER.debugf("Got GRPC requset to subscribe client with parameters:
    // client[%s/%s], IP [%s]", user, cid, ip);
    // Respond.Builder b = Respond.newBuilder();
    // if ("".equals(cid) || "".equals(user) || "".equals(ip)) {
    // final String msg = String
    // .format("User name '%s', client ID '%s', IP address '%s' must not be null or
    // empty", user, cid, ip);
    // LOGGER.debug(msg, new RuntimeException(msg));
    // final com.redhat.qws.model.grpc.Message message =
    // com.redhat.qws.model.grpc.Message.newBuilder()
    // .setFrom(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()))
    // .setDate(System.currentTimeMillis()).setBody(msg).build();
    // b.setHttpReturnCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    // b.setMessage(message);
    // } else {
    // final boolean subscribed;
    // final SmartClientContext client = new SmartClientContext(cid, new
    // UserContext(user));
    // if (subscription) {
    // subscribed = store.subscribe(client, ip);
    // } else {
    // subscribed = store.unsubscribe(client, ip);
    // }
    // if (subscribed) {
    // final String msg = String.format("SmartClient(%s) for user(%s) with ip(%s)
    // %s", user, cid, ip,
    // subscription ? "subscribed" : "unsubscribed");
    // b.setHttpReturnCode(Response.Status.OK.getStatusCode());
    // final com.redhat.qws.model.grpc.Message message =
    // com.redhat.qws.model.grpc.Message.newBuilder()
    // .setFrom(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()))
    // .setDate(System.currentTimeMillis()).setBody(msg).build();
    // b.setMessage(message);

    // } else {
    // b.setHttpReturnCode(Response.Status.NO_CONTENT.getStatusCode());
    // }
    // }
    // observer.onNext(b.build());
    // observer.onCompleted();
    // }
}
