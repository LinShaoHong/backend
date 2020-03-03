package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.UserMapper;
import com.github.sun.mall.core.entity.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 用户管理: user")
public class AdminUserResource extends AbstractResource {
  private final UserMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminUserResource(UserMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取用户信息")
  @Authentication(value = "admin:user:query", tags = {"用户管理", "会员管理", "查询"})
  public PageResponse<User> paged(@QueryParam("username") String username,
                                  @QueryParam("mobile") String mobile,
                                  @QueryParam("start") int start,
                                  @QueryParam("count") int count,
                                  @QueryParam("sort") @DefaultValue("createTime") String sort,
                                  @QueryParam("asc") boolean asc,
                                  @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(username).then(sb.field("username").contains(username))
      .and(mobile == null ? null : sb.field("mobile").eq(mobile));
    int total = mapper.countByTemplate(sb.from(User.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(User.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<User> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }
}
