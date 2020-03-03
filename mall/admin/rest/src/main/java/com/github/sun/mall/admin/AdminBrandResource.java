package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.BrandMapper;
import com.github.sun.mall.core.entity.Brand;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/brand")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 品牌管理: brand")
public class AdminBrandResource extends AbstractResource {
  private final BrandMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminBrandResource(BrandMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取品牌")
  @Authentication(value = "admin:brand:query", tags = {"商场管理", "品牌管理", "查询"})
  public PageResponse<Brand> getAll(@QueryParam("id") String id,
                                    @QueryParam("name") String name,
                                    @QueryParam("start") int start,
                                    @QueryParam("count") int count,
                                    @QueryParam("sort") @DefaultValue("createTime") String sort,
                                    @QueryParam("asc") boolean asc,
                                    @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(id).then(sb.field("id").eq(id))
      .and(name == null ? null : sb.field("name").contains(name));
    int total = mapper.countByTemplate(sb.from(Brand.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Brand.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Brand> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @POST
  @ApiOperation("创建")
  @Authentication(value = "admin:brand:create", tags = {"商场管理", "品牌管理", "添加"})
  public Response create(@Valid @NotNull(message = "缺少实体") Brand ad,
                         @Context Admin admin) {
    ad.setId(IdGenerator.next());
    mapper.insert(ad);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取")
  @Authentication(value = "admin:brand:detail", tags = {"商场管理", "品牌管理", "详情"})
  public SingleResponse<Brand> get(@PathParam("id") String id,
                                   @Context Admin admin) {
    Brand v = mapper.findById(id);
    if (v == null) {
      throw new com.github.sun.foundation.boot.exception.NotFoundException("Can not find brand by id=" + id);
    }
    return responseOf(v);
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("编辑")
  @Authentication(value = "admin:brand:update", tags = {"商场管理", "品牌管理", "编辑"})
  public Response update(@PathParam("id") String id,
                         @Valid @NotNull(message = "缺少实体") Brand v,
                         @Context Admin admin) {
    Brand e = mapper.findById(id);
    if (e == null) {
      throw new com.github.sun.foundation.boot.exception.NotFoundException("Can not find brand by id=" + id);
    }
    v.setId(id);
    mapper.update(v);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  @Authentication(value = "admin:brand:delete", tags = {"商场管理", "品牌管理", "删除"})
  public Response delete(@PathParam("id") String id,
                         @Context Admin admin) {
    Brand v = mapper.findById(id);
    if (v == null) {
      throw new NotFoundException("Can not find brand by id=" + id);
    }
    mapper.deleteById(id);
    return responseOf();
  }
}
