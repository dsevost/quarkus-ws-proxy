package com.redhat.qws.sender.service;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.redhat.qws.sender.model.MessageExchange;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey="ws-proxy")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("proxy")
public interface RestWSProxyService {
    @POST
    @Path("server/receiver")
    void send(@Valid MessageExchange m);
}
