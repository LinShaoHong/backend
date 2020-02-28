package com.github.sun.spider.mapper;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.spider.SpiderJob;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SpiderJobMapper extends CompositeMapper<SpiderJob> {
}
