package com.redhat.qws.proxy.service;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;

@Path("rest")
@RegisterRestClient(configKey="smarctlient-api")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface SmartClientRestService {
    @GET
    @Path("subscribe")
    Uni<String> subscribe(@QueryParam(SCMParams.PRAMETER_NAME_USER) String user, @QueryParam(SCMParams.PRAMETER_NAME_CLIENT_ID) String cid, @QueryParam(SCMParams.PRAMETER_NAME_LEGACY_IP) String ip) ;

    @GET
    @Path("unsubscribe")
    Uni<String> unsubscribe(@QueryParam(SCMParams.PRAMETER_NAME_USER) String user, @QueryParam(SCMParams.PRAMETER_NAME_CLIENT_ID) String cid, @QueryParam(SCMParams.PRAMETER_NAME_LEGACY_IP) String ip) ;
}
