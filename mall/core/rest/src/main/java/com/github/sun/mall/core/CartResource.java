package com.github.sun.mall.core;

import com.github.sun.foundation.boot.exception.BadRequestException;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.core.entity.*;
import com.github.sun.mall.core.resolver.LoginUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/v1/mall/cart")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall: Cart Resource", tags = "用户购物车服务")
public class CartResource extends AbstractResource {
  private final CartMapper mapper;
  private final CartService service;
  private final GoodsMapper goodsMapper;
  private final CouponService couponService;
  private final AddressMapper addressMapper;
  private final CouponMapper.User couponUserMapper;
  private final GrouponMapper.Rules grouponRulesMapper;
  private final GoodsMapper.Product goodsProductMapper;

  @Inject
  public CartResource(CartMapper mapper,
                      CartService service,
                      GoodsMapper goodsMapper,
                      CouponService couponService,
                      AddressMapper addressMapper,
                      CouponMapper.User couponUserMapper,
                      GrouponMapper.Rules grouponRulesMapper,
                      GoodsMapper.Product goodsProductMapper) {
    this.mapper = mapper;
    this.service = service;
    this.goodsMapper = goodsMapper;
    this.couponService = couponService;
    this.addressMapper = addressMapper;
    this.couponUserMapper = couponUserMapper;
    this.grouponRulesMapper = grouponRulesMapper;
    this.goodsProductMapper = goodsProductMapper;
  }

  @GET
  @ApiOperation("获取用户购物车信息")
  public SingleResponse<CartResp> getAll(@Context LoginUser user) {
    List<Cart> list = mapper.findByUserId(user.getId());
    if (!list.isEmpty()) {
      Set<String> goodsIds = list.stream().map(Cart::getGoodsId).collect(Collectors.toSet());
      Set<String> onSaleGoodsIds = goodsMapper.findByIds(goodsIds)
        .stream().filter(Goods::isOnSale).map(Goods::getId).collect(Collectors.toSet());
      final List<Cart> deletes = list.stream().filter(v -> !onSaleGoodsIds.contains(v.getGoodsId())).collect(Collectors.toList());
      mapper.deleteAll(deletes);
      list.removeAll(deletes);

      int goodsTotal = 0;
      BigDecimal goodsAmount = new BigDecimal(0.00);
      int checkedGoodsTotal = 0;
      BigDecimal checkedGoodsAmount = new BigDecimal(0.00);
      for (Cart cart : list) {
        goodsTotal += cart.getNumber();
        goodsAmount = goodsAmount.add(new BigDecimal(cart.getPrice()).multiply(new BigDecimal(cart.getNumber())));
        if (cart.isChecked()) {
          checkedGoodsTotal += cart.getNumber();
          checkedGoodsAmount = checkedGoodsAmount.add(new BigDecimal(cart.getPrice()).multiply(new BigDecimal(cart.getNumber())));
        }
      }
      return responseOf(CartResp.builder()
        .carts(list)
        .goodsTotal(goodsTotal)
        .goodsAmount(goodsAmount)
        .checkedGoodsTotal(checkedGoodsTotal)
        .checkedGoodsAmount(checkedGoodsAmount)
        .build());
    }
    return responseOf(CartResp.builder().carts(list).build());
  }

  @Data
  @Builder
  private static class CartResp {
    private List<Cart> carts;
    private int goodsTotal;
    private BigDecimal goodsAmount;
    private int checkedGoodsTotal;
    private BigDecimal checkedGoodsAmount;
  }

  /**
   * 如果已经存在购物车货品，则增加数量；
   * 否则添加新的购物车货品项。
   */
  @POST
  @ApiOperation("加入商品到购物车")
  public Response add(@Valid @NotNull(message = "缺少实体") Cart cart, @Context LoginUser user) {
    service.add(user.getId(), cart, false);
    return responseOf();
  }

  /**
   * 和add方法的区别在于：
   * 1. 如果购物车内已经存在购物车货品，前者的逻辑是数量添加，这里的逻辑是数量覆盖
   * 2. 添加成功以后，前者的逻辑是返回当前购物车商品数量，这里的逻辑是返回对应购物车项的ID
   */
  @POST
  @Path("/quickBuy")
  @ApiOperation("立即购买")
  public SingleResponse<String> quickBuy(@Valid @NotNull(message = "缺少实体") Cart cart, @Context LoginUser user) {
    return responseOf(service.add(user.getId(), cart, true));
  }

  @PUT
  @Path("/${id}/num")
  @ApiOperation("修改购物车商品货品数量")
  public Response update(@PathParam("id") String id,
                         @Min(value = 1, message = "最少1个") @QueryParam("num") int num,
                         @Context LoginUser user) {
    // 判断是否存在该订单
    // 如果不存在，直接返回错误
    Cart exist = mapper.findById(id);
    if (exist == null) {
      throw new NotFoundException("Can not find cart by id=" + id);
    }
    //判断商品是否可以购买
    Goods goods = goodsMapper.findById(exist.getGoodsId());
    if (goods == null || !goods.isOnSale()) {
      throw new BadRequestException("商品已下架");
    }
    //取得规格的信息,判断规格库存
    Goods.Product product = goodsProductMapper.findById(exist.getProductId());
    if (product == null || product.getNumber() < num) {
      throw new BadRequestException("库存不足");
    }
    exist.setNumber(num);
    mapper.update(exist);
    return responseOf();
  }

//  /**
//   * 购物车商品货品勾选状态
//   * <p>
//   * 如果原来没有勾选，则设置勾选状态；如果商品已经勾选，则设置非勾选状态。
//   *
//   * @param userId 用户ID
//   * @param body   购物车商品信息， { productIds: xxx, isChecked: 1/0 }
//   * @return 购物车信息
//   */
//  @PostMapping("checked")
//  public Object checked(@LoginUser Integer userId, @RequestBody String body) {
//    if (userId == null) {
//      return ResponseUtil.unlogin();
//    }
//    if (body == null) {
//      return ResponseUtil.badArgument();
//    }
//
//    List<Integer> productIds = JacksonUtil.parseIntegerList(body, "productIds");
//    if (productIds == null) {
//      return ResponseUtil.badArgument();
//    }
//
//    Integer checkValue = JacksonUtil.parseInteger(body, "isChecked");
//    if (checkValue == null) {
//      return ResponseUtil.badArgument();
//    }
//    Boolean isChecked = (checkValue == 1);
//
//    service.updateCheck(userId, productIds, isChecked);
//    return index(userId);
//  }

  @DELETE
  @ApiOperation("删除购物车商品")
  public Response delete(@NotNull(message = "请选择要删除的商品") @QueryParam("productIds") Set<String> productIds,
                         @Context LoginUser user) {
    mapper.deleteByUserIdAndProductIdIn(user.getId(), productIds);
    return responseOf();
  }

  /**
   * 如果用户没有登录，则返回空数据。
   */
  @GET
  @Path("/count")
  @ApiOperation("购物车商品货品数量")
  public SingleResponse<Integer> count(@Context LoginUser user) {
    int count = 0;
    List<Cart> cartList = mapper.findByUserId(user.getId());
    for (Cart cart : cartList) {
      count += cart.getNumber();
    }
    return responseOf(count);
  }

  /**
   * 购物车下单
   */
  @GET
  @Path("/checkout")
  @ApiOperation("购物车下单")
  public SingleResponse<CartCheckOutResp> checkout(@QueryParam("cartId") String cartId,
                                                   @QueryParam("addressId") String addressId,
                                                   @QueryParam("couponId") String couponId,
                                                   @QueryParam("userCouponId") String userCouponId,
                                                   @QueryParam("grouponRulesId") String grouponRulesId,
                                                   @Context LoginUser user) {
    String userId = user.getId();
    // 收货地址
    Address address;
    if (addressId == null) {
      address = addressMapper.findDefault(userId);
      if (address == null) {
        throw new NotFoundException("请先添加收货地址");
      }
    } else {
      address = addressMapper.findByIdAndUserId(addressId, userId);
      if (address == null) {
        throw new NotFoundException("请填写收货地址");
      }
    }

    // 团购优惠
    BigDecimal grouponPrice = new BigDecimal(0.00);
    Groupon.Rules grouponRules = grouponRulesMapper.findById(grouponRulesId);
    if (grouponRules != null) {
      grouponPrice = new BigDecimal(grouponRules.getDiscount());
    }

    // 商品价格
    List<Cart> checkedGoodsList;
    if (cartId == null) {
      checkedGoodsList = mapper.findByUserIdAndChecked(userId);
    } else {
      Cart cart = service.findById(cartId);
      if (cart == null) {
        throw new NotFoundException("Can not find cart by id=" + cartId);
      }
      checkedGoodsList = new ArrayList<>(1);
      checkedGoodsList.add(cart);
    }
    BigDecimal checkedGoodsPrice = new BigDecimal(0.00);
    for (Cart cart : checkedGoodsList) {
      //  只有当团购规格商品ID符合才进行团购优惠
      if (grouponRules != null && grouponRules.getGoodsId().equals(cart.getGoodsId())) {
        checkedGoodsPrice = checkedGoodsPrice.add(new BigDecimal(cart.getPrice()).subtract(grouponPrice).multiply(new BigDecimal(cart.getNumber())));
      } else {
        checkedGoodsPrice = checkedGoodsPrice.add(new BigDecimal(cart.getPrice()).multiply(new BigDecimal(cart.getNumber())));
      }
    }

    // 计算优惠券可用情况
    BigDecimal tmpCouponPrice = new BigDecimal(0.00);
    String tmpCouponId = "AUTO_USE";
    String tmpUserCouponId = "AUTO_USE";
    int tmpCouponLength = 0;
    List<Coupon.User> couponUserList = couponUserMapper.findByUserId(userId);
    for (Coupon.User couponUser : couponUserList) {
      tmpUserCouponId = couponUser.getId();
      Coupon coupon = couponService.check(userId, couponUser.getCouponId(), tmpUserCouponId, checkedGoodsPrice);
      if (coupon == null) {
        continue;
      }
      tmpCouponLength++;
      if (tmpCouponPrice.compareTo(new BigDecimal(coupon.getDiscount())) < 0) {
        tmpCouponPrice = new BigDecimal(coupon.getDiscount());
        tmpCouponId = coupon.getId();
      }
    }
    // 获取优惠券减免金额，优惠券可用数量
    int availableCouponLength = tmpCouponLength;
    BigDecimal couponPrice = new BigDecimal(0);
    // 这里存在三种情况
    // 1. 用户不想使用优惠券，则不处理
    // 2. 用户想自动使用优惠券，则选择合适优惠券
    // 3. 用户已选择优惠券，则测试优惠券是否合适
    if (couponId == null || "NO_USE".equalsIgnoreCase(couponId)) {
      couponId = "NO_USE";
      userCouponId = "NO_USE";
    } else if ("AUTO_USE".equalsIgnoreCase(couponId)) {
      couponPrice = tmpCouponPrice;
      couponId = tmpCouponId;
      userCouponId = tmpUserCouponId;
    } else {
      Coupon coupon = couponService.check(userId, couponId, userCouponId, checkedGoodsPrice);
      // 用户选择的优惠券有问题，则选择合适优惠券，否则使用用户选择的优惠券
      if (coupon == null) {
        couponPrice = tmpCouponPrice;
        couponId = tmpCouponId;
        userCouponId = tmpUserCouponId;
      } else {
        couponPrice = new BigDecimal(coupon.getDiscount());
      }
    }

    // todo 根据订单商品总价计算运费，满88则免运费，否则8元；
    BigDecimal freightPrice = new BigDecimal(0.00);
//    if (checkedGoodsPrice.compareTo(SystemConfig.getFreightLimit()) < 0) {
//      freightPrice = SystemConfig.getFreight();
//    }

    // 可以使用的其他钱，例如用户积分
    BigDecimal integralPrice = new BigDecimal(0.00);
    // 订单费用
    BigDecimal orderTotalPrice = checkedGoodsPrice.add(freightPrice).subtract(couponPrice).max(new BigDecimal(0.00));
    BigDecimal actualPrice = orderTotalPrice.subtract(integralPrice);
    return responseOf(CartCheckOutResp.builder()
      .addressId(addressId)
      .couponId(couponId)
      .userCouponId(userCouponId)
      .cartId(cartId)
      .grouponRulesId(grouponRulesId)
      .grouponPrice(grouponPrice)
      .checkedAddress(address)
      .availableCouponLength(availableCouponLength)
      .goodsTotalPrice(checkedGoodsPrice)
      .freightPrice(freightPrice)
      .couponPrice(couponPrice)
      .orderTotalPrice(orderTotalPrice)
      .actualPrice(actualPrice)
      .checkedGoodsList(checkedGoodsList)
      .build());
  }

  @Data
  @Builder
  private static class CartCheckOutResp {
    private String addressId;
    private String couponId;
    private String userCouponId;
    private String cartId;
    private String grouponRulesId;
    private BigDecimal grouponPrice;
    private Address checkedAddress;
    private int availableCouponLength;
    private BigDecimal goodsTotalPrice;
    private BigDecimal freightPrice;
    private BigDecimal couponPrice;
    private BigDecimal orderTotalPrice;
    private BigDecimal actualPrice;
    private List<Cart> checkedGoodsList;
  }
}
