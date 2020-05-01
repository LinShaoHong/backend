package com.github.sun.qm;

import com.github.sun.foundation.boot.exception.Message;
import com.github.sun.foundation.rest.AbstractResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@Slf4j
@Path("/v1/qm/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "User Resource")
public class UserResource extends AbstractResource {
  private final UserService service;
  private final UserMapper userMapper;
  private final PayLogMapper payLogMapper;
  private final StorageService storageService;

  @Inject
  public UserResource(UserService service, UserMapper userMapper, PayLogMapper payLogMapper, StorageService storageService) {
    this.service = service;
    this.userMapper = userMapper;
    this.payLogMapper = payLogMapper;
    this.storageService = storageService;
  }

  @PUT
  @Path("/signIn")
  @ApiOperation("签到")
  public SingleResponse<UserResp> signIn(@Context User user) {
    return responseOf(UserResp.from(service.signIn(user)));
  }

  @GET
  @Path("/info")
  @ApiOperation("用户信息")
  public SingleResponse<UserResp> info(@Context User user) {
    BigDecimal paymentTotal = payLogMapper.sumByUserId(user.getId());
    UserResp info = UserResp.from(user);
    info.setPaymentTotal(paymentTotal);
    return responseOf(info);
  }

  private static final SimpleDateFormat DAY_FORMATTER = new SimpleDateFormat("yyyy-MM-dd");
  private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Data
  @Builder
  private static class UserResp {
    private String id;
    private String name;
    private String avatar;
    private BigDecimal amount;
    private boolean vip;
    private String vipEndTime;
    private String signInTime;
    private int signInCount;
    private boolean canSignIn;
    private BigDecimal paymentTotal;

    private static UserResp from(User v) {
      String signTime = v.getSignInTime() == null ? null : DAY_FORMATTER.format(v.getSignInTime());
      String today = DAY_FORMATTER.format(new Date());
      return UserResp.builder()
        .id(v.getId())
        .name(v.getUsername())
        .avatar(v.getAvatar())
        .amount(v.getAmount())
        .vip(v.isVip())
        .vipEndTime(v.getVipEndTime() == null ? null : FORMATTER.format(v.getVipEndTime()))
        .signInTime(v.getSignInTime() == null ? null : FORMATTER.format(v.getSignInTime()))
        .signInCount(v.getSignInCount())
        .canSignIn(!Objects.equals(signTime, today))
        .build();
    }
  }

  @POST
  @Path("/upload/avatar")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @ApiOperation("上传头像")
  public SingleResponse<String> uploadAvatar(@FormDataParam("file") InputStream in,
                                             @FormDataParam("file") FormDataContentDisposition meta,
                                             @Context User user) {
    try {
      String avatar = storageService.upload(in, meta.getFileName());
      user.setAvatar(avatar);
      userMapper.update(user);
      return responseOf(avatar);
    } catch (IOException ex) {
      log.error("Upload Avatar Error: \n", ex);
      throw new Message(5001);
    }
  }

  @DELETE
  @Path("/delete/avatar")
  @ApiOperation("删除头像")
  public Response deleteImage(@Valid DeleteReq path) {
    final String p = path.getPath();
    if (p != null && !p.isEmpty() && !Objects.equals("/avatar.jpg", p)) {
      storageService.delete(p);
    }
    return responseOf();
  }

  @Data
  private static class DeleteReq {
    private String path;
  }
}
