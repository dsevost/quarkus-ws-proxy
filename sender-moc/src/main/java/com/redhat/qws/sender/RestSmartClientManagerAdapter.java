package com.redhat.qws.sender;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.redhat.qws.sender.model.Message;
import com.redhat.qws.sender.model.MessageExchange;
import com.redhat.qws.sender.model.SmartClientContext;
import com.redhat.qws.sender.model.UserContext;
import com.redhat.qws.sender.service.MessageStore;

import org.jboss.logging.Logger;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;

@Path("rest")
public class RestSmartClientManagerAdapter {

    static final String PRAMETER_NAME_USER = "user";
    static final String PRAMETER_NAME_CLIENT_ID = "cid";
    static final String PRAMETER_NAME_LEGACY_IP = "ip";

    private static final Logger LOGGER = Logger.getLogger(RestSmartClientManagerAdapter.class);

    @Inject
    MeterRegistry registry;

    @Inject
    MessageStore store;

    @GET
    @Path("subscribe")
    @Counted("rest_subscribe_counter")
    @Timed("rest_subscribe")
    public Response subscribe(@QueryParam(PRAMETER_NAME_USER) String user,
            @QueryParam(PRAMETER_NAME_CLIENT_ID) String cid, @QueryParam(PRAMETER_NAME_LEGACY_IP) String ip) {
        LOGGER.debugf("Got REST requset to subscribe client with parameters: client[%s/%s], IP [%s]", user, cid, ip);
        final SmartClientContext client = new SmartClientContext(cid, new UserContext(user));
        final ResponseBuilder response = Response
                .ok(new Message(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()),
                        String.format("SmartClient(%s) for user(%s) subscribed", cid, user)));
        if (store.subscribe(client, ip)) {
            return response.build();
        } else {
            return response.status(Response.Status.NO_CONTENT).build();
        }

    }

    @GET
    @Path("unsubscribe")
    @Counted("rest_unsubscribe_counter")
    @Timed("rest_unsubscribe")
    public Response unsubscribe(@QueryParam(PRAMETER_NAME_USER) String user,
            @QueryParam(PRAMETER_NAME_CLIENT_ID) String cid, @QueryParam(PRAMETER_NAME_LEGACY_IP) String ip) {
        LOGGER.debugf("Got REST requset to unsubscribe client with parameters: client[%s/%s], IP [%s]", user, cid, ip);
        final SmartClientContext client = new SmartClientContext(cid, new UserContext(user));
        final ResponseBuilder response = Response
                .ok(new Message(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()),
                        String.format("SmartClient(%s) for user(%s) unsubscribed", cid, user)));
        if (store.unsubscribe(client, ip)) {
            return response.build();
        } else {
            return response.status(Response.Status.NO_CONTENT).build();
        }
    }

    @POST
    @Path("store")
    public Response store(@Valid MessageExchange mx) {
        LOGGER.debugf("Received MessageExchange form WSProxyServer: [%s]", mx);
        store.storeMessage(mx.client, mx.message);
        return Response.ok().build();
    }
}
