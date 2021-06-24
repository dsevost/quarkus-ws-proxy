package com.redhat.qws.proxy.service;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.redhat.qws.model.grpc.Respond;
import com.redhat.qws.sender.grpc.MutinySmartClientGrpc;
import com.redhat.qws.sender.grpc.SmartClientContextWithIP;
import com.redhat.qws.sender.grpc.SmartClientGrpc;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.smallrye.mutiny.Uni;

@Singleton
public class SmartClientGrpcServiceStub {
    
    @Inject
    MeterRegistry registry;

    @Inject
    @GrpcService("smartclient")
    SmartClientGrpc.SmartClientBlockingStub grpcBlock;

    @Inject
    @GrpcService("smartclient")
    MutinySmartClientGrpc.MutinySmartClientStub grpcMutiny;

    @Counted("smartclient_connect_counter")
    @Timed("smartclient_connect")
    public Respond grpcBlockingSubscribe(String user, String cid, String ip) {
        Respond respond = grpcBlock.subscribe(prepareSmartClinetContextWitIP(user, cid, ip));
        return respond;
        // if (respond.getHttpReturnCode() == Response.Status.OK.getStatusCode()) {
        //     return respond.getMessage().getBody();
        // } else {
        //     throw new RuntimeException("Unexpected response: HTTP code: " + respond.getHttpReturnCode());
        // }
    }

    @Counted("smartclient_connect_counter")
    @Timed("smartclient_connect")
    public Uni<Respond> grpcMutinySubscribe(String user, String cid, String ip) {
        Uni<Respond> respond = grpcMutiny.subscribe(prepareSmartClinetContextWitIP(user, cid, ip));
        return respond;
    }

    @Counted("smartclient_disconnect_counter")
    @Timed("smartclient_disconnect")
    public Respond  grpcBlockingUnsubscribe(String user, String cid, String ip) {
        Respond respond = grpcBlock.unsubscribe(prepareSmartClinetContextWitIP(user, cid, ip));
        return respond;
        // if (respond.getHttpReturnCode() == Response.Status.OK.getStatusCode()) {
        //     return respond.getMessage().getBody();
        // } else {
        //     throw new RuntimeException("Unexpected response: HTTP code: " + respond.getHttpReturnCode());
        // }
    }

    @Counted("smartclient_disconnect_counter")
    @Timed("smartclient_disconnect")
    public Uni<Respond>  grpcMutinyUnsubscribe(String user, String cid, String ip) {
        Uni<Respond> respond = grpcMutiny.unsubscribe(prepareSmartClinetContextWitIP(user, cid, ip));
        return respond;
    }

    private SmartClientContextWithIP prepareSmartClinetContextWitIP(String user, String cid, String ip) {
        final com.redhat.qws.model.grpc.SmartClientContext.Builder clientBuilder = com.redhat.qws.model.grpc.SmartClientContext
                .newBuilder();
        final com.redhat.qws.model.grpc.UserContext.Builder userBuilder = com.redhat.qws.model.grpc.UserContext
                .newBuilder();
        clientBuilder.setClientId(cid).setUser(userBuilder.setName(user));
        return SmartClientContextWithIP.newBuilder().setClient(clientBuilder).setIp(ip).build();
    }
}
