package com.github.sun.spider.rest;


import com.github.sun.foundation.rest.AbstractResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * @Author LinSH
 * @Date: 12:33 PM 2019-07-11
 */
@Path("/api/v1")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SpiderResource extends AbstractResource {
}
