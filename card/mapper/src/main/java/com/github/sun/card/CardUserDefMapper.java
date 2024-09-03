package com.github.sun.card;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CardUserDefMapper extends CompositeMapper<CardUserDef> {
    @Select("select * from card_user_def where userId=#{userId}")
    CardUserDef byUserId(@Param("userId") String userId);
}