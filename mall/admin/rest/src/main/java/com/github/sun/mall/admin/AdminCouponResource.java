//package com.github.sun.mall.admin;
//
//import com.github.sun.foundation.sql.SqlBuilder;
//import com.github.sun.mall.core.CouponMapper;
//import com.github.sun.mall.core.entity.Coupon;
//import io.swagger.annotations.Api;
//import io.swagger.annotations.ApiOperation;
//import org.apache.shiro.authz.annotation.RequiresPermissions;
//import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
//import org.linlinjava.litemall.core.util.ResponseUtil;
//import org.linlinjava.litemall.core.validator.Order;
//import org.linlinjava.litemall.core.validator.Sort;
//import org.linlinjava.litemall.db.domain.LitemallCoupon;
//import org.linlinjava.litemall.db.domain.LitemallCouponUser;
//import org.springframework.util.StringUtils;
//
//import javax.inject.Named;
//import javax.ws.rs.*;
//import javax.ws.rs.core.MediaType;
//import java.util.List;
//
//@Path("/v1/mall/admin/category")
//@Consumes(MediaType.APPLICATION_JSON)
//@Produces(MediaType.APPLICATION_JSON)
//@Api(value = "mall-admin: 优惠券管理")
//public class AdminCouponResource extends BasicCURDResource<Coupon, CouponMapper> {
//  private final SqlBuilder.Factory factory;
//
//  public AdminCouponResource(@Named("mysql") SqlBuilder.Factory factory) {
//    this.factory = factory;
//  }
//
//  @GET
//  @ApiOperation("分页获取优惠券")
//  public Object list(@QueryParam("name") String name,
//                     Short type,
//                     Short status,
//                     @QueryParam("start") int start,
//                     @QueryParam("count") int count,
//                     @QueryParam("sort") @DefaultValue("createTime") String sort,
//                     @QueryParam("asc") boolean asc) {
//    List<LitemallCoupon> couponList = couponService.querySelective(name, type, status, page, limit, sort, order);
//    return ResponseUtil.okList(couponList);
//  }
//
//  @RequiresPermissions("admin:coupon:listuser")
//  @RequiresPermissionsDesc(menu = {"推广管理", "优惠券管理"}, button = "查询用户")
//  @GetMapping("/listuser")
//  public Object listuser(Integer userId, Integer couponId, Short status,
//                         @RequestParam(defaultValue = "1") Integer page,
//                         @RequestParam(defaultValue = "10") Integer limit,
//                         @Sort @RequestParam(defaultValue = "add_time") String sort,
//                         @Order @RequestParam(defaultValue = "desc") String order) {
//    List<LitemallCouponUser> couponList = couponUserService.queryList(userId, couponId, status, page,
//      limit, sort, order);
//    return ResponseUtil.okList(couponList);
//  }
//
//  private Object validate(LitemallCoupon coupon) {
//    String name = coupon.getName();
//    if (StringUtils.isEmpty(name)) {
//      return ResponseUtil.badArgument();
//    }
//    return null;
//  }
//
//  @Override
//  protected String name() {
//    return "优惠券";
//  }
//}
