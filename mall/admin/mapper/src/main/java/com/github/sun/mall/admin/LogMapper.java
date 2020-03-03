package com.github.sun.mall.admin;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.admin.entity.Log;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LogMapper extends CompositeMapper<Log> {
}
