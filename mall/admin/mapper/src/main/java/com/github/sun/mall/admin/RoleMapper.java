package com.github.sun.mall.admin;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.admin.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RoleMapper extends CompositeMapper<Role> {
  @Select("SELECT * FROM mall_admin_role WHERE name = #{name}")
  Role findByName(@Param("name") String name);
}
