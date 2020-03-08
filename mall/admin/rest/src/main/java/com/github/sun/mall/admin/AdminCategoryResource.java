package com.github.sun.mall.admin;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.IdGenerator;
import com.github.sun.mall.admin.auth.Authentication;
import com.github.sun.mall.admin.entity.Admin;
import com.github.sun.mall.core.CategoryMapper;
import com.github.sun.mall.core.entity.Category;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Builder;
import lombok.Data;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/v1/mall/admin/category")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "mall-admin: 类目管理: category")
public class AdminCategoryResource extends AbstractResource {
  private final CategoryMapper mapper;

  @Inject
  public AdminCategoryResource(CategoryMapper mapper) {
    this.mapper = mapper;
  }

  @GET
  @ApiOperation("获取所有类目")
  @Authentication(value = "admin:category:query", tags = {"商场管理", "类目管理", "查询"})
  public ListResponse<CategoryTreeResp> getAll(@Context Admin admin) {
    List<Category> categories = mapper.findAll();
    Map<String, List<Category>> map = categories.stream().collect(Collectors.groupingBy(Category::getPId));
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
  public ListResponse<CategoryResp> getLevel1(@Context Admin admin) {
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

  @POST
  @ApiOperation("创建")
  @Authentication(value = "admin:category:create", tags = {"商场管理", "类目管理", "添加"})
  public Response create(@Valid @NotNull(message = "缺少实体") Category v,
                         @Context Admin admin) {
    v.setId(IdGenerator.next());
    mapper.insert(v);
    return responseOf();
  }

  @GET
  @Path("/${id}")
  @ApiOperation("获取")
  @Authentication(value = "admin:category:detail", tags = {"商场管理", "类目管理", "详情"})
  public SingleResponse<Category> get(@PathParam("id") String id,
                                      @Context Admin admin) {
    Category v = mapper.findById(id);
    if (v == null) {
      throw new NotFoundException("Can not find Category by id=" + id);
    }
    return responseOf(v);
  }

  @PUT
  @Path("/${id}")
  @ApiOperation("编辑")
  @Authentication(value = "admin:category:update", tags = {"商场管理", "类目管理", "编辑"})
  public Response update(@PathParam("id") String id,
                         @Valid @NotNull(message = "缺少实体") Category v,
                         @Context Admin admin) {
    Category e = mapper.findById(id);
    if (e == null) {
      throw new NotFoundException("Can not find Category by id=" + id);
    }
    v.setId(id);
    mapper.update(v);
    return responseOf();
  }

  @DELETE
  @Path("/${id}")
  @ApiOperation("删除")
  @Authentication(value = "admin:category:delete", tags = {"商场管理", "类目管理", "删除"})
  public Response delete(@PathParam("id") String id,
                         @Context Admin admin) {
    Category v = mapper.findById(id);
    if (v == null) {
      throw new NotFoundException("Can not find Category by id=" + id);
    }
    mapper.deleteById(id);
    return responseOf();
  }
}
