package com.github.sun.qm;

import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Path("/v1/qm/charge")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Charge Resource")
public class ChargeResource extends AbstractResource {
  private final ChargeService service;
  private final ChargeMapper.YQMapper yqMapper;
  private final GirlMapper girlMapper;
  private final PayLogMapper payLogMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public ChargeResource(ChargeService service,
                        ChargeMapper.YQMapper yqMapper,
                        GirlMapper girlMapper,
                        PayLogMapper payLogMapper,
                        @Named("mysql") SqlBuilder.Factory factory) {
    this.service = service;
    this.yqMapper = yqMapper;
    this.girlMapper = girlMapper;
    this.payLogMapper = payLogMapper;
    this.factory = factory;
  }

  @POST
  @Path("/recharge")
  @ApiOperation("充值")
  public Response recharge(@Valid @NotNull(message = "require body") RechargeReq req,
                           @Context User user) {
    service.recharge(req.getCardNo(), user);
    return responseOf();
  }

  @Data
  private static class RechargeReq {
    @NotEmpty(message = "缺少卡号")
    private String cardNo;
    private Charge.Type type;
  }

  @POST
  @Path("/consume")
  @ApiOperation("消费")
  public Response consume(@Valid @NotNull(message = "require body") ConsumeReq req,
                          @Context User user) {
    service.consume(req.getGirlId(), user);
    return responseOf();
  }

  @Data
  private static class ConsumeReq {
    @NotEmpty
    private String girlId;
  }

  private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd: HH:mm:ss");

  @GET
  @Path("/flow")
  @ApiOperation("付款流水")
  public PageResponse<FlowRes> flow(@QueryParam("start") int start,
                                    @QueryParam("count") int count,
                                    @QueryParam("type") String type,
                                    @Context User user) {
    SqlBuilder sb = factory.create();
    Expression condition = sb.field("userId").eq(user.getId())
      .and(type == null || type.isEmpty() ? null : sb.field("type").eq(type));
    int total = payLogMapper.countByTemplate(sb.from(PayLog.class).where(condition).count().template());
    if (total > 0) {
      sb.clear();
      SqlBuilder.Template template = sb.from(PayLog.class)
        .where(condition)
        .desc("createTime")
        .limit(start, count)
        .template();
      List<PayLog> list = payLogMapper.findByTemplate(template);
      Set<String> girlIds = list.stream().map(PayLog::getGirlId).filter(Objects::nonNull).collect(Collectors.toSet());
      Map<String, Girl> girls = girlIds.isEmpty() ? new HashMap<>()
        : girlMapper.findByIds(girlIds).stream().collect(Collectors.toMap(Girl::getId, v -> v));
      return responseOf(total, list.stream().map(v -> {
        Girl girl = null;
        if (v.getGirlId() != null) {
          girl = girls.get(v.getGirlId());
          if (girl == null) {
            return null;
          }
        }
        return FlowRes.builder()
          .amount(v.getAmount())
          .type(v.getType())
          .chargeType(v.getChargeType())
          .girl(girl == null ? null : GirlRes.from(girl))
          .time(FORMATTER.format(v.getCreateTime()))
          .build();
      }).filter(Objects::nonNull).collect(Collectors.toList()));
    }
    return responseOf(total, Collections.emptyList());
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class FlowRes {
    private BigDecimal amount;
    private PayLog.Type type;
    private String chargeType;
    private GirlRes girl;
    private String time;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class GirlRes {
    private String id;
    private String name;
    private String city;
    private Girl.Type type;
    private String mainImage;
    private boolean onService;

    private static GirlRes from(Girl v) {
      return GirlRes.builder()
        .id(v.getId())
        .name(v.getName())
        .city(v.getCity())
        .type(v.getType())
        .mainImage(v.getMainImage())
        .onService(v.isOnService())
        .build();
    }
  }

  @GET
  @Path("/yq")
  @ApiOperation("易千支付")
  public ListResponse<Charge.YQ> qyAll(@Context User user) {
    return responseOf(yqMapper.findAll());
  }
}
