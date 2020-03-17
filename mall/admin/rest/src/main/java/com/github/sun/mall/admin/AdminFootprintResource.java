package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.FootprintMapper;
import com.github.sun.mall.core.entity.Footprint;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/footprint")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 历史足迹管理: footprint")
public class AdminFootprintResource extends AbstractResource {
  private final FootprintMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminFootprintResource(FootprintMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取足迹列表")
  @Authentication(value = "admin:footprint:query", tags = {"用户管理", "用户足迹", "查询"})
  public PageResponse<Footprint> paged(@QueryParam("userId") String userId,
                                       @QueryParam("goodsId") String goodsId,
                                       @QueryParam("start") int start,
                                       @QueryParam("count") int count,
                                       @QueryParam("sort") @DefaultValue("createTime") String sort,
                                       @QueryParam("asc") boolean asc,
                                       @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(userId).then(sb.field("userId").eq(userId))
      .and(goodsId == null ? null : sb.field("username").eq(goodsId));
    int total = mapper.countByTemplate(sb.from(Footprint.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Footprint.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Footprint> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }
}
