package com.github.sun.spider;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.exception.NotFoundException;
import com.github.sun.foundation.boot.utility.Dates;
import com.github.sun.foundation.boot.utility.Throws;
import com.github.sun.foundation.rest.AbstractResource;
import com.github.sun.foundation.sql.SqlBuilder;
import com.github.sun.spider.mapper.SpiderJobMapper;
import com.github.sun.spider.schedule.SpiderJobScheduler;
import com.github.sun.spider.spi.BasicSpider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Path("/v1/spider/jobs")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SpiderResource extends AbstractResource {
    private final SpiderJobScheduler scheduler;
    private final SpiderJobMapper mapper;
    private final SqlBuilder.Factory factory;

    @Inject
    public SpiderResource(SpiderJobScheduler scheduler,
                          SpiderJobMapper mapper,
                          @Named("mysql") SqlBuilder.Factory factory) {
        this.scheduler = scheduler;
        this.mapper = mapper;
        this.factory = factory;
    }

    @GET
    public ListResponse<SpiderJobRes> get(@QueryParam("group") String group) {
        if (group == null) {
            return responseOf(mapper.findAll().stream()
                    .map(this::from).collect(Collectors.toList()));
        } else {
            SqlBuilder sb = factory.create();
            SqlBuilder.Template template = sb.from(SpiderJob.class).where(sb.field("group").eq(group)).template();
            return responseOf(mapper.findByTemplate(template).stream()
                    .map(this::from).collect(Collectors.toList()));
        }
    }

    @GET
    @Path("/{id}")
    public SingleResponse<SpiderJobRes> getById(@PathParam("id") String id) {
        SpiderJob job = mapper.findById(id);
        if (job == null) {
            throw new NotFoundException("资源未找到");
        }
        return responseOf(from(job));
    }

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private SpiderJobRes from(SpiderJob job) {
        Date date = scheduler.getNextTime(job.getId());
        String nextTime = date == null ? null : format.format(date);
        return new SpiderJobRes(job, nextTime);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SpiderJobRes {
        @JsonUnwrapped
        private SpiderJob spiderJob;
        private String nextTime;
    }

    @GET
    @Path("/groups")
    public ListResponse<GroupRes> getGroups() {
        List<GroupRes> groups = Injector.interfaceOf(Spider.Processor.class)
                .stream()
                .map(v -> {
                    Service a = v.getClass().getAnnotation(Service.class);
                    String name = a == null ? null : a.value();
                    if (name != null && name.endsWith(Spider.Processor.SUFFIX)) {
                        name = name.substring(0, name.lastIndexOf(Spider.Processor.SUFFIX));
                    }
                    return GroupRes.builder().label(name).value(name).build();
                }).filter(Objects::nonNull).collect(Collectors.toList());
        return responseOf(groups);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class GroupRes {
        public String label;
        public String value;
    }

    @POST
    public void create(@NotNull JobRequest req) {
        JsonNode schema = req.getSchema();
        String id = schema.get("source").asText();
        req.getSetting().reCorrect();
        SpiderJob job = SpiderJob.builder()
                .id(id)
                .group(req.getGroup())
                .startTime(req.getStartTime())
                .rate(req.getRate())
                .setting(req.getSetting())
                .schema(req.getSchema())
                .checkpoint(req.checkpoint)
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
    @Path("/{id}")
    public void update(@PathParam("id") String id,
                       @NotNull JobRequest req) {
        SpiderJob exist = mapper.findById(id);
        if (exist == null) {
            throw new NotFoundException("资源未找到");
        }
        SpiderJob job = SpiderJob.builder()
                .id(id)
                .group(req.getGroup())
                .startTime(req.getStartTime())
                .rate(req.getRate())
                .publish(exist.isPublish())
                .setting(req.getSetting())
                .schema(req.getSchema())
                .checkpoint(req.getCheckpoint())
                .build();
        if (job.isPublish()) {
            if (job.needReschedule(exist)) {
                scheduler.update(job);
            }
            Spider spider = scheduler.getSpider(id);
            if (spider != null) {
                spider.setSetting(req.getSetting());
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
        @NotNull(message = "require group")
        private String group;
        @NotNull(message = "require startTime")
        private long startTime;
        private String time;
        private String rate;
        @NotNull(message = "require setting")
        private Setting setting;
        private Spider.Checkpoint checkpoint;
        @NotNull(message = "require schema")
        private JsonNode schema;
    }

    @DELETE
    @Path("{id}")
    public void delete(@PathParam("id") String id) {
        scheduler.delete(id);
        mapper.deleteById(id);
    }

    @DELETE
    @Path("/checkpoint/{id}")
    public void clearCheckpoint(@PathParam("id") String id) {
        SqlBuilder sb = factory.create();
        SqlBuilder.Template template = sb.from(SpiderJob.class)
                .update()
                .set("checkpoint", null)
                .template();
        mapper.updateByTemplate(template);
        Spider spider = scheduler.getSpider(id);
        if (spider != null && !spider.isRunning()) {
            spider.setCheckpoint(null);
        }
    }

    @PUT
    @Path("/publish/{id}")
    public void publish(@PathParam("id") String id) {
        SpiderJob spiderJob = mapper.findById(id);
        if (spiderJob == null) {
            throw new NotFoundException("资源未找到");
        }
        if (!spiderJob.isPublish()) {
            spiderJob.setPublish(true);
            if (!scheduler.has(spiderJob.getId())) {
                scheduler.add(spiderJob);
            } else {
                scheduler.update(spiderJob);
            }
        }
        mapper.update(spiderJob);
    }

    @PUT
    @Path("/unPublish/{id}")
    public void unPublish(@PathParam("id") String id) {
        SpiderJob spiderJob = mapper.findById(id);
        if (spiderJob == null) {
            throw new NotFoundException("资源未找到");
        }
        if (spiderJob.isPublish()) {
            spiderJob.setPublish(false);
        }
        scheduler.pause(id);
        mapper.update(spiderJob);
    }

    @GET
    @Path("/progress/latest/{id}")
    public ListResponse<ProgressRes> getLatestProgress(@PathParam("id") String id) {
        Spider spider = scheduler.getSpider(id);
        if (spider == null) {
            return responseOf(Collections.emptyList());
        }
        List<ProgressRes> latest = spider.latestProgress().stream()
                .map(p -> ProgressRes.from(spider.checkpoint(), spider.checkPointing(), p))
                .collect(Collectors.toList());
        Collections.reverse(latest);
        return responseOf(latest);
    }

    @GET
    @Path("/progress/{id}")
    public SingleResponse<ProgressRes> getProgress(@PathParam("id") String id) {
        Spider spider = scheduler.getSpider(id);
        Date nextTime = scheduler.getNextTime(id);
        return responseOf(ProgressRes.from(nextTime,
                spider == null ? null : spider.checkpoint(),
                spider == null ? null : spider.checkPointing(),
                spider == null ? null : spider.progress()));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ProgressRes {
        private int total;
        private int finished;
        private String endTime;
        private String usedTime;
        private int parallelism;
        private String startTime;
        private boolean isRunning;
        private String remainTime;
        private Spider.Checkpoint checkpoint;
        private Spider.Checkpoint checkPointing;
        private Set<String> errors;

        private static ProgressRes from(Spider.Checkpoint checkpoint, Spider.Checkpoint checkPointing, Spider.Progress p) {
            return from(null, checkpoint, checkPointing, p);
        }

        private static ProgressRes from(Date next, Spider.Checkpoint checkpoint, Spider.Checkpoint checkPointing, Spider.Progress p) {
            String remainTime = null;
            if (next != null) {
                long remains = next.getTime() - System.currentTimeMillis();
                remainTime = Dates.formatTime(remains);
            }
            if (p == null) {
                return ProgressRes.builder()
                        .remainTime(remainTime)
                        .checkpoint(checkpoint == null ? new Spider.Checkpoint(null, null) : checkpoint)
                        .checkPointing(checkPointing == null ? new Spider.Checkpoint(null, null) : checkPointing)
                        .errors(Collections.emptySet())
                        .build();
            } else {
                return ProgressRes.builder()
                        .parallelism(p.getParallelism())
                        .total(p.getTotal())
                        .isRunning(p.isRunning())
                        .finished(p.getFinished())
                        .startTime(p.getStartTime())
                        .endTime(p.getEndTime())
                        .usedTime(p.getUsedTime())
                        .remainTime(remainTime)
                        .checkpoint(checkpoint == null ? new Spider.Checkpoint(null, null) : checkpoint)
                        .checkPointing(checkPointing == null ? new Spider.Checkpoint(null, null) : checkPointing)
                        .errors(p.getErrors().stream().map(Throws::stackTraceOf).collect(Collectors.toSet()))
                        .build();
            }
        }
    }

    private static final Map<String, Holder> holders = new ConcurrentHashMap<>();

    @POST
    @Path("/test")
    public SingleResponse<TestRes> test(@NotNull TestReq req) {
        String key = req.getRequestId();
        TestRes res = new TestRes(404, new HashSet<>(), new ArrayList<>());
        if (!key.isEmpty()) {
            Holder holder = holders.get(key);
            if (holder != null) {
                res = holder.get();
                if (res.getCode() != 200 &&
                        (holder.spider.errors().size() >= BasicSpider.MAX_ERRORS_SIZE || !holder.spider.isRunning())) {
                    Set<String> errors = holder.spider.errors()
                            .stream().map(Throws::stackTraceOf)
                            .collect(Collectors.toSet());
                    res.setErrors(errors);
                    res.setCode(200);
                    holder.stop();
                }
            } else {
                holder = new Holder(req);
                holder.start();
                holders.put(key, holder);
            }
            if (res.getCode() == 200) {
                holders.remove(key);
            }
        }
        return responseOf(res);
    }

    @DELETE
    @Path("/test/{requestId}")
    public void deleteTest(@PathParam("requestId") String requestId) {
        Holder holder = holders.get(requestId);
        if (holder != null) {
            holder.stop();
            holders.remove(requestId);
        }
    }

    private static class Holder {
        private final Spider spider;
        private final List<JsonNode> values = new ArrayList<>();
        private final CountDownLatch startSingle = new CountDownLatch(1);
        private TestRes res = new TestRes(404, new HashSet<>(), new ArrayList<>());

        private Holder(TestReq req) {
            this.spider = new BasicSpider();
            this.spider.setSetting(req.getSetting());
            this.spider.setSchema(req.getSchema());
            this.spider.setProcessorProvider(() -> (source, nodes, setting, func) -> {
                values.addAll(nodes);
                stop();
                return nodes.size();
            });
        }

        private TestRes get() {
            return res;
        }

        private void stop() {
            spider.stop();
            startSingle.countDown();
        }

        private void start() {
            new Thread(() -> {
                spider.start();
                try {
                    startSingle.await();
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                Set<String> errors = spider.errors()
                        .stream().map(Throws::stackTraceOf)
                        .collect(Collectors.toSet());
                res = new TestRes(200, errors, values);
            }).start();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TestRes {
        private int code;
        private Set<String> errors;
        private List<JsonNode> values;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TestReq {
        @NotNull(message = "require requestId")
        private String requestId;
        @NotNull(message = "require setting")
        private Setting setting;
        @NotNull(message = "require schema")
        private JsonNode schema;
    }
}
