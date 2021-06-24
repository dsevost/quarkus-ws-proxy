package com.redhat.qws.sender;

import java.util.Date;

import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import com.redhat.qws.model.grpc.MessageExchange;
import com.redhat.qws.model.grpc.Respond;
import com.redhat.qws.model.grpc.SmartClientContext;
import com.redhat.qws.model.grpc.UserContext;
import com.redhat.qws.proxy.grpc.WSProxyGrpc.WSProxyImplBase;
import com.redhat.qws.sender.grpc.SmartClientContextWithIP;
import com.redhat.qws.sender.model.Message;

import org.jboss.logging.Logger;

import io.grpc.stub.StreamObserver;

@Singleton
class GrpcProxyServerAdapter extends WSProxyImplBase {
    private static final Logger LOGGER = Logger.getLogger(RestSmartClientManagerTest.class);

    @Override
    public void handleMessage(MessageExchange request, StreamObserver<Respond> responseObserver) {
        final Message message = new Message(request.getMessage().getFrom(), new Date(request.getMessage().getDate()),
                request.getMessage().getBody());
        LOGGER.debugf("Handling message [%s]", message);
        Util.GRPC_MESSAGES.add(request);
        responseObserver.onNext(Respond.newBuilder().setHttpReturnCode(Response.Status.OK.getStatusCode()).build());
        responseObserver.onCompleted();
    }

    static SmartClientContextWithIP buildSmartClientContextWithIP(String user, String clientId, String ip) {
        SmartClientContext.Builder sc = SmartClientContext.newBuilder();
        sc.setClientId(clientId).setUser(UserContext.newBuilder().setName(user));
        SmartClientContextWithIP.Builder scIp = SmartClientContextWithIP.newBuilder();
        scIp.setClient(sc.build()).setIp(ip);
        return scIp.build();
    }
}
