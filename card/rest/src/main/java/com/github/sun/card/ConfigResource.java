package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/config")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConfigResource extends AbstractResource {
  @GET
  public SingleResponse<Config> get(@QueryParam("code") String code) {
    return responseOf(Config.builder()
      .playLimit(5)
      .price("2.99")
      .willPrice("19.99")
      .build());
  }

  @Data
  @Builder
  public static class Config {
    private int playLimit;
    private String price;
    private String willPrice;
  }
}