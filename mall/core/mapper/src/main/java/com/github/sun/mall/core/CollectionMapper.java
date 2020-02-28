package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Collection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CollectionMapper extends CompositeMapper<Collection> {
  @Select("SELECT * FROM mall_collection WHERE userId = #{userId} AND type = #{type} AND valueId = #{valueId}")
  Collection findByUserIdAndTypeAndValueId(@Param("userId") String userId,
                                           @Param("type") int type,
                                           @Param("valueId") String valueId);
}
