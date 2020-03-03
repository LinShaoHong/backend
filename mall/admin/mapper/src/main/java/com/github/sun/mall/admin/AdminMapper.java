package com.github.sun.mall.admin;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.admin.entity.Admin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AdminMapper extends CompositeMapper<Admin> {
  @Select("SELECT * FROM mall_admin_admin WHERE username = #{username} AND password = #{password}")
  Admin findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);

  @Select("SELECT COUNT(0) FROM mall_admin_admin WHERE username = #{username}")
  int countByUsername(@Param("username") String username);

  @Select("SELECT COUNT(0) FROM mall_admin_admin WHERE JSON_CONTAINS(roleIds -> '$[*]', JSON_ARRAY(#{roleId}))")
  int countByRoleIdsContains(@Param("roleId") String roleId);
}
