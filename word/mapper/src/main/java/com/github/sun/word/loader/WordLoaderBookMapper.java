package com.github.sun.word.loader;

import com.github.sun.foundation.mybatis.CompositeMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WordLoaderBookMapper extends CompositeMapper<WordLoaderBook> {
    @Select("select * from word_loader_book where id not in (select id from word_dict_freq) and id not like '%-%' and id not like '% %'")
    List<WordLoaderBook> notInFreq();
}