//package com.github.sun.mall.core;
//
//import com.fasterxml.jackson.annotation.JsonUnwrapped;
//import com.github.sun.foundation.boot.exception.BadRequestException;
//import com.github.sun.foundation.boot.exception.NotFoundException;
//import com.github.sun.foundation.rest.AbstractResource;
//import com.github.sun.foundation.sql.SqlBuilder;
//import com.github.sun.mall.core.entity.Goods;
//import com.github.sun.mall.core.entity.Groupon;
//import com.github.sun.mall.core.entity.Order;
//import com.github.sun.mall.core.entity.User;
//import com.github.sun.mall.core.resolver.LoginUser;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import lombok.Builder;
//import lombok.Data;
//import org.linlinjava.litemall.core.express.dao.ExpressInfo;
//import org.linlinjava.litemall.core.util.ResponseUtil;
//import org.linlinjava.litemall.db.domain.*;
//import org.linlinjava.litemall.db.service.*;
//import org.linlinjava.litemall.db.util.OrderUtil;
//
//import javax.inject.Named;
//import javax.ws.rs.*;
//import javax.ws.rs.core.Context;
//import javax.ws.rs.core.MediaType;
//import java.util.*;
//import java.util.stream.Collectors;
//
//import static org.linlinjava.litemall.wx.util.WxResponseCode.*;
//
//@Path("/v1/mall/groupon")
//@Consumes(MediaType.APPLICATION_JSON)
//@Produces(MediaType.APPLICATION_JSON)
//@Api(value = "mall: Groupon Resource", tags = "团购服务")
//public class GrouponResource extends AbstractResource {
//  private final GrouponMapper mapper;
//  private final GoodsMapper goodsMapper;
//  private final GrouponMapper.Rules grouponRuleMapper;
//  private final OrderMapper orderMapper;
//  private final OrderMapper.Goods orderGoodsMapper;
//  private final UserMapper userMapper;
//  private final SqlBuilder.Factory factory;
//
//  public GrouponResource(GrouponMapper mapper,
//                         GoodsMapper goodsMapper,
//                         GrouponMapper.Rules grouponRuleMapper,
//                         OrderMapper orderMapper,
//                         OrderMapper.Goods orderGoodsMapper,
//                         UserMapper userMapper,
//                         @Named("mysql") SqlBuilder.Factory factory) {
//    this.mapper = mapper;
//    this.goodsMapper = goodsMapper;
//    this.orderMapper = orderMapper;
//    this.orderGoodsMapper = orderGoodsMapper;
//    this.userMapper = userMapper;
//    this.factory = factory;
//    this.grouponRuleMapper = grouponRuleMapper;
//  }
//
//  @GET
//  @ApiOperation("获取团购列表")
//  public PageResponse<GrouponRulesResp> getAll(@QueryParam("start") int start,
//                                               @QueryParam("count") int count,
//                                               @QueryParam("sort") @DefaultValue("createTime") String sort,
//                                               @QueryParam("asc") boolean asc) {
//    SqlBuilder sb = factory.create();
//    int total = grouponRuleMapper.count();
//    if (start < total) {
//      SqlBuilder.Template template = sb.from(Groupon.Rules.class)
//        .orderBy(sort, asc)
//        .limit(start, count)
//        .template();
//      List<Groupon.Rules> rules = grouponRuleMapper.findByTemplate(template);
//      Set<String> goodsIds = rules.stream().map(Groupon.Rules::getGoodsId).collect(Collectors.toSet());
//      Map<String, Goods> goodsMap = goodsMapper.findByIds(goodsIds).stream().collect(Collectors.toMap(Goods::getId, v -> v));
//      return responseOf(total, rules.stream()
//        .map(v -> GrouponRulesResp.builder()
//          .rules(v)
//          .goods(goodsMap.get(v.getGoodsId()))
//          .build())
//        .collect(Collectors.toList()));
//    }
//    return responseOf(total, Collections.emptyList());
//  }
//
//  @Data
//  @Builder
//  private static class GrouponRulesResp {
//    @JsonUnwrapped
//    private Groupon.Rules rules;
//    private Goods goods;
//  }
//
//  @GET
//  @Path("/${id}")
//  @ApiOperation("获取团购活动详情")
//  public Object detail(@PathParam("id") String id, @Context LoginUser user) {
//    String userId = user.getId();
//    Groupon groupon = mapper.findById(id);
//    if (groupon == null) {
//      throw new NotFoundException("Can not find groupon by id=" + id);
//    }
//    Groupon.Rules rules = grouponRuleMapper.findById(groupon.getRulesId());
//    if (rules == null) {
//      throw new NotFoundException("Can not find groupon rules by id=" + groupon.getRulesId());
//    }
//    // 订单信息
//    Order order = orderMapper.findById(groupon.getOrderId());
//    if (null == order) {
//      throw new NotFoundException("Can not find order by id=" + groupon.getOrderId());
//    }
//    if (!order.getUserId().equals(userId)) {
//      throw new BadRequestException("不是当前用户的订单");
//    }
//    Map<String, Object> orderVo = new HashMap<String, Object>();
//    orderVo.put("id", order.getId());
//    orderVo.put("orderSn", order.getOrderSn());
//    orderVo.put("addTime", order.getAddTime());
//    orderVo.put("consignee", order.getConsignee());
//    orderVo.put("mobile", order.getMobile());
//    orderVo.put("address", order.getAddress());
//    orderVo.put("goodsPrice", order.getGoodsPrice());
//    orderVo.put("freightPrice", order.getFreightPrice());
//    orderVo.put("actualPrice", order.getActualPrice());
//    orderVo.put("orderStatusText", OrderUtil.orderStatusText(order));
//    orderVo.put("handleOption", OrderUtil.build(order));
//    orderVo.put("expCode", order.getShipChannel());
//    orderVo.put("expNo", order.getShipSn());
//
//    List<LitemallOrderGoods> orderGoodsList = orderGoodsService.queryByOid(order.getId());
//    List<Map<String, Object>> orderGoodsVoList = new ArrayList<>(orderGoodsList.size());
//    for (LitemallOrderGoods orderGoods : orderGoodsList) {
//      Map<String, Object> orderGoodsVo = new HashMap<>();
//      orderGoodsVo.put("id", orderGoods.getId());
//      orderGoodsVo.put("orderId", orderGoods.getOrderId());
//      orderGoodsVo.put("goodsId", orderGoods.getGoodsId());
//      orderGoodsVo.put("goodsName", orderGoods.getGoodsName());
//      orderGoodsVo.put("number", orderGoods.getNumber());
//      orderGoodsVo.put("retailPrice", orderGoods.getPrice());
//      orderGoodsVo.put("picUrl", orderGoods.getPicUrl());
//      orderGoodsVo.put("goodsSpecificationValues", orderGoods.getSpecifications());
//      orderGoodsVoList.add(orderGoodsVo);
//    }
//
//    Map<String, Object> result = new HashMap<>();
//    result.put("orderInfo", orderVo);
//    result.put("orderGoods", orderGoodsVoList);
//
//    // 订单状态为已发货且物流信息不为空
//    //"YTO", "800669400640887922"
//    if (order.getOrderStatus().equals(OrderUtil.STATUS_SHIP)) {
//      ExpressInfo ei = expressService.getExpressInfo(order.getShipChannel(), order.getShipSn());
//      result.put("expressInfo", ei);
//    }
//
//    UserVo creator = userService.findUserVoById(groupon.getCreatorUserId());
//    List<UserVo> joiners = new ArrayList<>();
//    joiners.add(creator);
//    int linkGrouponId;
//    // 这是一个团购发起记录
//    if (groupon.getGrouponId() == 0) {
//      linkGrouponId = groupon.getId();
//    } else {
//      linkGrouponId = groupon.getGrouponId();
//
//    }
//    List<LitemallGroupon> groupons = grouponService.queryJoinRecord(linkGrouponId);
//
//    UserVo joiner;
//    for (LitemallGroupon grouponItem : groupons) {
//      joiner = userService.findUserVoById(grouponItem.getUserId());
//      joiners.add(joiner);
//    }
//
//    result.put("linkGrouponId", linkGrouponId);
//    result.put("creator", creator);
//    result.put("joiners", joiners);
//    result.put("groupon", groupon);
//    result.put("rules", rules);
//    return ResponseUtil.ok(result);
//  }
//
//  @POST
//  @Path("/${id}/join")
//  @ApiOperation("参加团购")
//  public SingleResponse<GrouponResp> join(@PathParam("id") String id) {
//    Groupon groupon = mapper.findById(id);
//    if (groupon == null) {
//      throw new NotFoundException("Can not find groupon by id=" + id);
//    }
//    Groupon.Rules rules = grouponRuleMapper.findById(groupon.getRulesId());
//    if (rules == null) {
//      throw new NotFoundException("Can not find groupon rules by id=" + groupon.getRulesId());
//    }
//    Goods goods = goodsMapper.findById(rules.getGoodsId());
//    if (goods == null) {
//      throw new NotFoundException("Can not find goods by id=" + rules.getGoodsId());
//    }
//    return responseOf(GrouponResp.builder().groupon(groupon).goods(goods).build());
//  }
//
//  @Data
//  @Builder
//  private static class GrouponResp {
//    @JsonUnwrapped
//    private Groupon groupon;
//    private Goods goods;
//  }
//
//  public enum ShowType {
//    CREATOR, JOINER
//  }
//
//  @GET
//  @Path("/my")
//  @ApiOperation("用户开团或入团情况")
//  public ListResponse<MyGrouponResp> info(@QueryParam("showType") @DefaultValue("CREATOR") ShowType showType,
//                                          @Context LoginUser user) {
//    String userId = user.getId();
//    SqlBuilder sb = factory.create();
//    boolean flag = showType == ShowType.CREATOR;
//    SqlBuilder.Template template = sb.from(Groupon.class)
//      .where(sb.field("userId").eq(userId))
//      .where(flag ? sb.field("creatorId").eq(userId) : null)
//      .where(flag ? sb.field("grouponId").eq("0") : sb.field("grouponId").ne("0"))
//      .where(sb.field("status").ne(Groupon.Status.NONE.name()))
//      .orderBy("createTime", false)
//      .template();
//    List<Groupon> groupons = mapper.findByTemplate(template);
//    if (!groupons.isEmpty()) {
//      Set<String> orderIds = groupons.stream().map(Groupon::getOrderId).collect(Collectors.toSet());
//      Set<String> rulesIds = groupons.stream().map(Groupon::getRulesId).collect(Collectors.toSet());
//      Set<String> creatorIds = groupons.stream().map(Groupon::getCreatorId).collect(Collectors.toSet());
//
//      Map<String, Order> orderMap = orderMapper.findByIds(orderIds).stream().collect(Collectors.toMap(Order::getId, v -> v));
//      Map<String, Groupon.Rules> rulesMap = grouponRuleMapper.findByIds(rulesIds).stream().collect(Collectors.toMap(Groupon.Rules::getId, v -> v));
//      Map<String, User> creatorMap = userMapper.findByIds(creatorIds).stream().collect(Collectors.toMap(User::getId, v -> v));
//      Map<String, List<Order.Goods>> goodsMap = orderGoodsMapper.findByOrderIdIn(orderIds).stream().collect(Collectors.groupingBy(Order.Goods::getOrderId));
//
//      List<MyGrouponResp> list = groupons.stream().map(v -> {
//        Order order = orderMap.get(v.getOrderId());
//        Groupon.Rules rules = rulesMap.get(v.getRulesId());
//        User creator = creatorMap.get(v.getCreatorId());
//        List<Order.Goods> goods = goodsMap.get(v.getOrderId());
//        boolean isCreator = false;
//        String linkGrouponId;
//        // 这是一个团购发起记录
//        if (v.getGrouponId().equalsIgnoreCase("0")) {
//          linkGrouponId = v.getId();
//          isCreator = creator.getId().equals(userId);
//        } else {
//          linkGrouponId = v.getGrouponId();
//        }
//        sb.clear();
//        int joinerCount = mapper.countByTemplate(sb.from(Groupon.class)
//          .where(sb.field("grouponId").eq(linkGrouponId))
//          .where(sb.field("status").ne(Groupon.Status.NONE.name()))
//          .count()
//          .template());
//        joinerCount += 1;
//        return MyGrouponResp.builder()
//          .groupon(v)
//          .creator(creator.getNickname())
//          .isCreator(isCreator)
//          .joinerCount(joinerCount)
//          .order(order)
//          .rules(rules)
//          .goods(goods)
//          .build();
//      }).collect(Collectors.toList());
//      return responseOf(list);
//    }
//    return responseOf(Collections.emptyList());
//  }
//
//  @Data
//  @Builder
//  private static class MyGrouponResp {
//    @JsonUnwrapped
//    private Groupon groupon;
//    private String creator;
//    private boolean isCreator;
//    private int joinerCount;
//    private Order order;
//    private Groupon.Rules rules;
//    private List<Order.Goods> goods;
//  }
//}
