package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Issue;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IssueMapper extends CompositeMapper<Issue> {
}
