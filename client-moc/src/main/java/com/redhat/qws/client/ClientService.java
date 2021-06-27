package com.redhat.qws.client;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.enterprise.context.ApplicationScoped;
import javax.websocket.DeploymentException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.scheduler.Scheduled;

@ApplicationScoped
public class ClientService {

    private static final Logger LOGGER = Logger.getLogger(ClientService.class);
    private Set<SmartClientDeviceEmulator> devices = Collections.synchronizedSet(new HashSet<>());

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    @ConfigProperty(name = "ws-proxy.uri", defaultValue = "http://127.0.0.1:8080")
    String wsProxyEndpoint;

    @ConfigProperty(name = "ws.connection.max-life-time", defaultValue = "600")
    long wsConnectionMaxLifeTime;

    long userCounter = 1;
    long clientCount = 1;

    UserContext userContextFactory() {
        return new UserContext(String.format("User-%010d", userCounter++));
    }

    String smartClienIdFactory() {
        return String.format("SCd-%010d", clientCount++);
    }

    // void userContextGenerator() {
    // LOGGER.infof("User created: %s", userContextFactory());
    // }

    @Scheduled(every = "{scheduler.every}")
    void createSmartClientDevice() {
        if (devices.size() > 900) {
            // avoid thread limit per container
            // https://access.redhat.com/discussions/4713291
            // https://kubernetes.io/docs/concepts/policy/pid-limiting/#pod-pid-limits
            return;
        }
        SmartClientDeviceEmulator client = null;
        try {
            client = new SmartClientDeviceEmulator(/* smartClienIdFactory(), */ userContextFactory(), wsProxyEndpoint,
                    wsConnectionMaxLifeTime, scheduler);
            client.start();
            devices.add(client);
        } catch (DeploymentException | IOException | URISyntaxException e) {
            if (client != null) {
                client.setRestart(true);
            }
            LOGGER.debug("Error occured while emulator creation", e);
        }
    }
}
