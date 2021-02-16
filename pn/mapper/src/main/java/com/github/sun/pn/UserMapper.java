package com.github.sun.pn;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends CompositeMapper<User> {
}
