package com.github.sun.mall.core;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.core.entity.Order;
import com.github.sun.mall.core.resolver.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/v1/mall/mine")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-core: 我的服务")
public class MineResource extends AbstractResource {
  private final OrderMapper orderMapper;

  @Inject
  public MineResource(OrderMapper orderMapper) {
    this.orderMapper = orderMapper;
  }

  /**
   * 目前是用户订单统计信息
   */
  @GET
  @ApiOperation("用户个人页面数据")
  public SingleResponse<MineResp> index(@Context LoginUser user) {
    List<Order> orders = orderMapper.findByUserId(user.getId());
    return responseOf(MineResp.builder()
      .unPaid(orders.stream().filter(v -> v.getStatus() == Order.Status.CREATE).count())
      .unShip(orders.stream().filter(v -> v.getStatus() == Order.Status.PAY).count())
      .unRecv(orders.stream().filter(v -> v.getStatus() == Order.Status.SHIP).count())
      .unComment(orders.stream().filter(v -> v.getStatus() == Order.Status.CONFIRM || v.getStatus() == Order.Status.AUTO_CONFIRM).count())
      .build());
  }

  @Data
  @Builder
  private static class MineResp {
    private long unPaid;
    private long unShip;
    private long unRecv;
    private long unComment;
  }
}
