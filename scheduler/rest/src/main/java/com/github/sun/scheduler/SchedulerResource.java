package com.github.sun.scheduler;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.rest.AbstractResource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Path("/v1/scheduler/jobs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SchedulerResource extends AbstractResource {
  private final SchedulerJobStarter scheduler;
  private final SchedulerJobMapper mapper;

  @Inject
  public SchedulerResource(SchedulerJobStarter scheduler,
                           SchedulerJobMapper mapper) {
    this.scheduler = scheduler;
    this.mapper = mapper;
  }

  @GET
  public ListResponse<SchedulerJobRes> getAll() {
    return responseOf(mapper.findAll().stream()
      .map(this::from).collect(Collectors.toList()));
  }

  @GET
  @Path("/${id}")
  public SingleResponse<SchedulerJobRes> getById(@PathParam("id") String id) {
    SchedulerJob job = mapper.findById(id);
    if (job == null) {
      throw new NotFoundException("资源未找到");
    }
    return responseOf(from(job));
  }

  private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  private SchedulerJobRes from(SchedulerJob job) {
    Date date = scheduler.getNextTime(job.getId());
    String nextTime = date == null ? null : format.format(date);
    return new SchedulerJobRes(job, nextTime);
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class SchedulerJobRes {
    @JsonUnwrapped
    private SchedulerJob schedulerJob;
    private String nextTime;
  }

  @GET
  @Path("/names")
  public ListResponse<NameRes> getNames() {
    List<NameRes> groups = Injector.interfaceOf(SchedulerTask.class)
      .stream()
      .map(v -> {
        Service a = v.getClass().getAnnotation(Service.class);
        String name = a == null ? null : a.value();
        if (name != null && name.endsWith(SchedulerTask.SUFFIX)) {
          name = name.substring(0, name.lastIndexOf(SchedulerTask.SUFFIX));
        }
        return NameRes.builder().label(name).value(name).build();
      }).filter(Objects::nonNull).collect(Collectors.toList());
    return responseOf(groups);
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class NameRes {
    public String label;
    public String value;
  }

  @POST
  public void create(@NotNull JobRequest req) {
    SchedulerJob job = SchedulerJob.builder()
      .id(req.getId())
      .startTime(req.getStartTime())
      .rate(req.getRate())
      .profiles(req.getProfiles())
      .build();
    if (job.isPublish()) {
      scheduler.add(job);
    }
    try {
      mapper.insert(job);
    } catch (Throwable ex) {
      scheduler.delete(job.getId());
      throw ex;
    }
  }

  @PUT
  @Path("/${id}")
  public void update(@PathParam("id") String id,
                     @NotNull JobRequest req) {
    SchedulerJob exist = mapper.findById(id);
    if (exist == null) {
      throw new NotFoundException("资源未找到");
    }
    SchedulerJob job = SchedulerJob.builder()
      .id(id)
      .startTime(req.getStartTime())
      .rate(req.getRate())
      .publish(exist.isPublish())
      .profiles(req.getProfiles())
      .build();
    if (job.isPublish()) {
      if (job.needReschedule(exist)) {
        scheduler.update(job);
      }
    } else if (exist.isPublish()) {
      scheduler.pause(id);
    }
    try {
      mapper.update(job);
    } catch (Throwable ex) {
      if (exist.isPublish()) {
        scheduler.update(exist);
      }
      throw ex;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  private static class JobRequest {
    @NotNull(message = "require id")
    private String id;
    @NotNull(message = "require startTime")
    private long startTime;
    private String rate;
    @NotNull(message = "require profiles")
    private JsonNode profiles;
  }

  @DELETE
  @Path("${id}")
  public void delete(@PathParam("id") String id) {
    scheduler.delete(id);
    mapper.deleteById(id);
  }

  @PUT
  @Path("/publish/${id}")
  public void publish(@PathParam("id") String id) {
    SchedulerJob job = mapper.findById(id);
    if (job == null) {
      throw new NotFoundException("资源未找到");
    }
    if (!job.isPublish()) {
      job.setPublish(true);
      if (!scheduler.has(job.getId())) {
        scheduler.add(job);
      } else {
        scheduler.update(job);
      }
    }
    mapper.update(job);
  }

  @PUT
  @Path("/unPublish/${id}")
  public void unPublish(@PathParam("id") String id) {
    SchedulerJob job = mapper.findById(id);
    if (job == null) {
      throw new NotFoundException("资源未找到");
    }
    if (job.isPublish()) {
      job.setPublish(false);
    }
    scheduler.pause(id);
    mapper.update(job);
  }

  @GET
  @Path("/progress/latest/${id}")
  public ListResponse<ProgressRes> getLatestProgress(@PathParam("id") String id) {
    SchedulerTask task = (SchedulerTask) Injector.getInstance(id + SchedulerTask.SUFFIX);
    List<ProgressRes> latest = task.latestProgress().stream()
      .map(ProgressRes::from)
      .collect(Collectors.toList());
    Collections.reverse(latest);
    return responseOf(latest);
  }

  @GET
  @Path("/progress/${id}")
  public SingleResponse<ProgressRes> getProgress(@PathParam("id") String id) {
    SchedulerTask task = (SchedulerTask) Injector.getInstance(id + SchedulerTask.SUFFIX);
    Date nextTime = scheduler.getNextTime(id);
    return responseOf(ProgressRes.from(nextTime, task.progress()));
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class ProgressRes {
    private long total;
    private long finished;
    private String endTime;
    private String usedTime;
    private String startTime;
    private boolean isRunning;
    private String remainTime;
    private List<String> errors;

    private static ProgressRes from(SchedulerTask.Progress p) {
      return from(null, p);
    }

    private static ProgressRes from(Date next, SchedulerTask.Progress p) {
      String remainTime = null;
      if (next != null) {
        long remains = next.getTime() - System.currentTimeMillis();
        remainTime = Dates.formatTime(remains);
      }
      if (p == null) {
        return ProgressRes.builder()
          .remainTime(remainTime)
          .errors(Collections.emptyList())
          .build();
      } else {
        return ProgressRes.builder()
          .total(p.getTotal())
          .isRunning(p.isRunning())
          .finished(p.getFinished())
          .startTime(p.getStartTime())
          .endTime(p.getEndTime())
          .usedTime(p.getUsedTime())
          .remainTime(remainTime)
          .errors(p.getErrors())
          .build();
      }
    }
  }
}
