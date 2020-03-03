package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Region;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RegionMapper extends CompositeMapper<Region> {
  @Select("SELECT * FROM mall_region WHERE pId = #{pId}")
  List<Region> findByPId(@Param("pId") String pId);
}
