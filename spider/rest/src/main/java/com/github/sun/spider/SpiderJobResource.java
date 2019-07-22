package com.github.sun.spider;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.spider.mapper.SpiderJobMapper;
import com.github.sun.spider.schedule.SpiderJobScheduler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
  private SpiderJobScheduler scheduler;
  @Autowired
  private SpiderJobMapper mapper;

  @POST
  public void create(@NotNull JobRequest req) {
    JsonNode schema = req.getSchema();
    String id = schema.get("source").asText();
    SpiderJob job = SpiderJob.builder()
      .id(id)
      .group(req.getGroup())
      .startTime(req.getStartTime())
      .rate(req.getRate())
      .publish(req.isPublish())
      .setting(req.getSetting())
      .schema(req.getSchema())
      .build();
    scheduler.addJob(job);
    mapper.insert(job);
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class JobRequest {
    @NotNull(message = "require group")
    private String group;
    private boolean publish;
    @NotNull(message = "require startTime")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy hh:mm:ss")
    private Date startTime;
    private String rate;
    @NotNull(message = "require setting")
    private Setting setting;
    @NotNull(message = "require schema")
    private JsonNode schema;
  }
}
