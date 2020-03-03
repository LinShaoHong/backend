//package com.github.sun.mall.admin;
//
//import com.github.sun.foundation.rest.AbstractResource;
//import io.swagger.annotations.Api;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.shiro.authz.annotation.*;
//import org.linlinjava.litemall.admin.annotation.RequiresPermissionsDesc;
//import org.linlinjava.litemall.core.util.ResponseUtil;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import javax.ws.rs.Consumes;
//import javax.ws.rs.Path;
//import javax.ws.rs.Produces;
//import javax.ws.rs.core.MediaType;
//
//@Path("/v1/mall/admin/notice")
//@Consumes(MediaType.APPLICATION_JSON)
//@Produces(MediaType.APPLICATION_JSON)
//@Api(value = "mall-admin: 通知管理: notice")
//public class AdminIndexResource extends AbstractResource {
//  private final Log logger = LogFactory.getLog(AdminIndexResource.class);
//
//  @RequestMapping("/index")
//  public Object index() {
//    return ResponseUtil.ok("hello world, this is admin service");
//  }
//
//  @RequiresGuest
//  @RequestMapping("/guest")
//  public Object guest() {
//    return ResponseUtil.ok("hello world, this is admin service");
//  }
//
//  @RequiresAuthentication
//  @RequestMapping("/authn")
//  public Object authn() {
//    return ResponseUtil.ok("hello world, this is admin service");
//  }
//
//  @RequiresUser
//  @RequestMapping("/user")
//  public Object user() {
//    return ResponseUtil.ok("hello world, this is admin service");
//  }
//
//  @RequiresRoles("admin")
//  @RequestMapping("/admin")
//  public Object admin() {
//    return ResponseUtil.ok("hello world, this is admin service");
//  }
//
//  @RequiresRoles("admin2")
//  @RequestMapping("/admin2")
//  public Object admin2() {
//    return ResponseUtil.ok("hello world, this is admin service");
//  }
//
//  @RequiresPermissions("index:permission:read")
//  @RequiresPermissionsDesc(menu = {"其他", "权限测试"}, button = "权限读")
//  @GetMapping("/read")
//  public Object read() {
//    return ResponseUtil.ok("hello world, this is admin service");
//  }
//
//  @RequiresPermissions("index:permission:write")
//  @RequiresPermissionsDesc(menu = {"其他", "权限测试"}, button = "权限写")
//  @PostMapping("/write")
//  public Object write() {
//    return ResponseUtil.ok("hello world, this is admin service");
//  }
//
//}
