package com.github.sun.word.loader;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WordLoaderBookMapper extends CompositeMapper<WordLoaderBook> {
}