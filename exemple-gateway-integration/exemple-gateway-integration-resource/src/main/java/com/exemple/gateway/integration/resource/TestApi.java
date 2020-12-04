package com.exemple.gateway.integration.resource;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Path("/test")
public class TestApi {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @HEAD
    @Path("/{id}")
    public Response head(@PathParam("id") String id) {

        return Response.status(Status.NO_CONTENT).build();

    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response post(@NotNull JsonNode source) {

        return Response.status(Status.CREATED).build();

    }

    @PATCH
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("id") String login, ArrayNode patch) {

        return Response.status(Status.NO_CONTENT).build();

    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode get(@PathParam("id") String id) {

        return MAPPER.createObjectNode();

    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {

        return Response.status(Status.NO_CONTENT).build();

    }
}
