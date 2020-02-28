package com.github.sun.mall.core;

import com.github.sun.foundation.mybatis.CompositeMapper;
import com.github.sun.mall.core.entity.Goods;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GoodsMapper extends CompositeMapper<Goods> {
    @Mapper
    interface Attribute extends CompositeMapper<Goods.Attribute> {
    }

    @Mapper
    interface Product extends CompositeMapper<Goods.Product> {
    }

    @Mapper
    interface Specification extends CompositeMapper<Goods.Specification> {
    }
}
