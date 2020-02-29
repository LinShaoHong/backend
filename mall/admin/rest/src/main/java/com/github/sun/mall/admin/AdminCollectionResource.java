package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.CollectionMapper;
import com.github.sun.mall.core.entity.Collection;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/collection")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 收藏管理: collection")
public class AdminCollectionResource extends AbstractResource {
  private final CollectionMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminCollectionResource(CollectionMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取收藏列表")
  public PageResponse<Collection> list(@QueryParam("userId") String userId,
                                       @QueryParam("valueId") String valueId,
                                       @QueryParam("start") int start,
                                       @QueryParam("count") int count,
                                       @QueryParam("sort") @DefaultValue("createTime") String sort,
                                       @QueryParam("asc") boolean asc) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(userId).then(sb.field("userId").eq(userId))
      .and(valueId == null ? null : sb.field("valueId").contains(valueId));
    int total = mapper.countByTemplate(sb.from(Collection.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Collection.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Collection> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }
}
