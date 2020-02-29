package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.GoodsMapper;
import com.github.sun.mall.core.TopicMapper;
import com.github.sun.mall.core.entity.Topic;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/topic")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 专题管理: topic")
public class AdminTopicResource extends BasicCURDResource<Topic, TopicMapper> {
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminTopicResource(@Named("mysql") SqlBuilder.Factory factory) {
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取专题列表")
  public PageResponse<Topic> list(@QueryParam("title") String title,
                                  @QueryParam("subtitle") String subtitle,
                                  @QueryParam("start") int start,
                                  @QueryParam("count") int count,
                                  @QueryParam("sort") @DefaultValue("createTime") String sort,
                                  @QueryParam("asc") boolean asc) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(title).then(sb.field("keyword").contains(title))
      .and(subtitle == null ? null : sb.field("subtitle").contains(subtitle));
    int total = mapper.countByTemplate(sb.from(Topic.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Topic.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Topic> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @Override
  protected String name() {
    return "专题";
  }
}
