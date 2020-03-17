package com.github.sun.xfg;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CollectionMapper extends CompositeMapper<Collection> {
  @Select("SELECT COUNT(0) FROM xfg_collection WHERE userId = #{userId} AND girlId = #{girlId}")
  int countByUserIdAndGirlId(@Param("userId") String userId, @Param("girlId") String girlId);

  @Delete("DELETE FROM xfg_collection WHERE userId = #{userId} AND girlId = #{girlId}")
  void deleteByUserIdAndGirlId(@Param("userId") String userId, @Param("girlId") String girlId);
}
