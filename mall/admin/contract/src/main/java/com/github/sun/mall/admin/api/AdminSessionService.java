package com.github.sun.mall.admin.api;

public interface AdminSessionService {
  String login(String username, String password, String ip);
}
