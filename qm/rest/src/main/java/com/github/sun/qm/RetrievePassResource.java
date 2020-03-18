package com.github.sun.qm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.sun.foundation.boot.exception.Message;
import com.github.sun.foundation.rest.AbstractResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Objects;

@Path("/v1/qm/retrievePass")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Retrieve Password Resource")
public class RetrievePassResource extends AbstractResource {
  private static final String key = "retrieve.password.key";
  private static final int expire = 5;

  @Autowired
  private Environment env;

  private final UserMapper userMapper;

  @Inject
  public RetrievePassResource(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  @POST
  @Path("/url")
  @ApiOperation("生成验证链接")
  public SingleResponse<String> genUrl(@NotNull(message = "缺乏实体") GenReq req) {
    User user = userMapper.findByEmail(req.getEmail());
    if (user == null) {
      throw new Message(4000);
    }
    String KEY = env.getProperty(key);
    long timestamp = System.currentTimeMillis();
    String sign = User.hashPassword(user.getId() + ":" + timestamp + ":" + KEY);
    return responseOf(String.format("sign=%s&timestamp=%s&id=%s", sign, timestamp, user.getId()));
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class GenReq {
    private String email;
  }

  @POST
  @Path("/check")
  @ApiOperation("校验")
  public Response check(@NotNull(message = "缺乏实体") CheckReq req) {
    String KEY = env.getProperty(key);
    String sign = User.hashPassword(req.getId() + ":" + req.getTimestamp() + ":" + KEY);
    if (!Objects.equals(sign, req.getSign())) {
      throw new Message(4001);
    }
    long now = System.currentTimeMillis();
    if (now - req.getTimestamp() > expire * 60 * 1000) {
      throw new Message(4002);
    }
    return responseOf();
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class CheckReq {
    private String id;
    private String sign;
    private long timestamp;
  }

  @PUT
  @Path("/change")
  @ApiOperation("更改")
  public Response change(@NotNull(message = "缺乏实体") ChangeReq req) {
    User user = userMapper.findById(req.getId());
    if (user != null) {
      user.setPassword(User.hashPassword(req.getPassword()));
      userMapper.update(user);
    } else {
      throw new Message(4003);
    }
    return responseOf();
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class ChangeReq {
    private String id;
    private String password;
  }
}
