package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
}