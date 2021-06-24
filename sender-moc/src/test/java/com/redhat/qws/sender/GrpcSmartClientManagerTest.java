package com.redhat.qws.sender;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import com.google.inject.Inject;
import com.redhat.qws.model.grpc.MessageExchange;
import com.redhat.qws.model.grpc.Respond;
import com.redhat.qws.sender.grpc.SmartClientGrpc;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.grpc.runtime.annotations.GrpcService;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class GrpcSmartClientManagerTest {
    private static final Logger LOGGER = Logger.getLogger(RestSmartClientManagerTest.class);
    private final String DEFAULT_CLIENT_ID = getClass().getSimpleName();

    @ConfigProperty(name = "quarkus.grpc.clients.smartclient.port", defaultValue = "9000")
    int quarkusGrpcPort;

    @ConfigProperty(name = "rpc.protocol", defaultValue = "rest")
    String rpcProtocol;

    @Inject
    @GrpcService("smartclient")
    SmartClientGrpc.SmartClientBlockingStub grpcSmartClient;

    @Test
    @Order(10)
    public void testNewClientSubscribeStub() {
        Respond reply = grpcSmartClient.subscribe(GrpcProxyServerAdapter
                .buildSmartClientContextWithIP(Util.DEFAULT_USER_NAME, DEFAULT_CLIENT_ID, Util.DEFAULT_LEGACY_IP));
        Util.assertResponse(reply, Response.Status.OK, "subscribed");
    }

    @Test
    @Order(11)
    public void testExistingClientSubscribeStub() {
        Respond reply = grpcSmartClient.subscribe(GrpcProxyServerAdapter
                .buildSmartClientContextWithIP(Util.DEFAULT_USER_NAME, DEFAULT_CLIENT_ID, Util.DEFAULT_LEGACY_IP));
        Util.assertResponse(reply, Response.Status.NO_CONTENT, "");
    }

    @Test
    @Order(20)
    public void testPeerReceiver() {
        assumeTrue(rpcProtocol.equalsIgnoreCase("grpc"), () -> "gRPC is not testing");
        try {
            final MessageExchange mmx = Util.GRPC_MESSAGES.poll(20, TimeUnit.SECONDS);
            LOGGER.infof("::testPeerReceiver(%s)", mmx);
            Util.assertMessageExchange(mmx, DEFAULT_CLIENT_ID);
        } catch (InterruptedException e) {
            LOGGER.debug("Error while wait message", e);
        }
    }

    @Test
    @Order(30)
    public void testUnsubscribeClientSubscribeStub() {
        Respond reply = grpcSmartClient.unsubscribe(GrpcProxyServerAdapter
                .buildSmartClientContextWithIP(Util.DEFAULT_USER_NAME, DEFAULT_CLIENT_ID, Util.DEFAULT_LEGACY_IP));
        Util.assertResponse(reply, Response.Status.OK, "unsubscribed");
    }

    @Test
    @Order(31)
    public void testUnsubscribeNonExistingClientSubscribeStub() {
        Respond reply = grpcSmartClient.unsubscribe(GrpcProxyServerAdapter.buildSmartClientContextWithIP(
                Util.DEFAULT_USER_NAME + "-!!!", DEFAULT_CLIENT_ID, Util.DEFAULT_LEGACY_IP));
        Util.assertResponse(reply, Response.Status.NO_CONTENT, "");
    }
}
