package com.github.sun.qm;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ViewStatMapper extends CompositeMapper<ViewStat> {
  @Insert("INSERT INTO `qm_view_stat`(`id`, `type`, `date`, `visits`) " +
    "VALUES(#{id}, #{type}, #{date}, #{visits}) " +
    "ON DUPLICATE KEY UPDATE `visits` = `visits` + 1")
  void insertOrUpdate(ViewStat v);
}
