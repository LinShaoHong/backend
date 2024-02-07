package com.github.sun.qm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.sun.foundation.boot.utility.IPs;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.rest.Locations;
import com.github.sun.qm.resolver.Session;
import lombok.Data;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;

import static com.github.sun.qm.SessionService.TOKEN_NAME;

@Path("/v1/qm/session")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SessionResource extends AbstractResource {
  private final UserMapper mapper;
  private final SessionService service;
  @Context
  private HttpServletRequest request;

  @Inject
  public SessionResource(UserMapper mapper,
                         SessionService service) {
    this.mapper = mapper;
    this.service = service;
  }

  /**
   * 注册
   */
  @POST
  @Path("/register")
  public SingleResponse<String> register(@Valid @NotNull(message = "require body") RegisterReq req) {
    String ip = IPs.getRemoteIP(request);
    String token = service.register(req.getUsername(), req.getPassword(), req.getEmail(), ip, Locations.fromIp(ip));
    return responseOf(token);
  }

  /**
   * 校验用户名是否已存在
   */
  @GET
  @Path("/checkName")
  public SingleResponse<Boolean> checkName(@NotEmpty(message = "缺少用户名") @QueryParam("name") String name) {
    return responseOf(mapper.countByUsername(name) > 0);
  }

  /**
   * 校验邮箱是否已存在
   */
  @GET
  @Path("/checkEmail")
  public SingleResponse<Boolean> checkEmail(@NotEmpty(message = "缺少邮箱") @QueryParam("email") String email) {
    return responseOf(mapper.countByEmail(email) > 0);
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class RegisterReq {
    @NotBlank(message = "缺少用户")
    private String username;
    @NotBlank(message = "缺少密码")
    private String password;
    private String email;
  }

  /**
   * 登录
   */
  @POST
  @Path("/login")
  public SingleResponse<String> login(@Valid @NotNull(message = "require body") LoginReq req) {
    String ip = IPs.getRemoteIP(request);
    String token = service.login(req.getUsername(), req.getPassword(), ip, Locations.fromIp(ip));
    return responseOf(token);
  }

  @Data
  private static class LoginReq {
    @NotBlank(message = "缺少用户")
    private String username;
    @NotBlank(message = "缺少密码")
    private String password;
  }

  /**
   * 登出
   */
  @POST
  @Path("/logout")
  public javax.ws.rs.core.Response logout(@Context Session session) {
    NewCookie cookie = new NewCookie(TOKEN_NAME, "", "/", null, "", 0, false, true);
    return javax.ws.rs.core.Response
      .ok()
      .cookie(cookie)
      .build();
  }
}
