package com.github.sun.mall.admin;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.sun.mall.core.CategoryMapper;
import com.github.sun.mall.core.entity.Category;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/v1/mall/admin/category")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 类目管理: category")
public class AdminCategoryResource extends BasicCURDResource<Category, CategoryMapper> {

  @GET
  @ApiOperation("获取所有类目")
  public ListResponse<CategoryTreeResp> list() {
    List<Category> categories = mapper.findAll();
    Map<String, List<Category>> map = categories.stream().collect(Collectors.groupingBy(Category::getPid));
    List<Category> roots = categories.stream().filter(v -> v.getLevel() == 1).collect(Collectors.toList());
    return responseOf(roots.stream().map(root -> CategoryTreeResp.builder()
      .category(root)
      .children(map.getOrDefault(root.getId(), Collections.emptyList()))
      .build()).collect(Collectors.toList()));
  }

  @Data
  @Builder
  private static class CategoryTreeResp {
    @JsonUnwrapped
    private Category category;
    private List<Category> children;
  }

  @GET
  @Path("/l1")
  @ApiOperation("获取所有一级类目")
  public ListResponse<CategoryResp> getLevel1() {
    return responseOf(mapper.findByLevel(1).stream().map(v -> CategoryResp.builder()
      .value(v.getId())
      .label(v.getName())
      .build()).collect(Collectors.toList()));
  }

  @Data
  @Builder
  private static class CategoryResp {
    private String value;
    private String label;
  }

  @Override
  protected String name() {
    return "类目";
  }
}
