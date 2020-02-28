package com.github.sun.mall.admin;

import com.github.sun.mall.core.CollectionMapper;
import com.github.sun.mall.core.entity.Collection;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.core.validator.Order;
import org.linlinjava.litemall.core.validator.Sort;
import org.linlinjava.litemall.db.domain.LitemallCollect;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/collect")
@Validated
public class AdminCollectionResource extends BasicCURDResource<Collection, CollectionMapper> {
  @GetMapping("/list")
  public Object list(String userId, String valueId,
                     @RequestParam(defaultValue = "1") Integer page,
                     @RequestParam(defaultValue = "10") Integer limit,
                     @Sort @RequestParam(defaultValue = "add_time") String sort,
                     @Order @RequestParam(defaultValue = "desc") String order) {
    List<LitemallCollect> collectList = collectService.querySelective(userId, valueId, page, limit, sort, order);
    return ResponseUtil.okList(collectList);
  }
}
