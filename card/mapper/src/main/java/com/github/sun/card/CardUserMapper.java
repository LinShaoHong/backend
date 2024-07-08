package com.github.sun.card;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.cursor.Cursor;

import java.util.List;

@Mapper
public interface CardUserMapper extends CompositeMapper<CardUser> {
  @Select("select * from card_user where openId=#{openId}")
  CardUser byOpenId(@Param("openId") String openId);

  @Select("select * from card_user where shareCode=#{shareCode} order by createTime desc")
  List<CardUser> byShareCode(@Param("shareCode") String shareCode);

  @Update("update card_user set playCount=playCount+1 where id=#{id}")
  void inc(@Param("id") String id);

  @Update("update card_user set loverPlayCount=loverPlayCount+1 where id=#{id}")
  void incLover(@Param("id") String id);

  @Update("update card_user set nickname=#{nickname} where id=#{id}")
  void updateNickname(@Param("id") String id, @Param("nickname") String nickname);

  @Update("update card_user set avatar=#{avatar} where id=#{id}")
  void updateAvatar(@Param("id") String id, @Param("avatar") int avatar);

  @Options(fetchSize = 100)
  @Select("select * from card_user order by id")
  Cursor<CardUser> all();
}