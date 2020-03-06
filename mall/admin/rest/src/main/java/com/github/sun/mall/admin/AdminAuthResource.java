package com.github.sun.mall.admin;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.admin.api.AdminSessionService;
import com.github.sun.mall.admin.entity.Admin;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.github.sun.mall.admin.AdminSessionServiceImpl.TOKEN_EXPIRED;
import static com.github.sun.mall.admin.AdminSessionServiceImpl.TOKEN_NAME;


@Path("/v1/mall/admin/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 登录管理: auth")
public class AdminAuthResource extends AbstractResource {
  private final AdminSessionService service;
  private final ContainerRequestContext request;

  @Inject
  public AdminAuthResource(AdminSessionService service, ContainerRequestContext request) {
    this.service = service;
    this.request = request;
  }

  @POST
  @Path("/login")
  @ApiOperation("登录")
  public javax.ws.rs.core.Response login(@Valid @NotNull(message = "require body") LoginReq req) {
    String token = service.login(req.getUsername(), req.getPassword(), getIpAddr());
    NewCookie cookie = new NewCookie(TOKEN_NAME, token, "/", null, NewCookie.DEFAULT_VERSION, null, TOKEN_EXPIRED, null, false, true);
    return javax.ws.rs.core.Response
      .ok()
      .cookie(cookie)
      .entity(responseOf(token))
      .build();
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
  public Object logout(@Context Admin admin) {
    NewCookie cookie = new NewCookie(TOKEN_NAME, "", "/", null, "", 0, false, true);
    return javax.ws.rs.core.Response
      .ok()
      .cookie(cookie)
      .build();
  }

  @GET
  @Path("/info")
  public Object info(@Context Admin admin) {
    Map<String, Object> data = new HashMap<>();
    data.put("name", admin.getUsername());
    data.put("avatar", admin.getAvatar());
    data.put("perms", Arrays.asList("*"));
    data.put("roles", Arrays.asList("超级管理员"));
    return responseOf(data);
  }
//
//  @Autowired
//  private ApplicationContext context;
//  private HashMap<String, String> systemPermissionsMap = null;
//
//  private Collection<String> toApi(Set<String> permissions) {
//    if (systemPermissionsMap == null) {
//      systemPermissionsMap = new HashMap<>();
//      final String basicPackage = "org.linlinjava.litemall.admin";
//      List<Permission> systemPermissions = PermissionUtil.listPermission(context, basicPackage);
//      for (Permission permission : systemPermissions) {
//        String perm = permission.getRequiresPermissions().value()[0];
//        String api = permission.getApi();
//        systemPermissionsMap.put(perm, api);
//      }
//    }
//
//    Collection<String> apis = new HashSet<>();
//    for (String perm : permissions) {
//      String api = systemPermissionsMap.get(perm);
//      apis.add(api);
//
//      if (perm.equals("*")) {
//        apis.clear();
//        apis.add("*");
//        return apis;
//        //                return systemPermissionsMap.values();
//
//      }
//    }
//    return apis;
//  }
//
//  @GetMapping("/401")
//  public Object page401() {
//    return ResponseUtil.unlogin();
//  }
//
//  @GetMapping("/index")
//  public Object pageIndex() {
//    return ResponseUtil.ok();
//  }
//
//  @GetMapping("/403")
//  public Object page403() {
//    return ResponseUtil.unauthz();
//  }
}
