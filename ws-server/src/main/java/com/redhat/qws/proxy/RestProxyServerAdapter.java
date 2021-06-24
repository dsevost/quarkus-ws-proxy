package com.redhat.qws.proxy;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.qws.proxy.model.MessageExchange;
import com.redhat.qws.proxy.service.DatagridService;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.StartupEvent;

@Path("proxy")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestProxyServerAdapter {
    private static final Logger LOGGER = Logger.getLogger(RestProxyServerAdapter.class);

    @ConfigProperty(name = "datagrid.use", defaultValue = "false")
    boolean datagridUsage;

    @Inject
    MeterRegistry registry;

    @Inject
    DatagridService datagrid;

    @Inject
    WebSocketServerAdapter wsServer;

    void onStart(@Observes StartupEvent ev) {
        if (datagridUsage) {
            LOGGER.info("Using datagrid");
        } else {
            LOGGER.infof("Datagrid will not be used, to use it set property '%s' to true", "datagrid.use");
        }
    }
    
    @POST
    @Path("server/receiver")
    @Counted("send_client_counter")
    @Timed("send_client")
    public void receive(@Valid MessageExchange mmx) {
        // LOGGER.infof("Received notification from SENDER: raw=[%s]", mx);
        // MessageExchange mmx = JsonbBuilder.create().fromJson(mx,
        // MessageExchange.class);
        LOGGER.infof("Received notification from SENDER: mmx=[%s]", mmx);
        if (datagridUsage) {
            datagrid.store(mmx.client.user.name, mmx.client.id, mmx.message);
        } else {
            wsServer.send(mmx.client.getSmartCleintKey(), mmx.message);
        }
    }

}
