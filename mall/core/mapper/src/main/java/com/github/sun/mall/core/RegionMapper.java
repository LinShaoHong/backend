package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Region;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RegionMapper extends CompositeMapper<Region> {
}
