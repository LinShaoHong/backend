package com.github.sun.card;

import com.github.sun.foundation.boot.utility.IPs;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.rest.Locations;
import lombok.Data;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/account")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CardUserResource extends AbstractResource {
  private final CardUserService service;
  @Context
  private HttpServletRequest request;

  @Inject
  public CardUserResource(CardUserService service) {
    this.service = service;
  }

  @GET
  @Path("/wx/login")
  public SingleResponse<CardUserService.UserResp> getOpenIdByCode(@QueryParam("code") String code) {
    String ip = IPs.getRemoteIP(request);
    String location = Locations.fromIp(ip);
    return responseOf(service.wxLogin(code, ip, location));
  }

  @GET
  @Path("/byId")
  public SingleResponse<CardUserService.UserResp> byId(@QueryParam("id") String id) {
    return responseOf(service.byId(id));
  }

  @GET
  @Path("/inc")
  public Response inc(@QueryParam("id") String id) {
    service.inc(id);
    return responseOf();
  }

  @POST
  @Path("/vip")
  public Response vip(@Valid UpdateVipReq q) {
    service.vip(q.getId(), q.getPrepayId(), q.getVip());
    return responseOf();
  }

  @POST
  @Path("/updateNickname")
  public Response updateNickname(@Valid UpdateNicknameReq q) {
    service.updateNickname(q.getId(), q.getNickname());
    return responseOf();
  }

  @POST
  @Path("/updateAvatar")
  public Response updateAvatar(@Valid UpdateAvatarReq q) {
    service.updateAvatar(q.getId(), q.getAvatar());
    return responseOf();
  }

  @Data
  public static class UpdateVipReq {
    private String id;
    private String prepayId;
    private int vip;
  }

  @Data
  public static class UpdateNicknameReq {
    private String id;
    private String nickname;
  }

  @Data
  public static class UpdateAvatarReq {
    private String id;
    private int avatar;
  }
}