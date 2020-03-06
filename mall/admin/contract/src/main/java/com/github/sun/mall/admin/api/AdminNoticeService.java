package com.github.sun.mall.admin.api;

import com.github.sun.mall.admin.entity.Notice;

import java.util.Set;

public interface AdminNoticeService {

  void create(Notice notice);

  void update(Notice exist, Notice notice);

  void delete(String id);

  void batchDelete(Set<String> ids);
}
