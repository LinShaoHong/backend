package com.github.sun.mall.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.mall.core.entity.Category;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Path("/v1/mall/category")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall: Category Resource", tags = "类目服务")
public class CategoryResource extends AbstractResource {
  private final Cache<String, List<SingleCategoryResp>> cache = Caffeine.newBuilder()
    .expireAfterWrite(5, TimeUnit.MINUTES)
    .maximumSize(100)
    .build();
  private final CategoryMapper mapper;

  @Inject
  public CategoryResource(CategoryMapper mapper) {
    this.mapper = mapper;
  }

  @GET
  @ApiOperation("获取所有分类数据")
  public ListResponse<SingleCategoryResp> queryAll() {
    return responseOf(cache.get("CATEGORY", key -> {
      // 所有一级分类目录
      List<Category> list = mapper.findAll();
      Map<String, List<Category>> map = list.stream().collect(Collectors.groupingBy(Category::getPid));
      return list.stream()
        .filter(v -> v.getLevel() == 1)
        .map(v -> {
          List<Category> sub = map.get(v.getId());
          if (sub == null) {
            sub = Collections.emptyList();
          }
          return SingleCategoryResp.builder().currentCategory(v).currentSubCategory(sub).build();
        }).collect(Collectors.toList());
    }));
  }


  @GET
  @Path("/byLevel")
  @ApiOperation("获取相应层级类目")
  public ListResponse<Category> byLevel(@Min(value = 1, message = "最小层级为1") @Max(value = 2, message = "最大层级为2")
                                        @QueryParam("level") int level) {
    return responseOf(mapper.findByLevel(level));
  }

  @GET
  @Path("/byId")
  @ApiOperation("获取分类详情")
  public Object byId(@QueryParam("id") String id) {
    // 所有一级分类目录
    List<Category> categories1 = mapper.findByLevel(1);
    // 当前一级分类目录
    Category currentCategory;
    if (id != null) {
      currentCategory = mapper.findById(id);
    } else {
      currentCategory = categories1.get(0);
    }

    // 当前一级分类目录对应的二级分类目录
    List<Category> currentSubCategory = null;
    if (null != currentCategory) {
      currentSubCategory = mapper.findByParentId(currentCategory.getId());
    }
    return responseOf(CategoryResp.builder()
      .categories(categories1)
      .currentCategory(currentCategory)
      .currentSubCategory(currentSubCategory)
      .build());
  }

  @Data
  @Builder
  private static class CategoryResp {
    private List<Category> categories;
    private Category currentCategory;
    private List<Category> currentSubCategory;
  }

  @GET
  @Path("/${id}")
  @ApiOperation("当前分类栏目")
  public SingleResponse<SingleCategoryResp> current(@PathParam("id") String id) {
    // 当前分类
    Category c = mapper.findById(id);
    if (c == null) {
      throw new NotFoundException("Can not find category by id=" + id);
    }
    List<Category> sub = mapper.findByParentId(c.getId());
    return responseOf(SingleCategoryResp.builder()
      .currentCategory(c)
      .currentSubCategory(sub)
      .build());
  }

  @Data
  @Builder
  private static class SingleCategoryResp {
    private Category currentCategory;
    private List<Category> currentSubCategory;
  }
}
