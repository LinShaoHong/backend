package com.github.sun.mall.core;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.mall.core.entity.Feedback;
import com.github.sun.mall.core.resolver.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;

@Path("/v1/mall/feedback")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall: Feedback Resource", tags = "意见反馈服务")
public class FeedbackResource extends AbstractResource {
  private final FeedbackMapper mapper;

  @Inject
  public FeedbackResource(FeedbackMapper mapper) {
    this.mapper = mapper;
  }

  @POST
  @ApiOperation("添加反馈")
  public Response submit(@Valid @NotNull(message = "缺少实体") Feedback feedback,
                         @Context LoginUser user) {
    String username = user.getName();
    feedback.setId(IdGenerator.next());
    feedback.setUserId(user.getId());
    feedback.setUsername(username);
    feedback.setStatus(1);
    if (!feedback.isHasPicture()) {
      feedback.setPicUrls(Collections.emptyList());
    }
    mapper.insert(feedback);
    return responseOf();
  }
}
