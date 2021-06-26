package com.redhat.qws.proxy;

import java.util.Date;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.Response;

import com.redhat.qws.model.grpc.MessageExchange;
import com.redhat.qws.model.grpc.Respond;
import com.redhat.qws.proxy.grpc.WSProxyGrpc;
import com.redhat.qws.proxy.model.Message;
import com.redhat.qws.proxy.service.DatagridService;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.grpc.stub.StreamObserver;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.StartupEvent;

@Singleton
public class GrpcProxyServerAdapter extends WSProxyGrpc.WSProxyImplBase {
    private static final Logger LOGGER = Logger.getLogger(GrpcProxyServerAdapter.class);

    @ConfigProperty(name = "datagrid.use", defaultValue = "false")
    boolean datagridUsage;

    @Inject
    MeterRegistry registry;

    @Inject
    DatagridService datagrid;

    @Inject
    WebSocketServerAdapter wsServer;

    void onStart(@Observes StartupEvent ev) {
        if (datagridUsage) {
            LOGGER.info("Using datagrid");
        } else {
            LOGGER.infof("Datagrid will not be used, to use it set property '%s' to true", "datagrid.use");
        }
    }

    @Override
    @Counted("quarkus_wsserver_handle_message_blocking_counter")
    @Timed("quarkus_wsserver_handle_message_blocking")
    public void handleMessage(MessageExchange request, StreamObserver<Respond> responseObserver) {
        final String clientId = request.getClient().getClientId();
        final String user = request.getClient().getUser().getName();
        Respond.Builder b = Respond.newBuilder();
        if ("".equals(clientId) || "".equals(user)) {
            LOGGER.warnf("Client id '%s' or user name '%s' must not be null or empty", clientId, user);
            responseObserver.onNext(b.setHttpReturnCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).build());
        } else {
            final Message message = new Message(request.getMessage().getFrom(),
                    new Date(request.getMessage().getDate()), request.getMessage().getBody());
            LOGGER.debugf("Handling message [%s]", message);
            if (datagridUsage) {
                datagrid.storeAsync(user, clientId, message)
                        .thenAccept(reply -> LOGGER.debugf("Message [%s] stored to datagrid asynchronously", message));
            } else {
                final String key = com.redhat.qws.proxy.model.SmartClientContext.getSmartClientKey(user, clientId);
                wsServer.send(key, message);
            }
            responseObserver.onNext(b.setHttpReturnCode(Response.Status.OK.getStatusCode()).build());
        }
        responseObserver.onCompleted();
    }
}
