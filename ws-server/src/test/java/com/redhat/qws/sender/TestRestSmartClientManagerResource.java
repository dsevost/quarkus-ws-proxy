package com.redhat.qws.sender;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import com.redhat.qws.proxy.model.Message;
import com.redhat.qws.proxy.model.MessageExchange;
import com.redhat.qws.proxy.model.SmartClientContext;
import com.redhat.qws.proxy.model.UserContext;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

@Path("rest")
// @ApplicationScoped
public class TestRestSmartClientManagerResource {

    static final String PRAMETER_NAME_USER = "user";
    static final String PRAMETER_NAME_CLIENT_ID = "cid";
    static final String PRAMETER_NAME_LEGACY_IP = "ip";

    private static final Logger LOGGER = Logger.getLogger(TestRestSmartClientManagerResource.class);

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @RegisterRestClient(configKey = "ws-proxy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("proxy")
    public interface RestWSProxyService {
        @POST
        @Path("server/receiver")
        void send(@Valid MessageExchange m);
    }

    @Inject
    @RestClient
    RestWSProxyService wsProxy;

    @GET
    @Path("subscribe")
    public Response subscribe(@QueryParam(PRAMETER_NAME_USER) String user,
            @QueryParam(PRAMETER_NAME_CLIENT_ID) String cid, @QueryParam(PRAMETER_NAME_LEGACY_IP) String ip) {
        LOGGER.debugf("Got REST requset to subscribe client with parameters: client[%s/%s], IP [%s]", user, cid, ip);
        LOGGER.debugf("user.contains('already'): %s, %s", user, user.contains("already"));
        if (user.contains("already")) {
            return Response.ok().status(Response.Status.NO_CONTENT).build();
        } else {
            scheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    final SmartClientContext client = new SmartClientContext(cid, new UserContext(user));
                    final MessageExchange mmx = new MessageExchange(client,
                            new Message(TestRestSmartClientManagerResource.class.getSimpleName(), "message -000-"));
                    LOGGER.infof("Thread(%s)::subscribe()::Runnable()::run() [%s]", Thread.currentThread().getName(), mmx);
                    wsProxy.send(mmx);
                }
            }, 1, TimeUnit.SECONDS);
            return Response.ok(new Message(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()),
                    String.format("SmartClient(%s) for user(%s) subscribed", cid, user))).build();
        }
    }

    @GET
    @Path("unsubscribe")
    public Response unsubscribe(@QueryParam(PRAMETER_NAME_USER) String user,
            @QueryParam(PRAMETER_NAME_CLIENT_ID) String cid, @QueryParam(PRAMETER_NAME_LEGACY_IP) String ip) {
        LOGGER.debugf("Got REST requset to unsubscribe client with parameters: client[%s/%s], IP [%s]", user, cid, ip);
        final ResponseBuilder response = Response
                .ok(new Message(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()),
                        String.format("SmartClient(%s) for user(%s) unsubscribed", cid, user)));
        return response.build();
    }
}
