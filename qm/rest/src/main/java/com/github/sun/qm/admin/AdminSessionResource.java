package com.github.sun.qm.admin;

import com.github.sun.foundation.boot.exception.UnAuthorizedException;
import com.github.sun.foundation.boot.utility.AES;
import com.github.sun.foundation.rest.AbstractResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/qm/admin/session")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Session Resource")
public class AdminSessionResource extends AbstractResource {
  private final String secretKey;
  private final String username;
  private final String password;

  @Inject
  public AdminSessionResource(Environment env) {
    this.username = env.getProperty("admin.username");
    this.password = env.getProperty("admin.password");
    this.secretKey = env.getProperty("base64.secret.key");
  }

  @POST
  @Path("/login")
  @ApiOperation("登录")
  public SingleResponse<String> login(@Valid @NotNull(message = "require body") LoginReq req) {
    if (username.equals(req.getUsername()) && password.equals(req.getPassword())) {
      return responseOf(AES.encrypt(username + ":" + password, secretKey));
    } else {
      throw new UnAuthorizedException("username or password is wrong");
    }
  }

  @Data
  private static class LoginReq {
    @NotNull(message = "缺少用户")
    private String username;
    @NotNull(message = "缺少密码")
    private String password;
  }
}
