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
}
