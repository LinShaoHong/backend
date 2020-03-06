package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.BrandMapper;
import com.github.sun.mall.core.GoodsMapper;
import com.github.sun.mall.core.entity.Brand;
import com.github.sun.mall.core.entity.Goods;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/v1/mall/admin/goods")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 商品管理: goods")
public class AdminGoodsResource extends AbstractResource {
  private final GoodsMapper mapper;
  private final BrandMapper brandMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminGoodsResource(GoodsMapper mapper,
                            BrandMapper brandMapper,
                            @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.brandMapper = brandMapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("查询商品")
  @Authentication(value = "admin:goods:query", tags = {"商品管理", "商品管理", "查询"})
  public PageResponse<Goods> paged(@QueryParam("id") String id,
                                   @QueryParam("sn") String sn,
                                   @QueryParam("name") String name,
                                   @QueryParam("start") int start,
                                   @QueryParam("count") int count,
                                   @QueryParam("sort") @DefaultValue("createTime") String sort,
                                   @QueryParam("asc") boolean asc,
                                   @Context Admin admin) {

    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(id).then(sb.field("id").eq(id))
      .and(sn == null ? null : sb.field("sn").eq(sn))
      .and(name == null ? null : sb.field("name").contains(name));
    int total = mapper.countByTemplate(sb.from(Goods.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Goods.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Goods> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @GET
  @Path("/catAndBrand")
  public Object catAndBrand() {
    List<Brand> all = brandMapper.findAll();
    Map<String, Object> data = new HashMap<>();
    data.put("brandList", all.stream().map(v -> new HashMap<String, String>() {{
      put("value", v.getId());
      put("label", v.getName());
    }}).collect(Collectors.toList()));
    return responseOf(data);
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("编辑商品")
  @Authentication(value = "admin:goods:update", tags = {"商品管理", "商品管理", "编辑"})
  public Response update(@PathParam("id") String id,
                         @Valid @NotNull(message = "缺少实体") Goods goods,
                         @Context Admin admin) {
    Goods e = mapper.findById(id);
    if (e == null) {
      throw new NotFoundException("Can not find goods by id=" + id);
    }
    List<Goods> list = mapper.findByName(goods.getName());
    list.removeIf(v -> v.getId().equals(id));
    if (list.size() > 0) {
      throw new BadRequestException("商品名称重复");
    }
    goods.setId(id);
    goods.recomputeRetailPrice();
    mapper.update(goods);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除商品")
  @Authentication(value = "admin:goods:delete", tags = {"商品管理", "商品管理", "删除"})
  public Response delete(@PathParam("id") String id,
                         @Context Admin admin) {
    Goods e = mapper.findById(id);
    if (e == null) {
      throw new NotFoundException("Can not find goods by id=" + id);
    }
    mapper.deleteById(id);
    return responseOf();
  }

  @POST
  @ApiOperation("添加商品")
  @Authentication(value = "admin:goods:create", tags = {"商品管理", "商品管理", "添加"})
  public Response update(@Valid @NotNull(message = "缺少实体") Goods goods,
                         @Context Admin admin) {
    List<Goods> list = mapper.findByName(goods.getName());
    if (list.size() > 0) {
      throw new BadRequestException("商品名称重复");
    }
    goods.setId(IdGenerator.next());
    goods.recomputeRetailPrice();
    mapper.insert(goods);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取单个商品详情")
  @Authentication(value = "admin:goods:detail", tags = {"商品管理", "商品管理", "详情"})
  public SingleResponse<Goods> get(@PathParam("id") String id,
                                   @Context Admin admin) {
    return responseOf(mapper.findById(id));
  }
}
