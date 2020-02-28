package com.github.sun.mall.core;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.entity.Brand;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/v1/mall/brand")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-core: 品牌服务")
public class BrandResource extends AbstractResource {
  private final BrandMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public BrandResource(BrandMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("获取品牌列表")
  public ListResponse<Brand> getAll(@QueryParam("start") int start,
                                    @QueryParam("count") int count,
                                    @QueryParam("sort") @DefaultValue("createTime") String sort,
                                    @QueryParam("asc") boolean asc) {
    SqlBuilder sb = factory.create();
    return responseOf(mapper.findByTemplate(sb.from(Brand.class).orderBy(sort, asc).template()));
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取单个品牌")
  public SingleResponse<Brand> detail(@PathParam("id") String id) {
    Brand brand = mapper.findById(id);
    if (brand == null) {
      throw new NotFoundException("Can not find brand by id=" + id);
    }
    return responseOf(brand);
  }
}
