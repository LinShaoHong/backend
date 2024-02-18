package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/share")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardShareResource extends AbstractResource {
  private final CardShareService service;

  @Inject
  public CardShareResource(CardShareService service) {
    this.service = service;
  }

  @GET
  @Path("/share")
  public Response share(@QueryParam("shareUserId") String shareUserId,
                        @QueryParam("shareId") String shareId) {
    service.share(shareUserId, shareId);
    return responseOf();
  }
}