package com.github.sun.mall.core;

import com.github.sun.foundation.boot.exception.BadRequestException;
import com.github.sun.foundation.mybatis.BasicService;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.mall.core.entity.Cart;
import com.github.sun.mall.core.entity.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService extends BasicService<String, Cart, CartMapper> {
  private final GoodsMapper goodsMapper;
  private final GoodsMapper.Product goodsProductMapper;

  @Autowired
  public CartService(GoodsMapper goodsMapper, GoodsMapper.Product goodsProductMapper) {
    this.goodsMapper = goodsMapper;
    this.goodsProductMapper = goodsProductMapper;
  }

  @Transactional
  public String add(String userId, Cart cart, boolean quickBuy) {
    String productId = cart.getProductId();
    int number = cart.getNumber();
    String goodsId = cart.getGoodsId();
    //判断商品是否可以购买
    Goods goods = goodsMapper.findById(goodsId);
    if (goods == null || !goods.isOnSale()) {
      throw new BadRequestException("商品已下架");
    }
    //判断购物车中是否存在此规格商品
    Cart exist = mapper.findByUserIdAndGoodsIdAndProductId(userId, goodsId, productId);
    if (exist == null) {
      Goods.Product product = goodsProductMapper.findById(productId);
      //取得规格的信息,判断规格库存
      if (product == null || number > product.getNumber()) {
        throw new BadRequestException("库存不足");
      }
      cart.setId(IdGenerator.next());
      cart.setGoodsSn(goods.getSn());
      cart.setGoodsName((goods.getName()));
      if (product.getUrl() == null) {
        cart.setPicUrl(goods.getPicUrl());
      } else {
        cart.setPicUrl(product.getUrl());
      }
      cart.setPrice(product.getPrice());
      cart.setSpecifications(product.getSpecifications());
      cart.setUserId(userId);
      cart.setChecked(true);
      mapper.insert(cart);
    } else {
      int num = quickBuy ? number : exist.getNumber() + number;
      Goods.Product product = goodsProductMapper.findById(productId);
      //取得规格的信息,判断规格库存
      if (product == null || num > product.getNumber()) {
        throw new BadRequestException("库存不足");
      }
      exist.setNumber(num);
      mapper.update(cart);
    }
    return cart.getId();
  }
}
