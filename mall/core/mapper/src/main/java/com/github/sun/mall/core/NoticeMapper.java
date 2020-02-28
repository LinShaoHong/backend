package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Notice;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoticeMapper extends CompositeMapper<Notice> {
    @Mapper
    interface Admin extends CompositeMapper<Notice.Admin> {
    }
}
