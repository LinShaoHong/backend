package com.github.sun.xzyy.admin;

import com.github.sun.foundation.rest.AbstractResource;
import io.swagger.annotations.Api;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/v1/xzyy/admin/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Admin User Resource")
public class AdminUserResource extends AbstractResource {
}
