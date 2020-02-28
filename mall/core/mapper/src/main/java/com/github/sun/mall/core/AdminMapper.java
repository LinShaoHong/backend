package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminMapper extends CompositeMapper<Admin> {
  @Select("SELECT * FROM mall_admin WHERE username = #{username}")
  List<Admin> findByName(@Param("username") String username);
}
