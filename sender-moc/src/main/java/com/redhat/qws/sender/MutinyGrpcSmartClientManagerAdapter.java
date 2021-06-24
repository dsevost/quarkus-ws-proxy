package com.redhat.qws.sender;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.redhat.qws.model.grpc.Respond;
import com.redhat.qws.sender.grpc.MutinySmartClientGrpc;
import com.redhat.qws.sender.grpc.SmartClientContextWithIP;
import com.redhat.qws.sender.model.Message;
import com.redhat.qws.sender.service.MessageStore;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.smallrye.mutiny.Uni;

@Singleton
public class MutinyGrpcSmartClientManagerAdapter extends MutinySmartClientGrpc.SmartClientImplBase {
    // private static final Logger LOGGER = Logger.getLogger(MutinyGrpcSmartClientManagerAdapter.class);

    @Inject
    MessageStore store;

    @Inject
    MeterRegistry registry;

    @Override
    @Counted("smartclient_subscribe_mutiny_counter")
    @Timed("smartclient_subscribe_mutiny")
    public Uni<Respond> subscribe(SmartClientContextWithIP request) {
        return Uni.createFrom().item(() -> GrpcSmartClientManagerHelper.subscription(request, store, true,
                Message.from(this), GrpcSmartClientManagerHelper.MUTINY));
    }

    @Override
    @Counted("smartclient_unsubscribe_mutiny_counter")
    @Timed("smartclient_unsubscribe_mutiny")
    public Uni<Respond> unsubscribe(SmartClientContextWithIP request) {
        return Uni.createFrom().item(() -> GrpcSmartClientManagerHelper.subscription(request, store, false,
                Message.from(this), GrpcSmartClientManagerHelper.MUTINY));
    }
}
