package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Groupon;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GrouponMapper extends CompositeMapper<Groupon> {
    @Mapper
    interface Rules extends CompositeMapper<Groupon.Rules> {
    }
}
