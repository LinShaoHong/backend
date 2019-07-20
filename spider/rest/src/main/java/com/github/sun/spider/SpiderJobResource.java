package com.github.sun.spider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.spider.mapper.SpiderJobMapper;
import org.springframework.beans.factory.annotation.Autowired;

import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Date;

@Path("/api/spider/jobs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SpiderJobResource extends AbstractResource {
  @Autowired
  private SpiderJobMapper mapper;

  @POST
  public void create(@NotNull SpiderJob job) {
    JsonNode schema = job.getSchema();
    String id = schema.get("source").asText();
    job.setId(id);
    job.setCreateTime(new Date());
    job.setUpdateTime(new Date());
    mapper.insert(job);
  }
}
