package com.github.sun.qm.admin;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.qm.Charge;
import com.github.sun.qm.ChargeMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/v1/qm/admin/charge")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Message Resource")
public class AdminChargeResource extends AbstractResource {
  private final ChargeMapper mapper;
  private final ChargeMapper.YQMapper yqMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminChargeResource(ChargeMapper mapper,
                             ChargeMapper.YQMapper yqMapper,
                             @Named("mysql") SqlBuilder.Factory factory,
                             @Context Admin admin) {
    this.mapper = mapper;
    this.yqMapper = yqMapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页获取")
  public PageResponse<Charge> paged(@QueryParam("start") int start,
                                    @QueryParam("count") int count,
                                    @QueryParam("type") String type,
                                    @QueryParam("used") Boolean used,
                                    @DefaultValue("updateTime") @QueryParam("rank") String rank,
                                    @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonEmpty(type).then(sb.field("type").eq(type))
      .and(used == null ? null : sb.field("used").eq(used));
    int total = mapper.countByTemplate(sb.from(Charge.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Charge.class)
        .where(condition)
        .desc(rank)
        .limit(start, count)
        .template();
      List<Charge> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @POST
  @ApiOperation("添加")
  public Response add(@Valid ChargeReq req,
                      @Context Admin admin) {
    List<Charge> vs = Stream.of(req.getCards().replaceAll(" ", "").split("\n"))
      .map(card -> Charge.builder()
        .id(card)
        .type(Charge.Type.valueOf(req.getType()))
        .build())
      .collect(Collectors.toList());
    mapper.insertAll(vs);
    return responseOf();
  }

  @Data
  private static class ChargeReq {
    @NotEmpty
    private String type;
    @NotEmpty
    private String cards;
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  public Response delete(@PathParam("id") String id,
                         @Context Admin admin) {
    mapper.deleteById(id);
    return responseOf();
  }

  @POST
  @Path("/yq")
  public Response addYq(Charge.YQ yq,
                        @Context Admin admin) {
    Charge.YQ v = yqMapper.findById(yq.getType());
    if (v == null) {
      yqMapper.insert(yq);
    } else {
      v.setUrl(yq.getUrl());
      v.setAmount(yq.getAmount());
      yqMapper.update(yq);
    }
    return responseOf();
  }

  @GET
  @Path("/yq")
  public ListResponse<Charge.YQ> yqAll(@Context Admin admin) {
    return responseOf(yqMapper.findAll());
  }

  @DELETE
  @Path("/yq/${id}")
  public Response deleteYq(@PathParam("id") String id,
                           @Context Admin admin) {
    yqMapper.deleteById(id);
    return responseOf();
  }
}
