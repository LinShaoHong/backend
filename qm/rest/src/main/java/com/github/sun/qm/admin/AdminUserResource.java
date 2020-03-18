package com.github.sun.qm.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.qm.User;
import com.github.sun.qm.UserMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/qm/admin/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin User Resource")
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
  public PageResponse<User> paged(@QueryParam("id") String id,
                                  @QueryParam("username") String username,
                                  @QueryParam("email") String email,
                                  @QueryParam("vip") Boolean vip,
                                  @QueryParam("start") int start,
                                  @QueryParam("count") int count,
                                  @DefaultValue("createTime") @QueryParam("rank") String rank) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonEmpty(id).then(sb.field("id").eq(id))
      .and(username == null ? null : sb.field("username").contains(username))
      .and(email == null ? null : sb.field("email").contains(email))
      .and(vip == null ? null : sb.field("vip").eq(vip));
    int total = mapper.countByTemplate(sb.from(User.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(User.class)
        .where(condition)
        .desc(rank)
        .limit(start, count)
        .template();
      List<User> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

}
