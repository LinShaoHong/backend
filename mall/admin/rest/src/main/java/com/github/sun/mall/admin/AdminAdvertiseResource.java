package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.AdvertiseMapper;
import com.github.sun.mall.core.entity.Advertise;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/ad")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 推广管理: advertise")
public class AdminAdvertiseResource extends BasicCURDResource<Advertise, AdvertiseMapper> {
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminAdvertiseResource(@Named("mysql") SqlBuilder.Factory factory) {
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取推广列表")
  public PageResponse<Advertise> paged(@QueryParam("name") String name,
                                       @QueryParam("content") String content,
                                       @QueryParam("start") int start,
                                       @QueryParam("count") int count,
                                       @QueryParam("sort") @DefaultValue("createTime") String sort,
                                       @QueryParam("asc") boolean asc) {

    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(name).then(sb.field("name").contains(name))
      .and(name == null ? null : sb.field("content").contains(content));
    int total = mapper.countByTemplate(sb.from(Advertise.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Advertise.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Advertise> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @Override
  protected String name() {
    return "推广";
  }
}
