package com.github.sun.mall.admin;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.admin.entity.Permission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PermissionMapper extends CompositeMapper<Permission> {
  @Select("SELECT * FROM mall_admin_permission WHERE roleId = #{roleId}")
  List<Permission> findByRoleId(@Param("roleId") String roleId);

  @Select("SELECT * FROM mall_admin_permission WHERE roleId = #{roleId} AND permission = #{permission}")
  List<Permission> findByRoleIdAndPermission(@Param("roleId") String roleId, @Param("permission") String permission);

  @Delete("DELETE FROM mall_admin_permission WHERE roleId = #{roleId}")
  void deleteByRoleId(@Param("roleId") String roleId);
}
