package com.github.sun.mall.admin;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Path("/v1/mall/admin/stat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 统计管理: stat")
public class AdminStatResource extends AbstractResource {
  private final UserMapper userMapper;
  private final OrderMapper orderMapper;
  private final OrderMapper.Goods goodsMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AdminStatResource(UserMapper userMapper,
                           OrderMapper orderMapper,
                           OrderMapper.Goods goodsMapper,
                           @Named("mysql") SqlBuilder.Factory factory) {
    this.userMapper = userMapper;
    this.orderMapper = orderMapper;
    this.goodsMapper = goodsMapper;
    this.factory = factory;
  }

  @GET
  @Path("/user")
  @ApiOperation("统计用户")
  @Authentication(value = "admin:stat:user", tags = {"统计管理", "用户统计", "查询"})
  public Object statUser(@Context Admin admin) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(User.class)
      .groupBy(sb.field("substr").call(sb.field("createTime"), 1, 10))
      .select(sb.field("substr").call(sb.field("createTime"), 1, 10), "day")
      .select(sb.field("id").distinct().count(), "users")
      .template();
    List<Map<String, Object>> list = userMapper.findByTemplateAsMap(template);
    return responseOf(StatResp.builder()
      .columns(Arrays.asList("day", "users"))
      .rows(list)
      .build());
  }

  @Data
  @Builder
  private static class StatResp {
    private List<String> columns;
    private List<Map<String, Object>> rows;
  }

  @GET
  @Path("/order")
  @ApiOperation("统计订单")
  @Authentication(value = "admin:stat:order", tags = {"统计管理", "订单统计", "查询"})
  public Object statOrder(@Context Admin admin) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Order.class)
      .where(sb.field("status").in("", ""))
      .groupBy(sb.field("substr").call(sb.field("createTime"), 1, 10))
      .select(sb.field("substr").call(sb.field("createTime"), 1, 10), "day")
      .select(sb.field("id").distinct().count(), "orders")
      .select(sb.field("userId").distinct().count(), "customers")
      .select(sb.field("actualPrice").sum(), "amount")
      .select(sb.field("round").call(sb.field("actualPrice").sum().div(sb.field("userId").distinct().count()), 2), "pcr")
      .template();
    List<Map<String, Object>> list = orderMapper.findByTemplateAsMap(template);
    return responseOf(StatResp.builder()
      .columns(Arrays.asList("day", "orders", "customers", "amount", "pcr"))
      .rows(list)
      .build());
  }


  @GET
  @Path("/goods")
  @ApiOperation("统计商品")
  @Authentication(value = "admin:stat:goods", tags = {"统计管理", "商品统计", "查询"})
  public Object statGoods(@Context Admin admin) {
    SqlBuilder sb = factory.create();
    SqlBuilder.Template template = sb.from(Order.Goods.class)
      .groupBy(sb.field("substr").call(sb.field("createTime"), 1, 10))
      .select(sb.field("substr").call(sb.field("createTime"), 1, 10), "day")
      .select(sb.field("orderId").distinct().count(), "orders")
      .select(sb.field("number").sum(), "products")
      .select(sb.field("sum").call(sb.field("number").mul(sb.field("price"))), "amount")
      .template();
    List<Map<String, Object>> list = goodsMapper.findByTemplateAsMap(template);
    return responseOf(StatResp.builder()
      .columns(Arrays.asList("day", "orders", "products", "amount"))
      .rows(list)
      .build());
  }
}
