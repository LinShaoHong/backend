package com.github.sun.mall.admin;

import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.mall.core.GoodsMapper;
import com.github.sun.mall.core.entity.Goods;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class AdminGoodsService {
  @Resource
  private GoodsMapper mapper;
  @Resource
  private GoodsMapper.Attribute attributeMapper;
  @Resource
  private GoodsMapper.Product productMapper;
  @Resource
  private GoodsMapper.Specification specificationMapper;

  @Transactional
  public void delete(String id) {
    Goods goods = mapper.findById(id);
    if (goods == null) {
      throw new NotFoundException("Can not find goods by id=" + id);
    }
    mapper.deleteById(id);
    attributeMapper.deleteByGoodsId(id);
    productMapper.deleteByGoodsId(id);
    specificationMapper.deleteByGoodsId(id);
  }
}
