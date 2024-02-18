package com.github.sun.card;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CardShareMapper extends CompositeMapper<CardShare> {
}