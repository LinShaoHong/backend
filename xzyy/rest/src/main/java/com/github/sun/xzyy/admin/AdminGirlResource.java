package com.github.sun.xzyy.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.xzyy.Girl;
import com.github.sun.xzyy.GirlMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/xzyy/admin/girl")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Girl Resource")
public class AdminGirlResource extends AbstractResource {
  private final GirlMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminGirlResource(GirlMapper mapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取")
  public PageResponse<Girl> paged(@QueryParam("id") String id,
                                  @QueryParam("type") String type,
                                  @QueryParam("city") String city,
                                  @QueryParam("name") String name,
                                  @QueryParam("start") int start,
                                  @QueryParam("count") int count,
                                  @DefaultValue("updateTime") @QueryParam("rank") String rank) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonEmpty(type).then(sb.field("type").eq(type))
      .and(id == null ? null : sb.field("id").eq(id))
      .and(city == null ? null : sb.field("city").eq(city))
      .and(name == null ? null : sb.field("name").contains(name));
    int total = mapper.countByTemplate(sb.from(Girl.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Girl.class)
        .where(condition)
        .desc(rank)
        .limit(start, count)
        .template();
      final List<Girl> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @POST
  @ApiOperation("添加")
  public Response create(@Valid @NotNull(message = "缺少实体") Girl v) {
    v.setId(IdGenerator.next());
    mapper.insert(v);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("详情")
  public SingleResponse<Girl> create(@PathParam("id") String id) {
    return responseOf(mapper.findById(id));
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  public Response delete(@PathParam("id") String id) {
    Girl girl = mapper.findById(id);
    if (girl != null && girl.isOnService()) {
      girl.setOnService(false);
      mapper.update(girl);
    }
    return responseOf();
  }

  @PUT
  @Path("/publish/${id}")
  @ApiOperation("上课")
  public Response publish(@PathParam("id") String id) {
    Girl girl = mapper.findById(id);
    if (girl != null && !girl.isOnService()) {
      girl.setOnService(true);
      mapper.update(girl);
    }
    return responseOf();
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("修改")
  public Response create(@PathParam("id") String id,
                         @Valid @NotNull(message = "缺少实体") Girl v) {
    v.setId(id);
    mapper.update(v);
    return responseOf();
  }
}
