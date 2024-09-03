package com.github.sun.card;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CardRoomMapper extends CompositeMapper<CardRoom> {
    @Select("select * from card_room where mainUserId=#{mainUserId} and userId=#{userId} and hks=#{hks}")
    CardRoom byMainUserIdAndUserId(@Param("mainUserId") String mainUserId,
                                   @Param("userId") String userId,
                                   @Param("hks") boolean hks);

    @Select("select * from card_room where userId=#{userId} and mainUserId<>#{userId} and hks=#{hks} order by enterTime desc")
    List<CardRoom> joined(@Param("userId") String userId, @Param("hks") boolean hks);

    @Delete("delete from card_room where id=#{id}")
    int remove(@Param("id") String id);
}