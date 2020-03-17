package com.github.sun.xfg.admin;

import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.xfg.GirlMapper;
import io.swagger.annotations.Api;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/xfg/admin/stat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin Statistics Resource")
public class AdminStatResource extends AbstractResource {
  private final GirlMapper mapper;

  @Inject
  public AdminStatResource(GirlMapper mapper) {
    this.mapper = mapper;
  }
}
