package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.KeywordMapper;
import com.github.sun.mall.core.entity.Keyword;
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

@Path("/v1/mall/admin/keyword")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 关键字管理: keyword")
public class AdminKeywordResource extends AbstractResource {
  private final KeywordMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminKeywordResource(KeywordMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取关键字列表")
  @Authentication(value = "admin:keyword:query", tags = {"商场管理", "关键词", "查询"})
  public PageResponse<Keyword> paged(@QueryParam("keyword") String keyword,
                                     @QueryParam("url") String url,
                                     @QueryParam("start") int start,
                                     @QueryParam("count") int count,
                                     @QueryParam("sort") @DefaultValue("createTime") String sort,
                                     @QueryParam("asc") boolean asc,
                                     @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(keyword).then(sb.field("keyword").contains(keyword))
      .and(url == null ? null : sb.field("username").contains(url));
    int total = mapper.countByTemplate(sb.from(Keyword.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Keyword.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Keyword> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @POST
  @ApiOperation("创建")
  @Authentication(value = "admin:keyword:create", tags = {"商场管理", "关键词", "添加"})
  public AbstractResource.Response create(@Valid @NotNull(message = "缺少实体") Keyword v,
                                          @Context Admin admin) {
    v.setId(IdGenerator.next());
    mapper.insert(v);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取")
  @Authentication(value = "admin:keyword:detail", tags = {"商场管理", "关键词", "详情"})
  public AbstractResource.SingleResponse<Keyword> get(@PathParam("id") String id,
                                                      @Context Admin admin) {
    Keyword v = mapper.findById(id);
    if (v == null) {
      throw new NotFoundException("Can not find Keyword by id=" + id);
    }
    return responseOf(v);
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("编辑")
  @Authentication(value = "admin:keyword:update", tags = {"商场管理", "关键词", "编辑"})
  public AbstractResource.Response update(@PathParam("id") String id,
                                          @Valid @NotNull(message = "缺少实体") Keyword v,
                                          @Context Admin admin) {
    Keyword e = mapper.findById(id);
    if (e == null) {
      throw new NotFoundException("Can not find Keyword by id=" + id);
    }
    v.setId(id);
    mapper.update(v);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  @Authentication(value = "admin:keyword:delete", tags = {"商场管理", "关键词", "删除"})
  public AbstractResource.Response delete(@PathParam("id") String id,
                                          @Context Admin admin) {
    Keyword v = mapper.findById(id);
    if (v == null) {
      throw new NotFoundException("Can not find Keyword by id=" + id);
    }
    mapper.deleteById(id);
    return responseOf();
  }
}
