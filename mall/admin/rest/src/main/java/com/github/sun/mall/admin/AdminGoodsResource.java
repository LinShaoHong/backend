//package com.github.sun.mall.admin;
//
//import com.github.sun.mall.core.GoodsMapper;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.shiro.authz.annotation.RequiresPermissions;
//import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
//import org.linlinjava.litemall.admin.dto.GoodsAllinone;
//import org.linlinjava.litemall.admin.service.AdminGoodsService;
//import org.linlinjava.litemall.core.validator.Order;
//import org.linlinjava.litemall.core.validator.Sort;
//import org.linlinjava.litemall.db.domain.LitemallGoods;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.validation.annotation.Validated;
//
//import javax.validation.constraints.NotNull;
//
//@RestController
//@RequestMapping("/admin/goods")
//@Validated
//public class AdminGoodsResource extends BasicCURDResource<Goods, GoodsMapper> {
//  private final Log logger = LogFactory.getLog(AdminGoodsResource.class);
//
//  @Autowired
//  private AdminGoodsService adminGoodsService;
//
//  /**
//   * 查询商品
//   *
//   * @param goodsId
//   * @param goodsSn
//   * @param name
//   * @param page
//   * @param limit
//   * @param sort
//   * @param order
//   * @return
//   */
//  @RequiresPermissions("admin:goods:list")
//  @RequiresPermissionsDesc(menu = {"商品管理", "商品管理"}, button = "查询")
//  @GetMapping("/list")
//  public Object list(Integer goodsId, String goodsSn, String name,
//                     @RequestParam(defaultValue = "1") Integer page,
//                     @RequestParam(defaultValue = "10") Integer limit,
//                     @Sort @RequestParam(defaultValue = "add_time") String sort,
//                     @Order @RequestParam(defaultValue = "desc") String order) {
//    return adminGoodsService.list(goodsId, goodsSn, name, page, limit, sort, order);
//  }
//
//  @GetMapping("/catAndBrand")
//  public Object list2() {
//    return adminGoodsService.list2();
//  }
//
//  /**
//   * 编辑商品
//   *
//   * @param goodsAllinone
//   * @return
//   */
//  @RequiresPermissions("admin:goods:update")
//  @RequiresPermissionsDesc(menu = {"商品管理", "商品管理"}, button = "编辑")
//  @PostMapping("/update")
//  public Object update(@RequestBody GoodsAllinone goodsAllinone) {
//    return adminGoodsService.update(goodsAllinone);
//  }
//
//  /**
//   * 删除商品
//   *
//   * @param goods
//   * @return
//   */
//  @RequiresPermissions("admin:goods:delete")
//  @RequiresPermissionsDesc(menu = {"商品管理", "商品管理"}, button = "删除")
//  @PostMapping("/delete")
//  public Object delete(@RequestBody LitemallGoods goods) {
//    return adminGoodsService.delete(goods);
//  }
//
//  /**
//   * 添加商品
//   *
//   * @param goodsAllinone
//   * @return
//   */
//  @RequiresPermissions("admin:goods:create")
//  @RequiresPermissionsDesc(menu = {"商品管理", "商品管理"}, button = "上架")
//  @PostMapping("/create")
//  public Object create(@RequestBody GoodsAllinone goodsAllinone) {
//    return adminGoodsService.create(goodsAllinone);
//  }
//
//  /**
//   * 商品详情
//   *
//   * @param id
//   * @return
//   */
//  @RequiresPermissions("admin:goods:read")
//  @RequiresPermissionsDesc(menu = {"商品管理", "商品管理"}, button = "详情")
//  @GetMapping("/detail")
//  public Object detail(@NotNull Integer id) {
//    return adminGoodsService.detail(id);
//
//  }
//
//}
