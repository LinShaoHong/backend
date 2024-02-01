package com.github.sun.card;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.rest.AbstractResource;
import lombok.Builder;
import lombok.Data;

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

  @GET
  @Path("/byId")
  public SingleResponse<UserResp> byId(@QueryParam("id") String id) {
    return responseOf(UserResp.builder()
      .id(id)
      .code("code123")
      .openId("openId123")
      .nickname("Lins")
      .playCount(2)
      .build());
  }

  @GET
  @Path("/inc")
  public Response inc(@QueryParam("id") String id) {
    return responseOf();
  }

  @Data
  @Builder
  public static class UserResp {
    private String id;
    private String code;
    private String openId;
    private String nickname;
    private int playCount;
    private boolean vip;
  }
}