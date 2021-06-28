package com.redhat.qws.sender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.LinkedBlockingDeque;

import javax.ws.rs.core.Response;

import com.redhat.qws.model.grpc.MessageExchange;
import com.redhat.qws.model.grpc.Respond;
import com.redhat.qws.sender.service.EventGenerator;

public class Util {
    static final LinkedBlockingDeque<MessageExchange> GRPC_MESSAGES = new LinkedBlockingDeque<>();
    static final LinkedBlockingDeque<com.redhat.qws.sender.model.MessageExchange> REST_MESSAGES = new LinkedBlockingDeque<>();

    static final String DEFAULT_USER_NAME = "user1";
    static final String DEFAULT_CLIENT_ID = "cid-1234567890";
    static final String DEFAULT_LEGACY_IP = "127.0.0.1";

    static final String DEFAULT_MESSAGE = "message ";

    static void assertResponse(Respond reply, Response.Status status, String expected) {
        assertTrue(reply.getHttpReturnCode() == status.getStatusCode());
        final String body = reply.getMessage().getBody();
        if (expected == null) {
            assertNull(reply.getMessage().getBody());
        } else if ("".equals(expected)) {
            assertEquals("", body);
        } else {
            assertTrue(body.contains(expected));
        }
    }

    static void assertMessageExchange(MessageExchange mmx, String clientId) {
        assertNotNull(mmx);
        assertTrue(mmx.getClient().getClientId().equals(clientId));
        assertTrue(mmx.getClient().getUser().getName().equals(DEFAULT_USER_NAME));
        assertTrue(mmx.getMessage().getBody().contains(DEFAULT_MESSAGE));
        assertTrue(mmx.getMessage().getFrom().contains(EventGenerator.class.getSimpleName() + "@"));
    }
}
