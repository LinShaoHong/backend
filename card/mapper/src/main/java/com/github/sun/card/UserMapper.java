package com.github.sun.card;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends CompositeMapper<CardUser> {
  @Select("select * from card_user where openId=#{openId}")
  CardUser byOpenId(@Param("openId") String openId);

  @Update("update card_user set playCount=playCount+1 where id=#{id}")
  void inc(@Param("id") String id);

  @Update("update card_user set nickname=#{nickname} where id=#{id}")
  void updateNickname(@Param("id") String id, @Param("nickname") String nickname);
}
