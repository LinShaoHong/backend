package com.github.sun.qm.admin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.qm.Collection;
import com.github.sun.qm.CollectionMapper;
import com.github.sun.qm.GirlMapper;
import com.github.sun.qm.UserMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/qm/admin/collects")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Message Resource")
public class AdminCollectResource extends AdminBasicResource {
  private final CollectionMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminCollectResource(UserMapper userMapper,
                              GirlMapper girlMapper,
                              CollectionMapper mapper,
                              @Named("mysql") SqlBuilder.Factory factory) {
    super(userMapper, girlMapper);
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取浏览记录")
  public PageResponse<ObjectNode> paged(@QueryParam("start") int start,
                                        @QueryParam("count") int count,
                                        @QueryParam("userName") String userName,
                                        @QueryParam("rank") @DefaultValue("updateTime") String rank,
                                        @Context Admin admin) {
    String userId = null;
    if (userName != null && !userName.isEmpty()) {
      userId = userMapper.findIdByUsername(userName.trim());
    }
    SqlBuilder sb = factory.create();
    Expression condition = Expression.EMPTY
      .and(userId == null || userId.isEmpty() ? null : sb.field("userId").eq(userId));
    int total = mapper.countByTemplate(sb.from(Collection.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Collection.class)
        .where(condition)
        .desc(rank)
        .limit(start, count)
        .template();
      List<Collection> list = mapper.findByTemplate(template);
      return responseOf(total, join(list, "userId", "girlId"));
    }
    return responseOf(total, Collections.emptyList());
  }
}
