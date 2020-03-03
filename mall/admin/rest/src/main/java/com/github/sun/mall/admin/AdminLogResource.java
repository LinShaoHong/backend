package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.admin.entity.Log;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/log")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 日志管理: log")
public class AdminLogResource extends AbstractResource {
  private final LogMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminLogResource(LogMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("日志查询")
  @Authentication(value = "admin:log:query", tags = {"系统管理", "操作日志", "查询"})
  public PageResponse<Log> list(@QueryParam("name") String name,
                                @QueryParam("start") int start,
                                @QueryParam("count") int count,
                                @QueryParam("sort") @DefaultValue("createTime") String sort,
                                @QueryParam("asc") boolean asc,
                                @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(name).then(sb.field("admin").contains(name));
    int total = mapper.countByTemplate(sb.from(Log.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Log.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Log> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }
}
