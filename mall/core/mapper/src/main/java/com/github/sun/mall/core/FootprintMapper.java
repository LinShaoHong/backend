package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Footprint;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FootprintMapper extends CompositeMapper<Footprint> {
  @Select("SELECT * FROM mall_footprint WHERE userId = #{userId} ORDER BY createTime DESC")
  List<Footprint> findPaged(@Param("userId") String userId, @Param("start") int start, @Param("count") int count);

  @Select("SELECT * FROM mall_footprint WHERE userId = #{userId} AND id = #{id}")
  Footprint findByUserIdAndId(@Param("userId") String userId, @Param("id") String id);
}
