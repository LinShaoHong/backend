package com.github.sun.mall.core;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.expression.Expression;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.entity.Order;
import com.github.sun.mall.core.entity.User;
import com.github.sun.mall.core.resolver.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.sun.mall.core.entity.Order.Status;

@Path("/v1/mall/order")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall: Order Resource", tags = "订单服务")
public class OrderResource extends AbstractResource {
  private final OrderMapper mapper;
  private final OrderService service;
  private final GrouponMapper grouponMapper;
  private final OrderMapper.Goods orderGoodsMapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public OrderResource(OrderMapper mapper,
                       OrderService service, GrouponMapper grouponMapper,
                       OrderMapper.Goods orderGoodsMapper,
                       @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.service = service;
    this.grouponMapper = grouponMapper;
    this.orderGoodsMapper = orderGoodsMapper;
    this.factory = factory;
  }

  public enum ShowType {
    ALL(Arrays.asList(Status.values())),
    PAYING(Collections.singletonList(Status.CREATE)),
    SENDING(Collections.singletonList(Status.PAY)),
    RECEIVING(Collections.singletonList(Status.SHIP)),
    EVALUATING(Collections.singletonList(Status.CONFIRM));

    private final List<Status> status;

    ShowType(List<Order.Status> status) {
      this.status = status;
    }
  }

  @GET
  @ApiOperation("获取订单列表")
  public PageResponse<Order> list(@QueryParam("showType") @DefaultValue("ALL") ShowType showType,
                                  @QueryParam("start") int start,
                                  @QueryParam("count") int count,
                                  @QueryParam("sort") @DefaultValue("createTime") String sort,
                                  @QueryParam("asc") boolean asc,
                                  @Context LoginUser user) {
    SqlBuilder sb = factory.create();
    Expression condition = sb.field("status").in(showType.status.stream().map(Enum::name).collect(Collectors.toSet()));
    int total = mapper.countByTemplate(sb.from(Order.class).where(condition).count().template());
    if (start < total) {
      sb.clear();
      SqlBuilder.Template template = sb.from(Order.class)
        .where(condition)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      List<Order> list = mapper.findByTemplate(template);
      return responseOf(total, list);
    }
    return responseOf(total, Collections.emptyList());
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取单个订单详情")
  public SingleResponse<OrderResp> detail(@PathParam("id") String id, @Context LoginUser user) {
    Order order = mapper.findById(id);
    if (order == null) {
      throw new NotFoundException("Can not find order by id=" + id);
    }
    List<Order.Goods> goodsList = orderGoodsMapper.findByUserIdAndOrderId(user.getId(), id);
    return responseOf(OrderResp.builder().order(order).goodsList(goodsList).build());
  }

  @Data
  @Builder
  private static class OrderResp {
    @JsonUnwrapped
    private Order order;
    private List<Order.Goods> goodsList;
  }

  /**
   * 提交订单
   *
   * @param userId 用户ID
   * @param body   订单信息，{ cartId：xxx, addressId: xxx, couponId: xxx, message: xxx, grouponRulesId: xxx,  grouponLinkId: xxx}
   * @return 提交订单操作结果
   */
  @POST
  @ApiOperation("提交订单")
  public Object submit(@NotNull(message = "缺少实体") OrderReq order, @Context LoginUser user) {
    return wxOrderService.submit(userId, body);
  }

  @Data
  private static class OrderReq {
    private String cartId;
    private String addressId;
    private String couponId;
    private String message;
    private String grouponRulesId;
    private String grouponLinkId;
  }

  /**
   * 取消订单
   *
   * @param userId 用户ID
   * @param body   订单信息，{ orderId：xxx }
   * @return 取消订单操作结果
   */
  @PostMapping("cancel")
  public Object cancel(@LoginUser Integer userId, @RequestBody String body) {
    return wxOrderService.cancel(userId, body);
  }

  /**
   * 付款订单的预支付会话标识
   *
   * @param userId 用户ID
   * @param body   订单信息，{ orderId：xxx }
   * @return 支付订单ID
   */
  @PostMapping("prepay")
  public Object prepay(@LoginUser Integer userId, @RequestBody String body, HttpServletRequest request) {
    return wxOrderService.prepay(userId, body, request);
  }

  /**
   * 微信H5支付
   *
   * @param userId
   * @param body
   * @param request
   * @return
   */
  @PostMapping("h5pay")
  public Object h5pay(@LoginUser Integer userId, @RequestBody String body, HttpServletRequest request) {
    return wxOrderService.h5pay(userId, body, request);
  }

  /**
   * 微信付款成功或失败回调接口
   * <p>
   * TODO
   * 注意，这里pay-notify是示例地址，建议开发者应该设立一个隐蔽的回调地址
   *
   * @param request  请求内容
   * @param response 响应内容
   * @return 操作结果
   */
  @PostMapping("pay-notify")
  public Object payNotify(HttpServletRequest request, HttpServletResponse response) {
    return wxOrderService.payNotify(request, response);
  }

  /**
   * 订单申请退款
   *
   * @param userId 用户ID
   * @param body   订单信息，{ orderId：xxx }
   * @return 订单退款操作结果
   */
  @PostMapping("refund")
  public Object refund(@LoginUser Integer userId, @RequestBody String body) {
    return wxOrderService.refund(userId, body);
  }

  /**
   * 确认收货
   *
   * @param userId 用户ID
   * @param body   订单信息，{ orderId：xxx }
   * @return 订单操作结果
   */
  @PostMapping("confirm")
  public Object confirm(@LoginUser Integer userId, @RequestBody String body) {
    return wxOrderService.confirm(userId, body);
  }

  /**
   * 删除订单
   *
   * @param userId 用户ID
   * @param body   订单信息，{ orderId：xxx }
   * @return 订单操作结果
   */
  @PostMapping("delete")
  public Object delete(@LoginUser Integer userId, @RequestBody String body) {
    return wxOrderService.delete(userId, body);
  }

  /**
   * 待评价订单商品信息
   *
   * @param userId  用户ID
   * @param orderId 订单ID
   * @param goodsId 商品ID
   * @return 待评价订单商品信息
   */
  @GetMapping("goods")
  public Object goods(@LoginUser Integer userId,
                      @NotNull Integer orderId,
                      @NotNull Integer goodsId) {
    return wxOrderService.goods(userId, orderId, goodsId);
  }

  /**
   * 评价订单商品
   *
   * @param userId 用户ID
   * @param body   订单信息，{ orderId：xxx }
   * @return 订单操作结果
   */
  @PostMapping("comment")
  public Object comment(@LoginUser Integer userId, @RequestBody String body) {
    return wxOrderService.comment(userId, body);
  }

}
