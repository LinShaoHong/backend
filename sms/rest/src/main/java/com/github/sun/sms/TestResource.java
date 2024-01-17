package com.github.sun.sms;

import com.github.sun.foundation.rest.AbstractResource;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/test")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Charge Resource")
public class TestResource extends AbstractResource {
  @GET
  @Path("/d")
  @ApiOperation("易千支付")
  public void qyAll() {
    int i = 1 / 0;
  }
}
