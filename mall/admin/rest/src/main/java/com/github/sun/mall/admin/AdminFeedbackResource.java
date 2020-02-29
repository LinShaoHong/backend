package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.FeedbackMapper;
import com.github.sun.mall.core.entity.Feedback;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/feedback")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 反馈管理: feedback")
public class AdminFeedbackResource extends AbstractResource {
  private final FeedbackMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminFeedbackResource(FeedbackMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取反馈列表")
  public PageResponse<Feedback> list(@QueryParam("userId") String userId,
                                     @QueryParam("username") String username,
                                     @QueryParam("start") int start,
                                     @QueryParam("count") int count,
                                     @QueryParam("sort") @DefaultValue("createTime") String sort,
                                     @QueryParam("asc") boolean asc) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(userId).then(sb.field("userId").eq(userId))
      .and(username == null ? null : sb.field("username").contains(username));
    int total = mapper.countByTemplate(sb.from(Feedback.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Feedback.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Feedback> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }
}
