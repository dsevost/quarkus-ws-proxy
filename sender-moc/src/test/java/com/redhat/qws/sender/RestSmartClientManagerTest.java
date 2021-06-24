package com.redhat.qws.sender;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.concurrent.TimeUnit;

import javax.json.bind.JsonbBuilder;
import javax.validation.Valid;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import com.redhat.qws.sender.model.MessageExchange;
import com.redhat.qws.sender.service.EventGenerator;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.RequestSpecification;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class RestSmartClientManagerTest {

    static final String rootPath = "/rest";
    static final String subscribe = rootPath + "/subscribe";
    static final String unsubscribe = rootPath + "/unsubscribe";

    private static final Logger LOGGER = Logger.getLogger(RestSmartClientManagerTest.class);

    @ConfigProperty(name = "rpc.protocol", defaultValue = "rest")
    String rpcProtocol;

    RequestSpecification defaultRequest() {
        return defaultRequest(Util.DEFAULT_USER_NAME);
    }

    RequestSpecification defaultRequest(String user) {
        return given().when().queryParam(RestSmartClientManagerAdapter.PRAMETER_NAME_USER, user)
                .queryParam(RestSmartClientManagerAdapter.PRAMETER_NAME_CLIENT_ID, Util.DEFAULT_CLIENT_ID)
                .queryParam(RestSmartClientManagerAdapter.PRAMETER_NAME_LEGACY_IP, Util.DEFAULT_LEGACY_IP);
    }

    @Test
    @Order(10)
    public void testNewClientSubscribe() {
        defaultRequest().get(subscribe).then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @Order(11)
    public void testExistingClientSubscribe() {
        defaultRequest().get(subscribe).then().statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Test
    @Order(20)
    public void testPeerReceiver() {
        assumeTrue(rpcProtocol.equalsIgnoreCase("rest"), () -> "REST is not testing");
        try {
            final MessageExchange mmx = Util.REST_MESSAGES.poll(100, TimeUnit.SECONDS);
            LOGGER.infof("::testPeerReceiver(%s)", mmx);
            assertNotNull(mmx);
            assertTrue(mmx.client.id.equals(Util.DEFAULT_CLIENT_ID));
            assertTrue(mmx.client.user.name.equals(Util.DEFAULT_USER_NAME));
            assertTrue(mmx.message.body.contains("message "));
            assertTrue(mmx.message.from.contains(EventGenerator.class.getSimpleName() + "@"));
        } catch (InterruptedException e) {
            LOGGER.debug("Error while wait message", e);
        }
    }

    @Test
    @Order(30)
    public void testExistingClientUnsubscribe() {
        defaultRequest().get(unsubscribe).then().statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    @Order(31)
    public void testNonExistingClientUnsubscribe() {
        defaultRequest(Util.DEFAULT_USER_NAME + "-!!!").get(unsubscribe).then()
                .statusCode(Response.Status.NO_CONTENT.getStatusCode());
    }

    @Path("proxy")
    public static class RestPeer {

        @POST
        @Path("server/receiver")
        public void receive(@Valid String mx) {
            LOGGER.debugf("RestPeer::Receive(%s)", mx);
            MessageExchange mmx = JsonbBuilder.create().fromJson(mx, MessageExchange.class);
            Util.REST_MESSAGES.add(mmx);
        }
    }
}