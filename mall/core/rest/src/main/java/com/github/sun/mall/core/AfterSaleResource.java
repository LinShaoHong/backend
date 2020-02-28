package com.github.sun.mall.core;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.entity.AfterSale;
import com.github.sun.mall.core.entity.Order;
import com.github.sun.mall.core.entity.User;
import com.github.sun.mall.core.resolver.LoginUser;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 目前只支持订单整体售后，不支持订单商品单个售后
 * 一个订单只能有一个售后记录
 */
@Path("/v1/mall/afterSale")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-core: 售后服务")
public class AfterSaleResource extends AbstractResource {
  private final AfterSaleMapper mapper;
  private final AfterSaleService service;
  private final OrderMapper orderMapper;
  private final OrderMapper.Goods orderGoodsMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public AfterSaleResource(AfterSaleMapper mapper,
                           AfterSaleService service,
                           OrderMapper orderMapper,
                           OrderMapper.Goods orderGoodsMapper,
                           @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.service = service;
    this.orderMapper = orderMapper;
    this.orderGoodsMapper = orderGoodsMapper;
    this.factory = factory;
  }

  @GET
  @ApiOperation("获取售后列表")
  public PageResponse<AfterSaleResp> getAll(@QueryParam("start") int start,
                                            @QueryParam("count") int count,
                                            @QueryParam("status") Integer status,
                                            @QueryParam("sort") @DefaultValue("createTime") String sort,
                                            @QueryParam("asc") boolean asc,
                                            @Context LoginUser user) {
    SqlBuilder sb = factory.create();
    Expression condition = sb.field("userId").eq(user.getId())
      .and(status == null ? null : sb.field("status").eq(status));
    int total = mapper.countByTemplate(sb.from(AfterSale.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(AfterSale.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      List<AfterSale> list = mapper.findByTemplate(template);
      if (!list.isEmpty()) {
        Set<String> orderIds = list.stream().map(AfterSale::getOrderId).collect(Collectors.toSet());
        Map<String, List<Order.Goods>> goods = orderGoodsMapper.findByUserIdAndOrderIdIn(user.getId(), orderIds)
          .stream()
          .collect(Collectors.groupingBy(Order.Goods::getOrderId));
        List<AfterSaleResp> values = list.stream()
          .map(v -> AfterSaleResp.builder()
            .afterSale(v)
            .goodsList(goods.getOrDefault(v.getOrderId(), Collections.emptyList()))
            .build())
          .collect(Collectors.toList());
        return responseOf(total, values);
      }
    }
    return responseOf(total, Collections.emptyList());
  }

  @Data
  @Builder
  private static class AfterSaleResp {
    @JsonUnwrapped
    private AfterSale afterSale;
    private List<Order.Goods> goodsList;
  }


  @GET
  @Path("/byOrder")
  @ApiOperation("根据订单获取对应的售后")
  public SingleResponse<AfterSaleOrderResp> get(@NotNull(message = "缺少相应订单") @QueryParam("orderId") String orderId,
                                                @Context LoginUser user) {
    AfterSale afterSale = mapper.findByUserIdAndOrderId(user.getId(), orderId);
    if (afterSale == null) {
      return responseOf(AfterSaleOrderResp.builder().build());
    }
    Order order = orderMapper.findById(orderId);
    if (order == null) {
      throw new NotFoundException("Can not find order by id=" + orderId);
    }
    List<Order.Goods> goodsList = orderGoodsMapper.findByUserIdAndOrderId(user.getId(), orderId);
    return responseOf(AfterSaleOrderResp.builder()
      .afterSale(afterSale)
      .order(order)
      .goodsList(goodsList)
      .build());
  }

  @Data
  @Builder
  private static class AfterSaleOrderResp {
    @JsonUnwrapped
    private AfterSale afterSale;
    private Order order;
    private List<Order.Goods> goodsList;
  }

  @POST
  @Path("/apply")
  @ApiOperation("申请售后")
  public Response apply(@Valid @NotNull(message = "缺少实体") AfterSale afterSale,
                        @Context LoginUser user) {
    service.apply(user.getId(), afterSale);
    return responseOf();
  }

  /**
   * 如果管理员还没有审核，用户可以取消自己的售后申请
   */
  @POST
  @Path("/${id}/cancel")
  @ApiOperation("取消售后")
  public Response cancel(@PathParam("id") String id,
                         @Context LoginUser user) {
    service.cancel(user.getId(), id);
    return responseOf();
  }
}
