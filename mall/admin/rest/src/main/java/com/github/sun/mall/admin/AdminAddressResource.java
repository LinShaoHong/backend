package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.AddressMapper;
import com.github.sun.mall.core.entity.Address;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/address")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 收货地址管理: address")
public class AdminAddressResource extends AbstractResource {
  private final AddressMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminAddressResource(AddressMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取地址列表")
  @Authentication(value = "admin:address:query", tags = {"用户管理", "收货地址", "查询"})
  public PageResponse<Address> paged(@QueryParam("userId") String userId,
                                     @QueryParam("name") String name,
                                     @QueryParam("start") int start,
                                     @QueryParam("count") int count,
                                     @QueryParam("sort") @DefaultValue("createTime") String sort,
                                     @QueryParam("asc") boolean asc,
                                     @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(userId).then(sb.field("userId").eq(userId))
      .and(name == null ? null : sb.field("name").contains(name));
    int total = mapper.countByTemplate(sb.from(Address.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Address.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Address> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }
}

