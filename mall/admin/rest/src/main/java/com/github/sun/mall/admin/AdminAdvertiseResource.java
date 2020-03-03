package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.AdvertiseMapper;
import com.github.sun.mall.core.entity.Advertise;
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

@Path("/v1/mall/admin/advertise")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 推广管理: advertise")
public class AdminAdvertiseResource extends AbstractResource {
  private final AdvertiseMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminAdvertiseResource(AdvertiseMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取推广列表")
  @Authentication(value = "admin:ad:query", tags = {"推广管理", "广告管理", "查询"})
  public PageResponse<Advertise> paged(@QueryParam("name") String name,
                                       @QueryParam("content") String content,
                                       @QueryParam("start") int start,
                                       @QueryParam("count") int count,
                                       @QueryParam("sort") @DefaultValue("createTime") String sort,
                                       @QueryParam("asc") boolean asc,
                                       @Context Admin admin) {

    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(name).then(sb.field("name").contains(name))
      .and(name == null ? null : sb.field("content").contains(content));
    int total = mapper.countByTemplate(sb.from(Advertise.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Advertise.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Advertise> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @POST
  @ApiOperation("创建")
  @Authentication(value = "admin:ad:create", tags = {"推广管理", "广告管理", "添加"})
  public Response create(@Valid @NotNull(message = "缺少实体") Advertise ad,
                         @Context Admin admin) {
    ad.setId(IdGenerator.next());
    mapper.insert(ad);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取")
  @Authentication(value = "admin:ad:detail", tags = {"推广管理", "广告管理", "详情"})
  public SingleResponse<Advertise> get(@PathParam("id") String id,
                                       @Context Admin admin) {
    Advertise v = mapper.findById(id);
    if (v == null) {
      throw new com.github.sun.foundation.boot.exception.NotFoundException("Can not find ad by id=" + id);
    }
    return responseOf(v);
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("编辑")
  @Authentication(value = "admin:ad:update", tags = {"推广管理", "广告管理", "编辑"})
  public Response update(@PathParam("id") String id,
                         @Valid @NotNull(message = "缺少实体") Advertise v,
                         @Context Admin admin) {
    Advertise e = mapper.findById(id);
    if (e == null) {
      throw new com.github.sun.foundation.boot.exception.NotFoundException("Can not find ad by id=" + id);
    }
    v.setId(id);
    mapper.update(v);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  @Authentication(value = "admin:ad:delete", tags = {"推广管理", "广告管理", "删除"})
  public Response delete(@PathParam("id") String id,
                         @Context Admin admin) {
    Advertise v = mapper.findById(id);
    if (v == null) {
      throw new NotFoundException("Can not find ad by id=" + id);
    }
    mapper.deleteById(id);
    return responseOf();
  }

  @DELETE
  @Path("/[{ids}]")
  @ApiOperation("批量删除")
  @Authentication(value = "admin:ad:batchDelete", tags = {"推广管理", "广告管理", "批量删除"})
  public Response delete(@PathParam("ids") List<String> ids) {
    mapper.deleteByIds(ids);
    return responseOf();
  }
}
