package com.github.sun.scheduler;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SchedulerJobMapper extends CompositeMapper<SchedulerJob> {
}
