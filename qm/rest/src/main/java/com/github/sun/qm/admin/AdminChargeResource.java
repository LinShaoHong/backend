package com.github.sun.qm.admin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.qm.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Path("/v1/qm/admin/charge")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Charge Resource")
public class AdminChargeResource extends AdminBasicResource {
  private final ChargeMapper mapper;
  private final PayLogMapper payLogMapper;
  private final ChargeMapper.YQMapper yqMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminChargeResource(UserMapper userMapper,
                             GirlMapper girlMapper,
                             ChargeMapper mapper,
                             ChargeMapper.YQMapper yqMapper,
                             PayLogMapper payLogMapper,
                             @Named("mysql") SqlBuilder.Factory factory) {
    super(userMapper, girlMapper);
    this.mapper = mapper;
    this.yqMapper = yqMapper;
    this.factory = factory;
    this.payLogMapper = payLogMapper;
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

  @GET
  @Path("/unused")
  @ApiOperation("分页获取")
  public void unused(@QueryParam("type") String type) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Charge.class)
      .where(sb.field("type").eq(type))
      .where(sb.field("used").eq(false))
      .template();
    System.out.println(mapper.findByTemplate(template)
      .stream()
      .map(Charge::getId)
      .collect(Collectors.joining("\n")));
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

  @GET
  @Path("/total")
  public SingleResponse<BigDecimal> rechargeTotal(@Context Admin admin) {
    return responseOf(mapper.rechargeTotal());
  }

  @GET
  @Path("/payLog")
  public PageResponse<ObjectNode> payLog(@QueryParam("start") int start,
                                         @QueryParam("count") int count,
                                         @QueryParam("type") String type,
                                         @QueryParam("chargeType") String chargeType,
                                         @QueryParam("userName") String userName,
                                         @QueryParam("girlId") String girlId,
                                         @DefaultValue("updateTime") @QueryParam("rank") String rank,
                                         @Context Admin admin) {
    String userId = null;
    if (userName != null && !userName.isEmpty()) {
      userId = userMapper.findIdByUsername(userName.trim());
    }
    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonEmpty(type).then(sb.field("type").eq(type))
      .and(userId == null || userId.isEmpty() ? null : sb.field("userId").eq(userId))
      .and(chargeType == null || chargeType.isEmpty() ? null : sb.field("chargeType").eq(chargeType))
      .and(girlId == null || girlId.isEmpty() ? null : sb.field("girlId").eq(girlId));
    int total = mapper.countByTemplate(sb.from(PayLog.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(PayLog.class)
        .where(condition)
        .desc(rank)
        .limit(start, count)
        .template();
      List<PayLog> list = payLogMapper.findByTemplate(template);
      return responseOf(total, join(list, "userId", "girlId"));
    }
    return responseOf(total, Collections.emptyList());
  }

  @GET
  @Path("/stat")
  @ApiOperation("统计充值金额")
  public SingleResponse<StatResp> statRecharge(@QueryParam("groupType") String groupType,
                                               @QueryParam("timeType") int timeType,
                                               @Context Admin admin) {
    SqlBuilder sb = factory.create();
    Calendar c = Calendar.getInstance();
    Date now = new Date();
    c.setTime(now);
    switch (timeType) {
      case 1:
        if (groupType.equals("day")) {
          c.add(Calendar.DAY_OF_WEEK, -7);
        } else {
          c.add(Calendar.MONTH, -1);
        }
        break;
      case 2:
        if (groupType.equals("day")) {
          c.add(Calendar.MONTH, -1);
        } else {
          c.add(Calendar.MONTH, -3);
        }
        break;
      case 3:
        if (groupType.equals("day")) {
          c.add(Calendar.MONTH, -3);
        } else {
          c.add(Calendar.MONTH, -6);
        }
        break;
      case 4:
        if (groupType.equals("day")) {
          c.add(Calendar.MONTH, -6);
        } else {
          c.add(Calendar.YEAR, -1);
        }
        break;
      case 5:
        if (groupType.equals("day")) {
          c.add(Calendar.YEAR, -1);
        } else {
          c.add(Calendar.YEAR, -3);
        }
        break;
    }
    SqlBuilder.Template template = sb.from(PayLog.class)
      .where(sb.field("type").eq("RECHARGE"))
      .select(sb.field("amount").sum())
      .template();
    BigDecimal total = (BigDecimal) mapper.findOneByTemplateAsMap(template).values().iterator().next();

    template = sb.from(PayLog.class)
      .where(sb.field("type").eq("RECHARGE"))
      .where(sb.id("UNIX_TIMESTAMP").call(sb.field("createTime")).ge(c.getTimeInMillis() / 1000))
      .groupBy(sb.field("substr").call(sb.field("createTime"), 1, groupType.equals("day") ? 10 : 7))
      .select(sb.field("substr").call(sb.field("createTime"), 1, groupType.equals("day") ? 10 : 7), "time")
      .select(sb.field("amount").sum(), "total")
      .template();
    List<Map<String, Object>> list = mapper.findByTemplateAsMap(template);

    List<String> times = new ArrayList<>();
    List<BigDecimal> nums = new ArrayList<>();
    List<BigDecimal> totals = new ArrayList<>();

    Collections.reverse(list);
    for (Map<String, Object> map : list) {
      times.add((String) map.get("time"));
      BigDecimal inc = (BigDecimal) map.get("total");
      nums.add(inc);

      totals.add(total);
      total = total.subtract(inc);
    }

    Collections.reverse(times);
    Collections.reverse(nums);
    Collections.reverse(totals);
    return responseOf(StatResp.builder()
      .times(times)
      .nums(nums)
      .totals(totals)
      .build());
  }

  @Data
  @Builder
  private static class StatResp {
    private List<String> times;
    private List<BigDecimal> nums;
    private List<BigDecimal> totals;
  }
}
