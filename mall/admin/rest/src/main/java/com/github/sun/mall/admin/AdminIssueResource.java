package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.IssueMapper;
import com.github.sun.mall.core.entity.Issue;
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

@Path("/v1/mall/admin/issue")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 通用问题: issue")
public class AdminIssueResource extends AbstractResource {
  private final IssueMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminIssueResource(IssueMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取问题列表")
  @Authentication(value = "admin:issue:query", tags = {"商品管理", "通用问题", "查询"})
  public PageResponse<Issue> paged(@QueryParam("question") String question,
                                   @QueryParam("username") String username,
                                   @QueryParam("start") int start,
                                   @QueryParam("count") int count,
                                   @QueryParam("sort") @DefaultValue("createTime") String sort,
                                   @QueryParam("asc") boolean asc,
                                   @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(question).then(sb.field("userId").contains(question));
    int total = mapper.countByTemplate(sb.from(Issue.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Issue.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Issue> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @POST
  @ApiOperation("创建")
  @Authentication(value = "admin:issue:create", tags = {"商品管理", "通用问题", "添加"})
  public AbstractResource.Response create(@Valid @NotNull(message = "缺少实体") Issue v,
                                          @Context Admin admin) {
    v.setId(IdGenerator.next());
    mapper.insert(v);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取")
  @Authentication(value = "admin:issue:detail", tags = {"商品管理", "通用问题", "详情"})
  public AbstractResource.SingleResponse<Issue> get(@PathParam("id") String id,
                                                    @Context Admin admin) {
    Issue v = mapper.findById(id);
    if (v == null) {
      throw new com.github.sun.foundation.boot.exception.NotFoundException("Can not find Issue by id=" + id);
    }
    return responseOf(v);
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("编辑")
  @Authentication(value = "admin:issue:update", tags = {"商品管理", "通用问题", "编辑"})
  public AbstractResource.Response update(@PathParam("id") String id,
                                          @Valid @NotNull(message = "缺少实体") Issue v,
                                          @Context Admin admin) {
    Issue e = mapper.findById(id);
    if (e == null) {
      throw new com.github.sun.foundation.boot.exception.NotFoundException("Can not find Issue by id=" + id);
    }
    v.setId(id);
    mapper.update(v);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  @Authentication(value = "admin:issue:delete", tags = {"商品管理", "通用问题", "删除"})
  public AbstractResource.Response delete(@PathParam("id") String id,
                                          @Context Admin admin) {
    Issue v = mapper.findById(id);
    if (v == null) {
      throw new NotFoundException("Can not find Issue by id=" + id);
    }
    mapper.deleteById(id);
    return responseOf();
  }
}
