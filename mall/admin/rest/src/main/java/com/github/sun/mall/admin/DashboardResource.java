package com.github.sun.mall.admin;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.GoodsMapper;
import com.github.sun.mall.core.OrderMapper;
import com.github.sun.mall.core.UserMapper;
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

@Path("/v1/mall/admin/dashboard")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 后台首页: dashboard")
public class DashboardResource extends AbstractResource {
  private final UserMapper userMapper;
  private final GoodsMapper goodsMapper;
  private final OrderMapper orderMapper;

  @Inject
  public DashboardResource(UserMapper userMapper,
                           GoodsMapper goodsMapper,
                           OrderMapper orderMapper) {
    this.userMapper = userMapper;
    this.goodsMapper = goodsMapper;
    this.orderMapper = orderMapper;
  }

  @GET
  @ApiOperation("获取首页信息")
  public SingleResponse<Resp> info(@Context Admin admin) {
    int userTotal = userMapper.count();
    int goodsTotal = goodsMapper.count();
    int productTotal = goodsMapper.countProduct();
    int orderTotal = orderMapper.count();
    return responseOf(Resp.builder()
      .userTotal(userTotal)
      .goodsTotal(goodsTotal)
      .productTotal(productTotal)
      .orderTotal(orderTotal)
      .build());
  }

  @Data
  @Builder
  private static class Resp {
    private int userTotal;
    private int goodsTotal;
    private int productTotal;
    private int orderTotal;
  }
}
