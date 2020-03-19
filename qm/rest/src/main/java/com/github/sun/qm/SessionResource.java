package com.github.sun.qm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.qm.resolver.Session;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;

import static com.github.sun.qm.SessionService.TOKEN_NAME;

@Path("/v1/qm/session")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Session Resource")
public class SessionResource extends AbstractResource {
  private final UserMapper mapper;
  private final SessionService service;
  private final ContainerRequestContext request;

  @Inject
  public SessionResource(UserMapper mapper,
                         SessionService service,
                         ContainerRequestContext request) {
    this.mapper = mapper;
    this.service = service;
    this.request = request;
  }

  @POST
  @Path("/register")
  @ApiOperation("注册")
  public SingleResponse<String> register(@Valid @NotNull(message = "require body") RegisterReq req) {
    String token = service.register(req.getUsername(), req.getPassword(), req.getEmail(), getIpAddr());
    return responseOf(token);
  }

  @GET
  @Path("/checkName")
  @ApiOperation("校验用户名是否已存在")
  public SingleResponse<Boolean> checkName(@NotEmpty(message = "缺少用户名") @QueryParam("name") String name) {
    return responseOf(mapper.countByUsername(name) > 0);
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class RegisterReq {
    @NotNull(message = "缺少用户")
    private String username;
    @NotNull(message = "缺少密码")
    private String password;
    private String email;
  }

  @POST
  @Path("/login")
  @ApiOperation("登录")
  public SingleResponse<String> login(@Valid @NotNull(message = "require body") LoginReq req) {
    String token = service.login(req.getUsername(), req.getPassword(), getIpAddr());
    return responseOf(token);
  }

  @Data
  private static class LoginReq {
    @NotNull(message = "缺少用户")
    private String username;
    @NotNull(message = "缺少密码")
    private String password;
  }

  private String getIpAddr() {
    String ipAddress;
    try {
      ipAddress = request.getHeaderString("x-forwarded-for");
      if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
        ipAddress = request.getHeaderString("Proxy-Client-IP");
      }
      if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
        ipAddress = request.getHeaderString("WL-Proxy-Client-IP");
      }
      // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
      if (ipAddress != null && ipAddress.length() > 15) { // "***.***.***.***".length()
        if (ipAddress.indexOf(",") > 0) {
          ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
        }
      }
    } catch (Exception e) {
      ipAddress = "";
    }

    return ipAddress;
  }

  @POST
  @Path("/logout")
  @ApiOperation("登出")
  public javax.ws.rs.core.Response logout(@Context Session session) {
    NewCookie cookie = new NewCookie(TOKEN_NAME, "", "/", null, "", 0, false, true);
    return javax.ws.rs.core.Response
      .ok()
      .cookie(cookie)
      .build();
  }
}
