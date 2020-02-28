//package com.github.sun.mall.admin;
//
//import com.github.sun.foundation.rest.AbstractResource;
//import io.swagger.annotations.Api;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.shiro.SecurityUtils;
//import org.apache.shiro.authz.annotation.RequiresPermissions;
//import org.apache.shiro.subject.Subject;
//import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
//import org.linlinjava.litemall.admin.service.LogHelper;
//import org.linlinjava.litemall.core.util.RegexUtil;
//import org.linlinjava.litemall.core.util.ResponseUtil;
//import org.linlinjava.litemall.core.util.bcrypt.BCryptPasswordEncoder;
//import org.linlinjava.litemall.core.validator.Order;
//import org.linlinjava.litemall.core.validator.Sort;
//import org.linlinjava.litemall.db.domain.LitemallAdmin;
//import org.linlinjava.litemall.db.service.LitemallAdminService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.util.StringUtils;
//import org.springframework.validation.annotation.Validated;
//
//import javax.validation.constraints.NotNull;
//import javax.ws.rs.Consumes;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.core.MediaType;
//import java.util.List;
//
//import static org.linlinjava.litemall.admin.util.AdminResponseCode.*;
//
//@Path("/v1/mall/admin/admin")
//@Consumes(MediaType.APPLICATION_JSON)
//@Produces(MediaType.APPLICATION_JSON)
//@Api(value = "mall-admin: 系统管理")
//public class AdminAdminResource extends AbstractResource {
//  @GetMapping("/list")
//  public Object list(String username,
//                     @RequestParam(defaultValue = "1") Integer page,
//                     @RequestParam(defaultValue = "10") Integer limit,
//                     @Sort @RequestParam(defaultValue = "add_time") String sort,
//                     @Order @RequestParam(defaultValue = "desc") String order) {
//    List<LitemallAdmin> adminList = adminService.querySelective(username, page, limit, sort, order);
//    return ResponseUtil.okList(adminList);
//  }
//
//  private Object validate(LitemallAdmin admin) {
//    String name = admin.getUsername();
//    if (StringUtils.isEmpty(name)) {
//      return ResponseUtil.badArgument();
//    }
//    if (!RegexUtil.isUsername(name)) {
//      return ResponseUtil.fail(ADMIN_INVALID_NAME, "管理员名称不符合规定");
//    }
//    String password = admin.getPassword();
//    if (StringUtils.isEmpty(password) || password.length() < 6) {
//      return ResponseUtil.fail(ADMIN_INVALID_PASSWORD, "管理员密码长度不能小于6");
//    }
//    return null;
//  }
//
//  @PostMapping("/create")
//  public Object create(@RequestBody LitemallAdmin admin) {
//    Object error = validate(admin);
//    if (error != null) {
//      return error;
//    }
//
//    String username = admin.getUsername();
//    List<LitemallAdmin> adminList = adminService.findAdmin(username);
//    if (adminList.size() > 0) {
//      return ResponseUtil.fail(ADMIN_NAME_EXIST, "管理员已经存在");
//    }
//
//    String rawPassword = admin.getPassword();
//    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//    String encodedPassword = encoder.encode(rawPassword);
//    admin.setPassword(encodedPassword);
//    adminService.add(admin);
//    logHelper.logAuthSucceed("添加管理员", username);
//    return ResponseUtil.ok(admin);
//  }
//
//  @GetMapping("/read")
//  public Object read(@NotNull Integer id) {
//    LitemallAdmin admin = adminService.findById(id);
//    return ResponseUtil.ok(admin);
//  }
//
//  @PostMapping("/update")
//  public Object update(@RequestBody LitemallAdmin admin) {
//    Object error = validate(admin);
//    if (error != null) {
//      return error;
//    }
//
//    Integer anotherAdminId = admin.getId();
//    if (anotherAdminId == null) {
//      return ResponseUtil.badArgument();
//    }
//
//    // 不允许管理员通过编辑接口修改密码
//    admin.setPassword(null);
//
//    if (adminService.updateById(admin) == 0) {
//      return ResponseUtil.updatedDataFailed();
//    }
//
//    logHelper.logAuthSucceed("编辑管理员", admin.getUsername());
//    return ResponseUtil.ok(admin);
//  }
//
//  @PostMapping("/delete")
//  public Object delete(@RequestBody LitemallAdmin admin) {
//    Integer anotherAdminId = admin.getId();
//    if (anotherAdminId == null) {
//      return ResponseUtil.badArgument();
//    }
//
//    // 管理员不能删除自身账号
//    Subject currentUser = SecurityUtils.getSubject();
//    LitemallAdmin currentAdmin = (LitemallAdmin) currentUser.getPrincipal();
//    if (currentAdmin.getId().equals(anotherAdminId)) {
//      return ResponseUtil.fail(ADMIN_DELETE_NOT_ALLOWED, "管理员不能删除自己账号");
//    }
//
//    adminService.deleteById(anotherAdminId);
//    logHelper.logAuthSucceed("删除管理员", admin.getUsername());
//    return ResponseUtil.ok();
//  }
//}
