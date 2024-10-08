package com.github.sun.word.loader;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.word.loader.WordLoaderError;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WordLoaderErrorMapper extends CompositeMapper<WordLoaderError> {
}