package com.github.sun.whispered.rest;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.layout.Category;
import com.github.sun.layout.CategoryProvider;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.stream.Collectors;

@Path("/api/v1/categories")
@Component
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CategoryResource extends AbstractResource {
  private final List<CategoryProvider> providers;

  @Inject
  public CategoryResource(List<CategoryProvider> providers) {
    this.providers = providers;
  }

  @GET
  public ListResponse<Category> getAll() {
    return responseOf(providers.stream().map(CategoryProvider::provide).collect(Collectors.toList()));
  }
}
