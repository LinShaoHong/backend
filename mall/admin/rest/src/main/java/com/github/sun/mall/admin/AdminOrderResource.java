package com.github.sun.mall.admin;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.admin.api.AdminOrderService;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.OrderMapper;
import com.github.sun.mall.core.UserMapper;
import com.github.sun.mall.core.entity.Order;
import com.github.sun.mall.core.entity.User;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;

@Path("/v1/mall/admin/order")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 订单管理: order")
public class AdminOrderResource extends AbstractResource {
  private final OrderMapper mapper;
  private final AdminOrderService service;
  private final OrderMapper.Goods orderGoodsMapper;
  private final UserMapper userMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminOrderResource(OrderMapper mapper,
                            AdminOrderService service,
                            OrderMapper.Goods orderGoodsMapper,
                            UserMapper userMapper,
                            @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.service = service;
    this.orderGoodsMapper = orderGoodsMapper;
    this.userMapper = userMapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("分页查询订单")
  @Authentication(value = "admin:order:query", tags = {"商场管理", "订单管理", "查询"})
  public PageResponse<Order> list(@QueryParam("userId") String userId,
                                  @QueryParam("orderSn") String orderSn,
                                  @QueryParam("startTime") String startTime,
                                  @QueryParam("endTime") String endTime,
                                  @QueryParam("statuses") List<Order.Status> statuses,
                                  @QueryParam("start") int start,
                                  @QueryParam("count") int count,
                                  @QueryParam("sort") @DefaultValue("createTime") String sort,
                                  @QueryParam("asc") boolean asc,
                                  @Context Admin admin) {

    SqlBuilder sb = factory.create();
    Expression condition = Expression.nonNull(userId).then(sb.field("userId").eq(userId))
      .and(orderSn == null ? null : sb.field("orderSn").eq(orderSn))
      .and(statuses == null || statuses.isEmpty() ? null : sb.field("status").in(statuses))
      .and(startTime == null ? null : sb.field("createTime").ge(startTime))
      .and(endTime == null ? null : sb.field("endTime").le(endTime));
    int total = mapper.countByTemplate(sb.from(Order.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Order.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      final List<Order> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @GET
  @Path("/channel")
  @ApiOperation("查询物流公司")
  public Object channel(@Context Admin admin) {
    // todo
    return null;
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取订单详情")
  @Authentication(value = "admin:order:detail", tags = {"商场管理", "订单管理", "详情"})
  public Object detail(@PathParam("id") String id,
                       @Context Admin admin) {
    Order order = mapper.findById(id);
    if (order == null) {
      throw new NotFoundException("Can not find order by id" + id);
    }
    List<Order.Goods> orderGoods = orderGoodsMapper.findByOrderId(order.getId());
    User user = userMapper.findById(order.getId());
    return responseOf(OrderResp.builder()
      .order(order)
      .orderGoods(orderGoods)
      .user(UserResp.builder()
        .nickname(user.getNickname())
        .avatar(user.getAvatar())
        .build())
      .build());
  }

  @Data
  @Builder
  private static class OrderResp {
    @JsonUnwrapped
    private Order order;
    private List<Order.Goods> orderGoods;
    private UserResp user;
  }

  @Data
  @Builder
  private static class UserResp {
    private String nickname;
    private String avatar;
  }

  @POST
  @Path("/refund")
  @ApiOperation("订单退款")
  @Authentication(value = "admin:order:refund", tags = {"商场管理", "订单管理", "退款"})
  public Object refund(@Valid @NotNull(message = "缺少实体") String body,
                       @Context Admin admin) {
    // todo
//    return service.refund(body);
    return responseOf();
  }


  @POST
  @Path("/ship")
  @ApiOperation("发货")
  @Authentication(value = "admin:order:ship", tags = {"商场管理", "订单管理", "发货"})
  public Object ship(@Valid @NotNull(message = "缺少实体") String body,
                     @Context Admin admin) {
    // todo
//    return service.ship(body);
    return responseOf();
  }

  @POST
  @Path("/reply")
  @ApiOperation("回复订单商品")
  @Authentication(value = "admin:order:reply", tags = {"商场管理", "订单管理", "订单回复"})
  public Object reply(@Valid @NotNull(message = "缺少实体") String body,
                      @Context Admin admin) {
    // todo
//    return service.reply(body);
    return responseOf();
  }
}
