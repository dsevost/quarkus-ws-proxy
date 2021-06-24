package com.redhat.qws.sender;

import javax.ws.rs.core.Response;

import com.redhat.qws.model.grpc.Message;
import com.redhat.qws.model.grpc.Respond;
import com.redhat.qws.sender.grpc.SmartClientContextWithIP;
import com.redhat.qws.sender.model.SmartClientContext;
import com.redhat.qws.sender.model.UserContext;
import com.redhat.qws.sender.service.MessageStore;

import org.jboss.logging.Logger;

class GrpcSmartClientManagerHelper {
    private static final Logger LOGGER = Logger.getLogger(GrpcSmartClientManagerHelper.class);

    static final String MUTINY = "Mutiny";
    static final String BLOCKING = "Blocking";

    static Respond subscription(SmartClientContextWithIP request, MessageStore store, boolean subscription, String from,
            String debugMutiny) {
        final String cid = request.getClient().getClientId();
        final String ip = request.getIp();
        final String user = request.getClient().getUser().getName();
        LOGGER.debugf("Got GRPC/%s requset to %s client with parameters: client[%s/%s], IP [%s]", debugMutiny,
                subscription ? "subscribe" : "unsubscribe", user, cid, ip);
        Respond.Builder b = Respond.newBuilder();
        if ("".equals(cid) || "".equals(user) || "".equals(ip)) {
            final String msg = String
                    .format("User name '%s', client ID '%s', IP address '%s' must not be null or empty", user, cid, ip);
            LOGGER.debug(msg, new RuntimeException(msg));
            final Message message = Message.newBuilder().setFrom(from).setDate(System.currentTimeMillis()).setBody(msg)
                    .build();
            b.setHttpReturnCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
            b.setMessage(message);
        } else {
            final boolean subscribed;
            final SmartClientContext client = new SmartClientContext(cid, new UserContext(user));
            if (subscription) {
                subscribed = store.subscribe(client, ip);
            } else {
                subscribed = store.unsubscribe(client, ip);
            }
            if (subscribed) {
                final String msg = String.format("SmartClient(%s) for user(%s) %s", user, cid,
                        subscription ? "subscribed" : "unsubscribed");
                b.setHttpReturnCode(Response.Status.OK.getStatusCode());
                final Message message = Message.newBuilder().setFrom(from).setDate(System.currentTimeMillis())
                        .setBody(msg).build();
                b.setMessage(message);

            } else {
                b.setHttpReturnCode(Response.Status.NO_CONTENT.getStatusCode());
            }
        }
        return b.build();
    }
}
