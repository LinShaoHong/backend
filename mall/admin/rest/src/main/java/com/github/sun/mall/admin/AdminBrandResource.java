package com.github.sun.mall.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.BrandMapper;
import com.github.sun.mall.core.entity.Brand;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;

import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/brand")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 品牌管理")
@Validated
public class AdminBrandResource extends BasicCURDResource<Brand, BrandMapper> {
  private final SqlBuilder.Factory factory;

  public AdminBrandResource(@Named("mysql") SqlBuilder.Factory factory) {
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取品牌")
  public PageResponse<Brand> getAll(@QueryParam("id") String id,
                                    @QueryParam("name") String name,
                                    @QueryParam("start") int start,
                                    @QueryParam("count") int count,
                                    @QueryParam("sort") @DefaultValue("createTime") String sort,
                                    @QueryParam("asc") boolean asc) {
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

  @Override
  protected String name() {
    return "品牌";
  }
}
