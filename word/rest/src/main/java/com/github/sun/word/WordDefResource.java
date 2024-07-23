package com.github.sun.word;

import com.github.sun.foundation.rest.AbstractResource;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/def")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WordDefResource extends AbstractResource {
  private final WordDefFetcher fetcher;

  @Inject
  public WordDefResource(WordDefFetcher fetcher) {
    this.fetcher = fetcher;
  }


  @GET
  @Path("/fetch")
  public Response chat(@QueryParam("word") String word) {
    fetcher.fetch(word);
    return responseOf();
  }
}