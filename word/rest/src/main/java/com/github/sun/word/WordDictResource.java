package com.github.sun.word;

import com.github.sun.foundation.rest.AbstractResource;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/dict")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WordDictResource extends AbstractResource {
  private final WordDictService service;

  @Inject
  public WordDictResource(WordDictService service) {
    this.service = service;
  }

  @GET
  @Path("/{id}")
  public SingleResponse<WordDict> byId(@PathParam("id") String id) {
    return responseOf(service.byId(id));
  }
}