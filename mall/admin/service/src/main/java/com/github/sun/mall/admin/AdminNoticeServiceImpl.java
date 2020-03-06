package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.AccessDeniedException;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.mall.admin.api.AdminNoticeService;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.admin.entity.Notice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminNoticeServiceImpl implements AdminNoticeService {
  @Resource
  private NoticeMapper mapper;
  @Resource
  private AdminMapper adminMapper;
  @Resource
  private NoticeMapper.Admin noticeAdminMapper;

  @Override
  @Transactional
  public void create(Notice notice) {
    String id = IdGenerator.next();
    notice.setId(id);
    mapper.insert(notice);
    // 2. 添加管理员通知记录
    List<Admin> admins = adminMapper.findAll();
    List<Notice.Admin> list = admins.stream().map(admin -> Notice.Admin.builder()
      .id(IdGenerator.next())
      .noticeId(id)
      .noticeTitle(notice.getTitle())
      .build()).collect(Collectors.toList());
    noticeAdminMapper.insertAll(list);
  }

  @Override
  @Transactional
  public void update(Notice exist, Notice notice) {
    // 如果通知已经有人阅读过，则不支持编辑
    if (noticeAdminMapper.countByNoticeIdAndReadTimeNotNull(notice.getId()) > 0) {
      throw new AccessDeniedException("通知已被阅读，不能重新编辑");
    }
    // 1. 更新通知记录
    mapper.update(notice);
    // 2. 更新管理员通知记录
    if (!exist.getTitle().equals(notice.getTitle())) {
      noticeAdminMapper.updateNoticeTitleByNoticeId(notice.getId(), notice.getTitle());
    }
  }

  @Override
  @Transactional
  public void delete(String id) {
    mapper.deleteById(id);
    noticeAdminMapper.deleteByNoticeId(id);
  }

  @Override
  @Transactional
  public void batchDelete(Set<String> ids) {
    mapper.deleteByIds(new ArrayList<>(ids));
    noticeAdminMapper.deleteByNoticeIdIn(ids);
  }
}
