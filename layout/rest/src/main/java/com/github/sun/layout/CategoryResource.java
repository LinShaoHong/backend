package com.github.sun.layout;

import com.github.sun.foundation.rest.AbstractResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/categories")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api("layout: 布局服务")
public class CategoryResource extends AbstractResource {
  private final CategoryService service;

  @Inject
  public CategoryResource(CategoryService service) {
    this.service = service;
  }

  @GET
  @ApiOperation("获取目录列表")
  public ListResponse<Category> getAll() {
    return responseOf(service.getAll());
  }
}
