package com.github.sun.qm;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends CompositeMapper<User> {
  @Select("SELECT * FROM qm_user WHERE email = #{email}")
  User findByEmail(@Param("email") String email);

  @Select("SELECT * FROM qm_user WHERE username = #{username} AND password = #{password}")
  User findByUsernameAndPassword(@Param("username") String username, @Param("password") String password);

  @Select("SELECT COUNT(0) FROM qm_user WHERE username = #{username}")
  int countByUsername(@Param("username") String username);
}