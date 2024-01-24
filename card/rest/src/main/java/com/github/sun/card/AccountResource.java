package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import io.swagger.annotations.Api;
import lombok.AllArgsConstructor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@AllArgsConstructor
@Path("/v1/account")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Collection Resource")
public class AccountResource extends AbstractResource {
  private final AccountService service;

  @GET
  @Path("openId")
  public SingleResponse<String> getOpenIdByCode(@QueryParam("code") String code) {
    return responseOf(service.getOpenIdByCode(code));
  }
}