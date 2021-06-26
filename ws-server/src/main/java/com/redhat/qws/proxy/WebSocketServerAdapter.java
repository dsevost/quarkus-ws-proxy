package com.redhat.qws.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.JsonbBuilder;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.ws.rs.core.Response;

import com.redhat.qws.model.grpc.Respond;
import com.redhat.qws.proxy.model.Message;
import com.redhat.qws.proxy.model.SmartClientContext;
import com.redhat.qws.proxy.service.CachedMessage;
import com.redhat.qws.proxy.service.DatagridService;
import com.redhat.qws.proxy.service.SmartClientGrpcServiceStub;
import com.redhat.qws.proxy.service.SmartClientRestService;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.jboss.logging.Logger;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
@ServerEndpoint("/stream/{user}/{cid}")
public class WebSocketServerAdapter {

    private static final Logger LOGGER = Logger.getLogger(WebSocketServerAdapter.class);

    private Map<String, Session> sessions = new ConcurrentHashMap<>();
    private Map<Session, Date> lifeTime = new ConcurrentHashMap<>();

    @Inject
    MeterRegistry registry;

    @Inject
    @RestClient
    SmartClientRestService restScs;

    @Inject
    SmartClientGrpcServiceStub grpcScs;

    @Inject
    DatagridService datagrid;

    @ConfigProperty(name = "datagrid.use", defaultValue = "false")
    boolean datagridUsage;

    @ConfigProperty(name = "rpc.protocol", defaultValue = "rest")
    String rpcProtocol;

    @ConfigProperty(name = "downward-api.env.my-ip", defaultValue = "")
    String POD_IP;

    @ConfigProperty(name = "ws.connection.max-life-time", defaultValue = "900")
    long wsConnectionMaxLifeTime;

    private String MY_IP = null;

    private boolean peerAvailability = false;

    private DataCapture dc;

    @PostConstruct
    void init() {
        if (datagridUsage) {
            LOGGER.info("Using datagrid");
            dc = new DataCapture();
            datagrid.register(dc);
        } else {
            LOGGER.infof("Datagrid will not be used, to use it set property '%s' to true", "datagrid.use");
        }
    }

    @PreDestroy
    void destroy() {
        // it's done automatically by Quarkus, just for sending justification to client
        for (Session s : sessions.values()) {
            try {
                LOGGER.debugf("Open session founs [%s], shutting it down gracefully", s.getRequestURI());
                s.close(new CloseReason(CloseCodes.NORMAL_CLOSURE,
                        String.format("Instance of %s is going to shutdown, please reconnect", this)));
            } catch (IOException e) {
                LOGGER.debugf("Websocker session close error on instance %s shutdown", this);
            }
        }
        if (datagridUsage) {
            datagrid.unregister(dc);
        }
    }

    @ClientListener
    class DataCapture {
        @ClientCacheEntryCreated
        public void handleCreatedEvent(ClientCacheEntryCreatedEvent<String> e) {
            final String KEY = e.getKey();
            LOGGER.debugf("Entity(%s) created", KEY);
            // is client session present on this instance
            final String clientKey = KEY.substring(0, KEY.lastIndexOf(':'));
            if (sessions.get(clientKey) == null) {
                LOGGER.debugf("Got notification not for my client %s, ignoring", clientKey);
                return;
            } else {
                LOGGER.debugf("Processing message [%s] for my client, client key is: [%s]", KEY, clientKey);
            }
            CompletableFuture<CachedMessage> cm = datagrid.getAsync(KEY);
            cm.thenAcceptAsync(create -> {
                LOGGER.debugf("Datagrid returned (CREATE): %s ", create);
                final String key = SmartClientContext.getSmartClientKey(create.getUser(), create.getClientId());
                final Message message = new Message(create.getFrom(), create.getDatetime(), create.getBody());
                LOGGER.warnf("(%s) Transfering message [%s] to [%s]", getClass().getSimpleName(), message, clientKey);
                send(key, message);
                datagrid.removeAsync(e.getKey())
                        .thenAccept(remove -> LOGGER.debugf("Datagrid returned (REMOVE): %s ", remove));
            });
        }
    }

    @OnOpen
    @Counted("quarkus_wsserver_client_connect_counter")
    @Timed("quarkus_wsserver_client_connect")
    public void clientConnect(Session session, @PathParam("user") String user, @PathParam("cid") String cid) {
        LOGGER.infof("Connect: smartClient id [%s], user [%s]", cid, user);
        final String message = "Backend not available, try again later";
        if (isPeerEndpointUnreachable()) {
            try {
                session.close(new CloseReason(CloseCodes.TRY_AGAIN_LATER, message));
            } catch (IOException e) {
                LOGGER.debugf(e, "Error while closing session [%s]", session.getRequestURI());
            }
            return;
        }
        final String key = SmartClientContext.getSmartClientKey(user, cid);
        sessions.put(key, session);
        lifeTime.put(session, new Date());
        if (rpcProtocol.equalsIgnoreCase("grpc")) {
            grpcScs.grpcMutinySubscribe(user, cid, getRuntimeIP()).onCancellation().invoke(() -> {
                send(session, Message.from(this, "The downstream does not want our items anymore!"));
            }).subscribe().with(reply -> handleSubscribeResponseLambda(reply, session), failure -> {
                setPeerEndpointUnreachable();
                LOGGER.fatal("SmartClientGrpcServiceStub error", failure);
                send(session, Message.from(this, message + ", reason: " + failure.getMessage()));
            });
        } else {
            try {
                String msg = restSubscribe(user, cid);
                if (msg == null || msg.equals("")) {
                    send(session, Message.from(this, "You've already been registered"));
                } else {
                    send(session, JsonbBuilder.create().fromJson(msg, Message.class));
                }
            } catch (Exception e) {
                setPeerEndpointUnreachable();
                LOGGER.debug("SmartClientRestService error", e);
                send(session, Message.from(this, message + ", reason: " + e.getMessage()));
            }
        }
        session.addMessageHandler(PongMessage.class,
                pong -> LOGGER.debugf("PONG received from client(%s): %s", session.getId(), pong.getApplicationData()));
    }

    void handleSubscribeResponseLambda(Respond reply, Session session) {
        final Message m;
        final int retCode = reply.getHttpReturnCode();
        if (retCode == Response.Status.OK.getStatusCode()) {
            LOGGER.debugf("MESSAGE: %s", reply.getMessage());
            m = new Message(reply.getMessage().getFrom(), new Date(reply.getMessage().getDate()),
                    reply.getMessage().getBody());
        } else if (retCode == Response.Status.NO_CONTENT.getStatusCode()) {
            m = Message.from(this, "You've already been registered");
        } else {
            m = Message.from(this, "Unexpected error (http code): " + retCode);
        }
        send(session, m);
    }

    @OnClose
    @Counted("quarkus_wsserver_client_disconnect_counter")
    @Timed("quarkus_wsserver_client_disconnect")
    public void clientDisconnect(Session session, @PathParam("user") String user, @PathParam("cid") String cid) {
        LOGGER.infof("Disconnect: smartClient id [%s], user [%s]", cid, user);
        if (isPeerEndpointUnreachable()) {
            return;
        }
        final String key = SmartClientContext.getSmartClientKey(user, cid);
        sessions.remove(key);
        lifeTime.remove(session);
        if (rpcProtocol.equalsIgnoreCase("grpc")) {
            grpcScs.grpcMutinyUnsubscribe(user, cid, getRuntimeIP()).subscribe().with((reply) -> {
                if (reply.getHttpReturnCode() == Response.Status.OK.getStatusCode()) {
                    LOGGER.debugf("MESSAGE: %s", reply.getMessage());
                } else {
                    LOGGER.debugf("Subscription not foud for [%s] ", new SmartClientContext(user, cid));
                }
            });
        } else {
            try {
                send(session, Message.from(this, restUnsubscribe(user, cid)));
            } catch (Exception e) {
                setPeerEndpointUnreachable();
                LOGGER.debug("SmartClientRestService error", e);
            }
        }
    }

    @OnMessage
    public void onMessage(Session session, String msg) {
        LOGGER.infof("ServerWS::onMessage(%s)", msg);
    }

    @OnError
    @Counted("quarkus_wsserver_on_error_counter")
    public void onError(Session session, @PathParam("user") String user, @PathParam("cid") String cid, Throwable t) {
        LOGGER.warnf("Connection error: [%s] - smartClient id [%s], user [%s], close connection", t.getMessage(), cid,
                user);
        LOGGER.debugf(t, "Connection error: smartClient id [%s], user [%s], close connection", cid, user);
        final String key = SmartClientContext.getSmartClientKey(user, cid);
        sessions.remove(key);
        lifeTime.remove(session);
        try {
            synchronized (session) {
                if (isPeerEndpointUnreachable()) {
                    final String message = "Backend not available, try again later";
                    session.close(new CloseReason(CloseCodes.TRY_AGAIN_LATER, message));
                } else {
                    session.close(new CloseReason(CloseCodes.UNEXPECTED_CONDITION, t.getMessage()));
                }
            }
            if (rpcProtocol.equalsIgnoreCase("REST")) {
                LOGGER.warnf("REST unsubscribe: %d", restUnsubscribe(user, cid));
            } else {
                grpcScs.grpcMutinyUnsubscribe(user, cid, getRuntimeIP()).subscribe().with(reply -> {
                    LOGGER.debugf("GrpcMutiny unsubscribe: %s", reply);
                }, failure -> {
                    setPeerEndpointUnreachable();
                    LOGGER.warn("SmartClientGrpcServiceStub error", failure);
                });
            }
        } catch (IOException e) {
            LOGGER.debugf(e, "::onError(%s)", session.getId());
        }
    }

    void send(@NotBlank String key, @NotNull Message mx) {
        send(sessions.get(key), mx);
    }

    void send(/* @NotNull */Session session, @NotNull Message message) {
        if (session == null) {
            if (datagridUsage) {
                LOGGER.warnf("WebSocket session is already null or not found, MESSAGE [%s] SAVED by datagrid", message);
            } else {
                LOGGER.warnf("WebSocket session is already null or not found, MESSAGE [%s] LOST!", message);
            }
            return;
        }
        synchronized (session) {
            RemoteEndpoint.Async remote = session.getAsyncRemote();
            if (session.isOpen() != true || remote == null) {
                if (datagridUsage) {
                    LOGGER.warnf("Session closed [%s] or its remote peer is null [%s], MESSAGE [%s] SAVED by datagrid ",
                            session, remote, message);
                } else {
                    LOGGER.warnf("Session closed [%s] or its remote peer is null [%s], MESSAGE [%s] LOST! ", session,
                            remote, message);
                }
                return;
            }
            remote.sendObject(message.toJson(), result -> {
                if (result.getException() != null) {
                    LOGGER.warnf("Unable to send message: %s", result.getException());
                }
            });
        }
    }

    String restSubscribe(String user, String cid) {
        return restScs.subscribe(user, cid, getRuntimeIP());
    }

    String restUnsubscribe(String user, String cid) {
        return restScs.unsubscribe(user, cid, getRuntimeIP());
    }

    @Scheduled(every = "{scheduler.every}")
    void checkConnectionAiveAndLifetimeEceeded() throws IOException {
        LOGGER.debugf("Connection pruner started for every %s(sec)",
                ConfigProvider.getConfig().getValue("scheduler.every", String.class));
        final Date prunerStart = new Date();
        long connectionPruned = 0;
        final long maxTime = wsConnectionMaxLifeTime * 1000;
        final long connectionsTotal = sessions.size();
        for (Session session : sessions.values()) {
            synchronized (session) {
                final Date opened = lifeTime.get(session);
                final long alive = (new Date()).getTime() - opened.getTime();
                if (alive > maxTime) {
                    if (session.isOpen()) {
                        LOGGER.debugf("Session [%s] is too old %d, when maximux lifetime is %d, trying to close",
                                session.getRequestURI(), alive / 1000, wsConnectionMaxLifeTime);
                        session.close(new CloseReason(CloseCodes.SERVICE_RESTART, "Connection max lifetime reached"));
                        lifeTime.remove(session);
                        connectionPruned++;
                    }
                } else {
                    // will fail on browsers
                    final ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
                    buf.putLong(alive);
                    session.getAsyncRemote().sendPing(buf);
                }
            }
        }
        final String msg = String.format("Connections pruned: %d of %d, time spent(ms): %d", connectionPruned,
                connectionsTotal, new Date().getTime() - prunerStart.getTime());
        LOGGER.log(connectionPruned > 0 ? Logger.Level.WARN : Logger.Level.INFO, msg);
    }

    synchronized String getRuntimeIP() {
        if (MY_IP != null) {
            return MY_IP;
        }
        String myIP = "127.0.0.1";
        if (POD_IP == null || POD_IP.equals("") || POD_IP.equals("\"\"") || POD_IP.equals("''")
                || POD_IP.equals("127.0.0.1") || POD_IP.length() < 7) {
            LOGGER.debugf("POD_IP env not set or loopback [%s], trying to get first address", POD_IP);
            try {
                myIP = InetAddress.getLocalHost().getHostAddress();
                LOGGER.debugf("MY_IP [%s]", myIP);
            } catch (UnknownHostException e) {
                LOGGER.debug("Error occured while host address resolved, fallback to 127.0.0.1", e);
            }
        } else {
            // OpenShift downward API
            LOGGER.debugf("Downward API: POD_IP [%s]", POD_IP);
            myIP = POD_IP;
        }
        MY_IP = myIP;
        return myIP;
    }

    @Scheduled(every = "60s")
    synchronized void handleHostUnreachable() {
        peerAvailability = false;
    }

    synchronized void setPeerEndpointUnreachable() {
        peerAvailability = true;
    }

    synchronized boolean isPeerEndpointUnreachable() {
        return peerAvailability;
    }
}
