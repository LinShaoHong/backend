package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.UnAuthorizedException;
import com.github.sun.mall.admin.api.AdminSessionService;
import com.github.sun.mall.admin.entity.Admin;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
public class AdminSessionServiceImpl implements AdminSessionService {
  public static final String TOKEN_NAME = "MALL-ADMIN-TOKEN";
  public static final int TOKEN_EXPIRED = 7 * 60 * 60 * 24;

  @Resource
  private AdminMapper mapper;

  @Override
  public String login(String username, String password, String ip) {
    String hashPassword = Admin.hashPassword(password);
    Admin admin = mapper.findByUsernameAndPassword(username, hashPassword);
    if (admin == null) {
      throw new UnAuthorizedException("请先登录");
    }
    if (ip != null) {
      admin.setLastLoginIp(ip);
    }
    admin.setLastLoginTime(new Date());
    mapper.update(admin);
    return admin.getId();
  }
}
