package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Slf4j
@Path("/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardConfigResource extends AbstractResource {
  private final CardConfig config;

  @Inject
  public CardConfigResource(CardConfig config) {
    this.config = config;
  }

  @GET
  public SingleResponse<CardConfig> get() {
    return responseOf(config);
  }

  @POST
  @Path("/log")
  public Response log(@Valid LogReq req) {
    log.info("\n\n----------------\n" + req.getLog() + "\n----------------\n\n");
    return responseOf();
  }

  @Data
  public static class LogReq {
    private String log;
  }
}