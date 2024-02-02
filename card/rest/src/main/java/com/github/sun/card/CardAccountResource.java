package com.github.sun.card;

import com.github.sun.foundation.rest.AbstractResource;
import lombok.Data;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/account")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardAccountResource extends AbstractResource {
  private final AccountService service;

  @Inject
  public CardAccountResource(AccountService service) {
    this.service = service;
  }

  @GET
  @Path("/wx/login")
  public SingleResponse<AccountService.UserResp> getOpenIdByCode(@QueryParam("code") String code) {
    return responseOf(service.wxLogin(code));
  }

  @GET
  @Path("/byId")
  public SingleResponse<AccountService.UserResp> byId(@QueryParam("id") String id) {
    return responseOf(service.byId(id));
  }

  @GET
  @Path("/inc")
  public Response inc(@QueryParam("id") String id) {
    service.inc(id);
    return responseOf();
  }

  @POST
  @Path("/updateNickname")
  public Response updateNickname(@Valid UpdateNicknameReq q) {
    service.updateNickname(q.getId(), q.getNickname());
    return responseOf();
  }

  @Data
  public static class UpdateNicknameReq {
    private String id;
    private String nickname;
  }
}