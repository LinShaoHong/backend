package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.TopicMapper;
import com.github.sun.mall.core.entity.Topic;
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

@Path("/v1/mall/admin/topic")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 专题管理: topic")
public class AdminTopicResource extends AbstractResource {
  private final TopicMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminTopicResource(TopicMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取专题列表")
  @Authentication(value = "admin:topic:query", tags = {"推广管理", "专题管理", "查询"})
  public PageResponse<Topic> paged(@QueryParam("title") String title,
                                   @QueryParam("subtitle") String subtitle,
                                   @QueryParam("start") int start,
                                   @QueryParam("count") int count,
                                   @QueryParam("sort") @DefaultValue("createTime") String sort,
                                   @QueryParam("asc") boolean asc,
                                   @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(title).then(sb.field("Topic").contains(title))
      .and(subtitle == null ? null : sb.field("subtitle").contains(subtitle));
    int total = mapper.countByTemplate(sb.from(Topic.class).where(condition).count().template());
    if (start < total) {
      SqlBuilder.Template template = sb.from(Topic.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Topic> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @POST
  @ApiOperation("创建")
  @Authentication(value = "admin:topic:create", tags = {"推广管理", "专题管理", "添加"})
  public AbstractResource.Response create(@Valid @NotNull(message = "缺少实体") Topic v,
                                          @Context Admin admin) {
    v.setId(IdGenerator.next());
    mapper.insert(v);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取")
  @Authentication(value = "admin:topic:detail", tags = {"推广管理", "专题管理", "详情"})
  public AbstractResource.SingleResponse<Topic> get(@PathParam("id") String id,
                                                    @Context Admin admin) {
    Topic v = mapper.findById(id);
    if (v == null) {
      throw new com.github.sun.foundation.boot.exception.NotFoundException("Can not find Topic by id=" + id);
    }
    return responseOf(v);
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("编辑")
  @Authentication(value = "admin:topic:update", tags = {"推广管理", "专题管理", "编辑"})
  public AbstractResource.Response update(@PathParam("id") String id,
                                          @Valid @NotNull(message = "缺少实体") Topic v,
                                          @Context Admin admin) {
    Topic e = mapper.findById(id);
    if (e == null) {
      throw new com.github.sun.foundation.boot.exception.NotFoundException("Can not find Topic by id=" + id);
    }
    v.setId(id);
    mapper.update(v);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  @Authentication(value = "admin:topic:delete", tags = {"推广管理", "专题管理", "删除"})
  public AbstractResource.Response delete(@PathParam("id") String id,
                                          @Context Admin admin) {
    Topic v = mapper.findById(id);
    if (v == null) {
      throw new NotFoundException("Can not find Topic by id=" + id);
    }
    mapper.deleteById(id);
    return responseOf();
  }
}
