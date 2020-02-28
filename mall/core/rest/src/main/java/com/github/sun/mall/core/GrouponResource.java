package com.github.sun.mall.core;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.sun.foundation.boot.exception.BadRequestException;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.mall.core.entity.Goods;
import com.github.sun.mall.core.entity.Groupon;
import com.github.sun.mall.core.entity.Order;
import com.github.sun.mall.core.resolver.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;
import org.linlinjava.litemall.core.express.dao.ExpressInfo;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.*;
import org.linlinjava.litemall.db.service.*;
import org.linlinjava.litemall.db.util.OrderUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.*;
import java.util.stream.Collectors;

import static org.linlinjava.litemall.wx.util.WxResponseCode.*;

@Path("/v1/mall/groupon")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall: Groupon Resource", tags = "团购服务")
public class GrouponResource extends AbstractResource {
  private final GrouponMapper mapper;
  private final GoodsMapper goodsMapper;
  private final GrouponMapper.Rules grouponRuleMapper;
  private final OrderMapper orderMapper;
  private final OrderMapper.Goods orderGoodsMapper;
  private final SqlBuilder.Factory factory;

  public GrouponResource(GrouponMapper mapper,
                         GoodsMapper goodsMapper,
                         GrouponMapper.Rules grouponRuleMapper,
                         OrderMapper orderMapper,
                         OrderMapper.Goods orderGoodsMapper, @Named("mysql") SqlBuilder.Factory factory) {
    this.mapper = mapper;
    this.goodsMapper = goodsMapper;
    this.orderMapper = orderMapper;
    this.orderGoodsMapper = orderGoodsMapper;
    this.factory = factory;
    this.grouponRuleMapper = grouponRuleMapper;
  }

  @GET
  @ApiOperation("获取团购列表")
  public PageResponse<> getAll(@QueryParam("start") int start,
                               @QueryParam("count") int count,
                               @QueryParam("sort") @DefaultValue("createTime") String sort,
                               @QueryParam("asc") boolean asc) {
    SqlBuilder sb = factory.create();
    int total = grouponRuleMapper.count();
    if (start < total) {
      SqlBuilder.Template template = sb.from(Groupon.Rules.class)
        .orderBy(sort, asc)
        .limit(start, count)
        .template();
      List<Groupon.Rules> rules = grouponRuleMapper.findByTemplate(template);
      Set<String> goodsIds = rules.stream().map(Groupon.Rules::getGoodsId).collect(Collectors.toSet());
      Map<String, Goods> goodsMap = goodsMapper.findByIds(goodsIds).stream().collect(Collectors.toMap(Goods::getId, v -> v));
      return responseOf(total, rules.stream()
        .map(v -> GrouponResp.builder()
          .rules(v)
          .goods(goodsMap.get(v.getGoodsId()))
          .build())
        .collect(Collectors.toList()));
    }
    return responseOf(total, Collections.emptyList());
  }

  @Data
  @Builder
  private static class GrouponResp {
    @JsonUnwrapped
    private Groupon.Rules rules;
    private Goods goods;
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取团购活动详情")
  public Object detail(@PathParam("id") String id, @Context LoginUser user) {
    String userId = user.getId();
    Groupon groupon = mapper.findById(id);
    if (groupon == null) {
      throw new NotFoundException("Can not find groupon by id=" + id);
    }
    Groupon.Rules rules = grouponRuleMapper.findById(groupon.getRulesId());
    if (rules == null) {
      throw new NotFoundException("Can not find groupon rules by id=" + groupon.getRulesId());
    }
    // 订单信息
    Order order = orderMapper.findById(groupon.getOrderId());
    if (null == order) {
      throw new NotFoundException("Can not find order by id=" + groupon.getOrderId());
    }
    if (!order.getUserId().equals(userId)) {
      throw new BadRequestException("不是当前用户的订单");
    }
    Map<String, Object> orderVo = new HashMap<String, Object>();
    orderVo.put("id", order.getId());
    orderVo.put("orderSn", order.getOrderSn());
    orderVo.put("addTime", order.getAddTime());
    orderVo.put("consignee", order.getConsignee());
    orderVo.put("mobile", order.getMobile());
    orderVo.put("address", order.getAddress());
    orderVo.put("goodsPrice", order.getGoodsPrice());
    orderVo.put("freightPrice", order.getFreightPrice());
    orderVo.put("actualPrice", order.getActualPrice());
    orderVo.put("orderStatusText", OrderUtil.orderStatusText(order));
    orderVo.put("handleOption", OrderUtil.build(order));
    orderVo.put("expCode", order.getShipChannel());
    orderVo.put("expNo", order.getShipSn());

    List<LitemallOrderGoods> orderGoodsList = orderGoodsService.queryByOid(order.getId());
    List<Map<String, Object>> orderGoodsVoList = new ArrayList<>(orderGoodsList.size());
    for (LitemallOrderGoods orderGoods : orderGoodsList) {
      Map<String, Object> orderGoodsVo = new HashMap<>();
      orderGoodsVo.put("id", orderGoods.getId());
      orderGoodsVo.put("orderId", orderGoods.getOrderId());
      orderGoodsVo.put("goodsId", orderGoods.getGoodsId());
      orderGoodsVo.put("goodsName", orderGoods.getGoodsName());
      orderGoodsVo.put("number", orderGoods.getNumber());
      orderGoodsVo.put("retailPrice", orderGoods.getPrice());
      orderGoodsVo.put("picUrl", orderGoods.getPicUrl());
      orderGoodsVo.put("goodsSpecificationValues", orderGoods.getSpecifications());
      orderGoodsVoList.add(orderGoodsVo);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("orderInfo", orderVo);
    result.put("orderGoods", orderGoodsVoList);

    // 订单状态为已发货且物流信息不为空
    //"YTO", "800669400640887922"
    if (order.getOrderStatus().equals(OrderUtil.STATUS_SHIP)) {
      ExpressInfo ei = expressService.getExpressInfo(order.getShipChannel(), order.getShipSn());
      result.put("expressInfo", ei);
    }

    UserVo creator = userService.findUserVoById(groupon.getCreatorUserId());
    List<UserVo> joiners = new ArrayList<>();
    joiners.add(creator);
    int linkGrouponId;
    // 这是一个团购发起记录
    if (groupon.getGrouponId() == 0) {
      linkGrouponId = groupon.getId();
    } else {
      linkGrouponId = groupon.getGrouponId();

    }
    List<LitemallGroupon> groupons = grouponService.queryJoinRecord(linkGrouponId);

    UserVo joiner;
    for (LitemallGroupon grouponItem : groupons) {
      joiner = userService.findUserVoById(grouponItem.getUserId());
      joiners.add(joiner);
    }

    result.put("linkGrouponId", linkGrouponId);
    result.put("creator", creator);
    result.put("joiners", joiners);
    result.put("groupon", groupon);
    result.put("rules", rules);
    return ResponseUtil.ok(result);
  }

  @POST
  @Path("/${id}/join")
  @ApiOperation("参加团购")
  public Object join(@PathParam("id") String id) {
    LitemallGroupon groupon = grouponService.queryById(grouponId);
    if (groupon == null) {
      return ResponseUtil.badArgumentValue();
    }

    LitemallGrouponRules rules = rulesService.findById(groupon.getRulesId());
    if (rules == null) {
      return ResponseUtil.badArgumentValue();
    }

    LitemallGoods goods = goodsService.findById(rules.getGoodsId());
    if (goods == null) {
      return ResponseUtil.badArgumentValue();
    }

    Map<String, Object> result = new HashMap<>();
    result.put("groupon", groupon);
    result.put("goods", goods);

    return ResponseUtil.ok(result);
  }

  /**
   * 用户开团或入团情况
   *
   * @param userId   用户ID
   * @param showType 显示类型，如果是0，则是当前用户开的团购；否则，则是当前用户参加的团购
   * @return 用户开团或入团情况
   */
  @GetMapping("my")
  public Object my(@LoginUser Integer userId, @RequestParam(defaultValue = "0") Integer showType) {
    if (userId == null) {
      return ResponseUtil.unlogin();
    }

    List<LitemallGroupon> myGroupons;
    if (showType == 0) {
      myGroupons = grouponService.queryMyGroupon(userId);
    } else {
      myGroupons = grouponService.queryMyJoinGroupon(userId);
    }

    List<Map<String, Object>> grouponVoList = new ArrayList<>(myGroupons.size());

    LitemallOrder order;
    LitemallGrouponRules rules;
    LitemallUser creator;
    for (LitemallGroupon groupon : myGroupons) {
      order = orderService.findById(userId, groupon.getOrderId());
      rules = rulesService.findById(groupon.getRulesId());
      creator = userService.findById(groupon.getCreatorUserId());

      Map<String, Object> grouponVo = new HashMap<>();
      //填充团购信息
      grouponVo.put("id", groupon.getId());
      grouponVo.put("groupon", groupon);
      grouponVo.put("rules", rules);
      grouponVo.put("creator", creator.getNickname());

      int linkGrouponId;
      // 这是一个团购发起记录
      if (groupon.getGrouponId() == 0) {
        linkGrouponId = groupon.getId();
        grouponVo.put("isCreator", creator.getId() == userId);
      } else {
        linkGrouponId = groupon.getGrouponId();
        grouponVo.put("isCreator", false);
      }
      int joinerCount = grouponService.countGroupon(linkGrouponId);
      grouponVo.put("joinerCount", joinerCount + 1);

      //填充订单信息
      grouponVo.put("orderId", order.getId());
      grouponVo.put("orderSn", order.getOrderSn());
      grouponVo.put("actualPrice", order.getActualPrice());
      grouponVo.put("orderStatusText", OrderUtil.orderStatusText(order));

      List<LitemallOrderGoods> orderGoodsList = orderGoodsService.queryByOid(order.getId());
      List<Map<String, Object>> orderGoodsVoList = new ArrayList<>(orderGoodsList.size());
      for (LitemallOrderGoods orderGoods : orderGoodsList) {
        Map<String, Object> orderGoodsVo = new HashMap<>();
        orderGoodsVo.put("id", orderGoods.getId());
        orderGoodsVo.put("goodsName", orderGoods.getGoodsName());
        orderGoodsVo.put("number", orderGoods.getNumber());
        orderGoodsVo.put("picUrl", orderGoods.getPicUrl());
        orderGoodsVoList.add(orderGoodsVo);
      }
      grouponVo.put("goodsList", orderGoodsVoList);
      grouponVoList.add(grouponVo);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("total", grouponVoList.size());
    result.put("list", grouponVoList);

    return ResponseUtil.ok(result);
  }

}
