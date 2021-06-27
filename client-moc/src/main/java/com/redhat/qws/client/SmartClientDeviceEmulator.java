package com.redhat.qws.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.json.bind.JsonbBuilder;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.jboss.logging.Logger;

public class SmartClientDeviceEmulator implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(SmartClientDeviceEmulator.class);

    @ClientEndpoint
    class MessageStreamer {
        @OnMessage
        void onMessage(String message) {
            if (message.contains("subscribed")) {
                LOGGER.debugf("server message: for client(%s/%s) [%s]", clientId, user.name, message);
            } else {
                Message msg = JsonbBuilder.create().fromJson(message, Message.class);
                LOGGER.warnf("Delivery time %sms - Message [%s] for client(%s/%s)", System.currentTimeMillis() - msg.date.getTime(), message, clientId, user.name);
            }
        }

        @OnOpen
        void onOpen(Session session) {
            LOGGER.debugf("::onOpen(), time spent(ms): %s", (new Date()).getTime() - aliveTime.getTime());
            session.getAsyncRemote().sendObject(new Message(clientId, "hello"));
        }

        @OnError
        void onError(Session session, Throwable t) {
            LOGGER.debug("::onError()", t);
            setRestart(true);
        }

        @OnClose
        void onClose(Session session) {
            // try {
            // start();
            // } catch (DeploymentException | IOException e) {
            // LOGGER.debugf(e,
            // "Error occured while start new WS-session for client [%s], user [%s],
            // continue in background",
            // clientId, user.name);
            setRestart(true);
            // }
        }
    }

    private final ScheduledExecutorService scheduler;

    private final long wsConnectionMaxLifeTime;

    private final String clientId;
    private final UserContext user;

    private boolean restart = false;
    private Object restartLock = new Object();
    private Session session = null;

    private URI wsProxy;
    private Date aliveTime;

    SmartClientDeviceEmulator(@NotBlank String clientId, @NotNull UserContext user, @NotBlank String wsProxyEndpoint,
            long wsConnectionMaxLifeTime, ScheduledExecutorService scheduler) throws URISyntaxException {
        if (clientId == null) {
            this.clientId = "Client-" + Long.toHexString(this.hashCode());
        } else {
            this.clientId = clientId;
        }
        this.user = user;
        this.scheduler = scheduler;
        wsProxy = new URI(wsProxyEndpoint + "/stream/" + this.user.name + "/" + this.clientId);
        this.scheduler.scheduleWithFixedDelay(this, 10, 10, TimeUnit.SECONDS);
        aliveTime = null;
        this.wsConnectionMaxLifeTime = wsConnectionMaxLifeTime;
        LOGGER.debugf("SmartClient id(%s), user(%s), URI(%s), connection MAX_LIFE_TIME(%s)", this.clientId, user,
                wsProxy, wsConnectionMaxLifeTime);
    }

    public SmartClientDeviceEmulator(@NotNull UserContext user, @NotBlank String wsProxyEndpoint,
            long wsConnectionMaxLifeTime, ScheduledExecutorService scheduler) throws URISyntaxException {
        this(null, user, wsProxyEndpoint, wsConnectionMaxLifeTime, scheduler);
    }

    public void start() throws DeploymentException, IOException {
        aliveTime = new Date();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        session = container.connectToServer(new MessageStreamer(), wsProxy);
        setRestart(false);
        session.addMessageHandler(PongMessage.class,
                pong -> LOGGER.debugf("PONG received from server(%s): %s", session.getId(), pong.getApplicationData()));

    }

    public void stop() throws IOException {
        setRestart(false);
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } finally {
            session = null;
        }
        aliveTime = null;
    }

    void setRestart(boolean value) {
        synchronized (restartLock) {
            restart = value;
        }
    }

    boolean isRestart() {
        return restart;
    }

    public void run() {
        final Date now = new Date();
        final long alive = now.getTime() - aliveTime.getTime();
        if (alive > (wsConnectionMaxLifeTime * 1000)) {
            LOGGER.infof("It's time to restart session(%s), seconds of alive: %s", session.getRequestURI(),
                    alive / 1000);
            try {
                stop();
                start();
            } catch (IOException | DeploymentException e) {
                LOGGER.debugf("Error while scheduled session restart: %s", e.getMessage());
                setRestart(true);
            }
        }
        synchronized (restartLock) {
            if (restart == false) {
                final ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
                buf.putLong(alive);
                try {
                    session.getAsyncRemote().sendPing(buf);
                } catch (Exception e) {
                    setRestart(true);
                }
                return;
            }
            try {
                start();
            } catch (DeploymentException | IOException e) {
                LOGGER.debug("Exception occured while start", e);
            }
        }
    }
}
