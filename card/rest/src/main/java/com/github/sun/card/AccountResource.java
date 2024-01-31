package com.github.sun.card;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.rest.AbstractResource;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/account")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AccountResource extends AbstractResource {
  private final AccountService service;

  @Inject
  public AccountResource(AccountService service) {
    this.service = service;
  }

  /**
   * 微信登录获取用户OpenId
   */
  @GET
  @Path("/wx/login")
  public SingleResponse<JsonNode> getOpenIdByCode(@QueryParam("code") String code) {
    return responseOf(service.wxLogin(code));
  }
}