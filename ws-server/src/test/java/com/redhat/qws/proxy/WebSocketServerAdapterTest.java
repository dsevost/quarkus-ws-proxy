package com.redhat.qws.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import com.google.inject.Inject;
import com.redhat.qws.sender.GrpcSmartClientManagerResourceStub;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
@QuarkusTestResource(InfinispanServerTestResource.class)
public class WebSocketServerAdapterTest {
    private static final Logger LOGGER = Logger.getLogger(WebSocketServerAdapterTest.class);
    private static final LinkedBlockingDeque<String> MESSAGES = new LinkedBlockingDeque<>();

    @ClientEndpoint
    static class Client {
        @OnOpen
        public void open(Session session) {
            MESSAGES.add("CONNECT");
            session.getAsyncRemote().sendText("!!! READY !!!");
        }

        @OnClose
        public void onClose(Session session) {
            MESSAGES.add("DISCONNECT");
        }

        @OnMessage
        void message(String msg) {
            LOGGER.debugf("Client.class::onMessage(%s)", msg);
            MESSAGES.add(msg);
        }
    }

    @Inject
    RemoteCacheManager rcm;

    private static Session session;

    @ConfigProperty(name = "ws.connection.max-life-time", defaultValue = "100")
    int wsConnectionMaxLifeTime;

    @TestHTTPResource("/stream/u1/cid-1")
    URI uri;

    @TestHTTPResource("/stream/u1-already/cid-1")
    URI alreadyUri;

    // @Test
    // @Order(1)
    public void delay() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            LOGGER.debug(e);
        }
    }

    @Test
    @Order(10)
    public void testClientConnect() {
        try {
            session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri);
            waitFor(1000);
            String response = poll(10);
            LOGGER.infof("::testClientConnect() RESPONSE_CONNECT: %s ", response);
            assertEquals(response, "CONNECT");
            session.getAsyncRemote().sendText("!!! TEXT !!!");
            response = poll(10);
            LOGGER.infof("::testClientConnect() RESPONSE_subscribed: %s", response);
            assertRespond(response, "subscribed");
        } catch (Exception e) {
            LOGGER.info(e);
            session = null;
        }
    }

    @Test
    @Order(11)
    public void testAlreadyConnected() throws IOException, InterruptedException {
        Session s = null;
        try {
            s = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, alreadyUri);
            String response = poll(10);
            LOGGER.infof("::testAlreadyConnected() RESPONSE_CONNECT: %s ", response);
            assertEquals(response, "CONNECT");
            response = poll(10);
            LOGGER.infof("::testAlreadyConnected() RESPONSE_already: %s", response);
            assertTrue(response.contains("already"));
        } catch (Exception e) {
            LOGGER.info(e);
        } finally {
            if (s != null && s.isOpen()) {
                s.close();
                String response = poll(1);
                LOGGER.infof("::testAlreadyConnected() RESPONSE_DISONNECT: %s", response);
                assertEquals(response, "DISCONNECT");
                waitFor(1000);
            }
        }
    }

    @Test
    @Order(20)
    public void testWaitForMessage() {
        try {
            String response = poll(10);
            LOGGER.infof("::testWaitForMessage() RESPONSE_message: %s", response);
            assertRespond(response, "message -000-");
        } catch (Exception e) {
            LOGGER.info(e);
            session = null;
        }
    }

    @Test
    @Order(30)
    public void testDisconnect() {
        try {
            session.close();
            String response = poll(1);
            LOGGER.infof("::testDisconnect() RESPONSE_DISONNECT: %s", response);
            assertEquals(response, "DISCONNECT");
            // never happen
            // response = poll(10);
            // assertSubscriubtion(response, "subscribed");
            waitFor(1);
        } catch (Exception e) {
            LOGGER.info(e);
            session = null;
        }
    }

    @Test
    @Order(40)
    public void testDropConnect() {
        try {
            session = ContainerProvider.getWebSocketContainer().connectToServer(Client.class, uri);
            String response = poll(10);
            LOGGER.infof("::testDropConnect() RESPONSE_CONNECT: %s ", response);
            assertEquals(response, "CONNECT");
            response = poll(10);
            LOGGER.infof("::testDropConnect() RESPONSE_subscribed: %s", response);
            assertRespond(response, "subscribed");
            do {
                response = poll(wsConnectionMaxLifeTime + 10);
                LOGGER.infof("::testDropConnect() RESPONSE_DISONNECT: %s", response);
            } while (response.contains("message -000-"));
            assertEquals(response, "DISCONNECT");
            // give some time to propogate unsubscribe event
            waitFor(1000);
        } catch (Exception e) {
            LOGGER.info(e);
            session = null;
        }
    }

    String poll(int waitSeconds) throws InterruptedException {
        final String msg = MESSAGES.poll(waitSeconds, TimeUnit.SECONDS);
        // LOGGER.infof("Server pooled: %s", msg);
        return msg;
    }

    private static void assertRespond(String response, String subscribed) {
        assertNotNull(response);
        assertTrue(response.contains(subscribed));
        assertTrue(response.contains(GrpcSmartClientManagerResourceStub.class.getSimpleName()));
    }

    private static Object waiter = new Object();

    private static void waitFor(int millis) throws InterruptedException {
        synchronized (waiter) {
            waiter.wait(millis);
        }
    }

    static void notifier() {
        waiter.notifyAll();
    }
}
