package com.github.sun.mall.admin;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.admin.entity.Admin;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/v1/mall/admin/profile")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: profile: region")
public class AdminProfileResource extends AbstractResource {
  private final NoticeMapper.Admin noticeAdminMapper;

  @Inject
  public AdminProfileResource(NoticeMapper.Admin noticeAdminMapper) {
    this.noticeAdminMapper = noticeAdminMapper;
  }

  //
//  @RequiresAuthentication
//  @PostMapping("/password")
//  public Object create(@RequestBody String body) {
//    String oldPassword = JacksonUtil.parseString(body, "oldPassword");
//    String newPassword = JacksonUtil.parseString(body, "newPassword");
//    if (StringUtils.isEmpty(oldPassword)) {
//      return ResponseUtil.badArgument();
//    }
//    if (StringUtils.isEmpty(newPassword)) {
//      return ResponseUtil.badArgument();
//    }
//
//    Subject currentUser = SecurityUtils.getSubject();
//    LitemallAdmin admin = (LitemallAdmin) currentUser.getPrincipal();
//
//    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
//    if (!encoder.matches(oldPassword, admin.getPassword())) {
//      return ResponseUtil.fail(ADMIN_INVALID_ACCOUNT, "账号密码不对");
//    }
//
//    String encodedNewPassword = encoder.encode(newPassword);
//    admin.setPassword(encodedNewPassword);
//
//    adminService.updateById(admin);
//    return ResponseUtil.ok();
//  }
//
//  private Integer getAdminId() {
//    Subject currentUser = SecurityUtils.getSubject();
//    LitemallAdmin admin = (LitemallAdmin) currentUser.getPrincipal();
//    return admin.getId();
//  }


  @GET
  @Path("/nNotice")
  public SingleResponse<Integer> nNotice(@Context Admin admin) {
    return responseOf(noticeAdminMapper.countByAdminId(admin.getId()));
  }

//  @RequiresAuthentication
//  @GetMapping("/lsnotice")
//  public Object lsNotice(String title, String type,
//                         @RequestParam(defaultValue = "1") Integer page,
//                         @RequestParam(defaultValue = "10") Integer limit,
//                         @Sort @RequestParam(defaultValue = "add_time") String sort,
//                         @Order @RequestParam(defaultValue = "desc") String order) {
//    List<LitemallNoticeAdmin> noticeList = noticeAdminService.querySelective(title, type, getAdminId(), page, limit, sort, order);
//    return ResponseUtil.okList(noticeList);
//  }
//
//  @RequiresAuthentication
//  @PostMapping("/catnotice")
//  public Object catNotice(@RequestBody String body) {
//    Integer noticeId = JacksonUtil.parseInteger(body, "noticeId");
//    if (noticeId == null) {
//      return ResponseUtil.badArgument();
//    }
//
//    LitemallNoticeAdmin noticeAdmin = noticeAdminService.find(noticeId, getAdminId());
//    if (noticeAdmin == null) {
//      return ResponseUtil.badArgumentValue();
//    }
//    // 更新通知记录中的时间
//    noticeAdmin.setReadTime(LocalDateTime.now());
//    noticeAdminService.update(noticeAdmin);
//
//    // 返回通知的相关信息
//    Map<String, Object> data = new HashMap<>();
//    LitemallNotice notice = noticeService.findById(noticeId);
//    data.put("title", notice.getTitle());
//    data.put("content", notice.getContent());
//    data.put("time", notice.getUpdateTime());
//    Integer adminId = notice.getAdminId();
//    if (adminId.equals(0)) {
//      data.put("admin", "系统");
//    } else {
//      LitemallAdmin admin = adminService.findById(notice.getAdminId());
//      data.put("admin", admin.getUsername());
//      data.put("avatar", admin.getAvatar());
//    }
//    return ResponseUtil.ok(data);
//  }
//
//  @RequiresAuthentication
//  @PostMapping("/bcatnotice")
//  public Object bcatNotice(@RequestBody String body) {
//    List<Integer> ids = JacksonUtil.parseIntegerList(body, "ids");
//    noticeAdminService.markReadByIds(ids, getAdminId());
//    return ResponseUtil.ok();
//  }
//
//  @RequiresAuthentication
//  @PostMapping("/rmnotice")
//  public Object rmNotice(@RequestBody String body) {
//    Integer id = JacksonUtil.parseInteger(body, "id");
//    if (id == null) {
//      return ResponseUtil.badArgument();
//    }
//    noticeAdminService.deleteById(id, getAdminId());
//    return ResponseUtil.ok();
//  }
//
//  @RequiresAuthentication
//  @PostMapping("/brmnotice")
//  public Object brmNotice(@RequestBody String body) {
//    List<Integer> ids = JacksonUtil.parseIntegerList(body, "ids");
//    noticeAdminService.deleteByIds(ids, getAdminId());
//    return ResponseUtil.ok();
//  }

}
