package com.github.sun.word.loader;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.foundation.mybatis.interceptor.anno.Flatten;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface WordLoaderAffixMapper extends CompositeMapper<WordLoaderAffix> {
    @Select("select * from word_loader_affix where root=#{root}")
    List<WordLoaderAffix> byRoot(@Param("root") String root);

    @Select("select * from word_loader_affix where rootDesc IN(${desc})")
    List<WordLoaderAffix> byRootDesc(@Flatten @Param("desc") Set<String> desc);

    @Select("select * from word_loader_affix where lower(id)=lower(#{word})")
    WordLoaderAffix byId(@Param("word") String word);
}