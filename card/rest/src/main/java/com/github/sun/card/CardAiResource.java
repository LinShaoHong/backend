package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import lombok.Data;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/ai")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardAiResource extends AbstractResource {
  private final CardAiService service;

  @Inject
  public CardAiResource(CardAiService service) {
    this.service = service;
  }


  @POST
  @Path("/chat")
  public ListResponse<String> chat(@Valid AiReq q) {
    return responseOf(service.chat(q.getQ()));
  }

  @Data
  public static class AiReq {
    private List<String> q;
  }
}