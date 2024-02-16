package com.github.sun.card;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CardRoomMapper extends CompositeMapper<CardRoom> {
  @Select("select * from card_room where mainUserId=#{mainUserId} and userId=#{userId}")
  CardRoom byMainUserIdAndUserId(@Param("mainUserId") String mainUserId, @Param("userId") String userId);

  @Select("select * from card_room where userId=#{userId} order by enterTime desc")
  List<CardRoom> joined(@Param("userId") String userId);

  @Select("select * from card_room where mainUserId=#{mainUserId} order by enterTime desc")
  List<CardRoom> byMainUserId(@Param("mainUserId") String mainUserId);
}