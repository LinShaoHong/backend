package com.github.sun.mall.admin;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.foundation.mybatis.interceptor.anno.Flatten;
import com.github.sun.mall.admin.entity.System;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface SystemMapper extends CompositeMapper<System> {
  @Select("SELECT * FROM mall_admin_system WHERE keyName LIKE CONCAT(#{name}, '%')")
  List<System> findByKeyNameStartsWith(@Param("name") String name);

  @Select("SELECT * FROM mall_admin_system WHERE keyName IN(${keyNames}) FOR UPDATE")
  List<System> findByKeyNameIn(@Flatten @Param("keyNames") Set<String> keyNames);
}
