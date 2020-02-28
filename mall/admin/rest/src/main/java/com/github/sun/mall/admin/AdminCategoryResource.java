package com.github.sun.mall.admin;

import com.github.sun.mall.core.CategoryMapper;
import com.github.sun.mall.core.entity.Category;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.linlinjava.litemall.admin.vo.CategoryVo;
import org.linlinjava.litemall.core.util.ResponseUtil;
import org.linlinjava.litemall.db.domain.LitemallCategory;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/category")
@Validated
public class AdminCategoryResource extends BasicCURDResource<Category, CategoryMapper> {
  @GetMapping("/list")
  public Object list() {
    List<CategoryVo> categoryVoList = new ArrayList<>();

    List<LitemallCategory> categoryList = categoryService.queryByPid(0);
    for (LitemallCategory category : categoryList) {
      CategoryVo categoryVO = new CategoryVo();
      categoryVO.setId(category.getId());
      categoryVO.setDesc(category.getDesc());
      categoryVO.setIconUrl(category.getIconUrl());
      categoryVO.setPicUrl(category.getPicUrl());
      categoryVO.setKeywords(category.getKeywords());
      categoryVO.setName(category.getName());
      categoryVO.setLevel(category.getLevel());

      List<CategoryVo> children = new ArrayList<>();
      List<LitemallCategory> subCategoryList = categoryService.queryByPid(category.getId());
      for (LitemallCategory subCategory : subCategoryList) {
        CategoryVo subCategoryVo = new CategoryVo();
        subCategoryVo.setId(subCategory.getId());
        subCategoryVo.setDesc(subCategory.getDesc());
        subCategoryVo.setIconUrl(subCategory.getIconUrl());
        subCategoryVo.setPicUrl(subCategory.getPicUrl());
        subCategoryVo.setKeywords(subCategory.getKeywords());
        subCategoryVo.setName(subCategory.getName());
        subCategoryVo.setLevel(subCategory.getLevel());

        children.add(subCategoryVo);
      }

      categoryVO.setChildren(children);
      categoryVoList.add(categoryVO);
    }

    return ResponseUtil.okList(categoryVoList);
  }

  private Object validate(LitemallCategory category) {
    String name = category.getName();
    if (StringUtils.isEmpty(name)) {
      return ResponseUtil.badArgument();
    }

    String level = category.getLevel();
    if (StringUtils.isEmpty(level)) {
      return ResponseUtil.badArgument();
    }
    if (!level.equals("L1") && !level.equals("L2")) {
      return ResponseUtil.badArgumentValue();
    }

    Integer pid = category.getPid();
    if (level.equals("L2") && (pid == null)) {
      return ResponseUtil.badArgument();
    }

    return null;
  }

  @RequiresPermissions("admin:category:list")
  @GetMapping("/l1")
  public Object catL1() {
    // 所有一级分类目录
    List<LitemallCategory> l1CatList = categoryService.queryL1();
    List<Map<String, Object>> data = new ArrayList<>(l1CatList.size());
    for (LitemallCategory category : l1CatList) {
      Map<String, Object> d = new HashMap<>(2);
      d.put("value", category.getId());
      d.put("label", category.getName());
      data.add(d);
    }
    return ResponseUtil.okList(data);
  }
}
