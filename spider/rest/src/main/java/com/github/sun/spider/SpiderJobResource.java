package com.github.sun.spider;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.spider.mapper.SpiderJobMapper;
import com.github.sun.spider.schedule.SpiderJobScheduler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Path("/api/spider/jobs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SpiderJobResource extends AbstractResource {
  private final SpiderJobScheduler scheduler;
  private final SpiderJobMapper mapper;
  private final SqlBuilder.Factory factory;

  @Inject
  public SpiderJobResource(SpiderJobScheduler scheduler,
                           SpiderJobMapper mapper,
                           @Named("mysql") SqlBuilder.Factory factory) {
    this.scheduler = scheduler;
    this.mapper = mapper;
    this.factory = factory;
  }

  @GET
  public ListResponse<SpiderJob> get(@QueryParam("group") String group) {
    if (group == null) {
      return responseOf(mapper.findAll());
    } else {
      SqlBuilder sb = factory.create();
      SqlBuilder.Template template = sb.from(SpiderJob.class).where(sb.field("group").eq(group)).template();
      return responseOf(mapper.findByTemplate(template));
    }
  }

  @GET
  @Path("/{id}")
  public SingleResponse<SpiderJob> getById(@PathParam("id") String id) {
    return responseOf(mapper.findById(id));
  }

  @GET
  @Path("/groups")
  public SetResponse<String> getGroups() {
    Set<String> names = Injector.interfaceOf(Spider.Processor.class)
      .stream()
      .map(v -> {
        Service a = v.getClass().getAnnotation(Service.class);
        String name = a == null ? null : a.value();
        if (name != null && name.endsWith(Spider.Processor.SUFFIX)) {
          name = name.substring(0, name.lastIndexOf(Spider.Processor.SUFFIX));
        }
        return name;
      }).filter(Objects::nonNull).collect(Collectors.toSet());
    return responseOf(names);
  }

  @PUT
  @Path("/{id}")
  public void get(@PathParam("id") String id,
                  @NotNull JobRequest req) {
    SpiderJob exist = mapper.findById(id);
    if (exist == null) {
      throw new NotFoundException();
    }
    SpiderJob job = SpiderJob.builder()
      .id(id)
      .group(req.getGroup())
      .startTime(req.getStartTime())
      .rate(req.getRate())
      .publish(req.isPublish())
      .setting(req.getSetting())
      .schema(req.getSchema())
      .build();
    mapper.update(job);
  }

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
    if (job.isPublish()) {
      scheduler.addJob(job);
    }
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    private Date startTime;
    private String rate;
    @NotNull(message = "require setting")
    private Setting setting;
    @NotNull(message = "require schema")
    private JsonNode schema;
  }
}
