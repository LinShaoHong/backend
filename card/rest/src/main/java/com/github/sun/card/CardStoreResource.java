package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;

@Path("/store")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardStoreResource extends AbstractResource {
  private final CardStoreService service;

  @Inject
  public CardStoreResource(CardStoreService service) {
    this.service = service;
  }

  @POST
  @Path("/put")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  public SingleResponse<String> upload(@FormDataParam("file") InputStream in,
                                       @FormDataParam("file") FormDataContentDisposition meta) {
    return responseOf(service.put(in, meta.getFileName()));
  }
}