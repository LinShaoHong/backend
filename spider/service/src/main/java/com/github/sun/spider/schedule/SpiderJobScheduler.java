package com.github.sun.spider.schedule;

import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.Lifecycle;
import com.github.sun.foundation.boot.Order;
import com.github.sun.foundation.boot.utility.Cache;
import com.github.sun.foundation.quartz.Scheduler;
import com.github.sun.spider.Spider;
import com.github.sun.spider.SpiderJob;
import com.github.sun.spider.mapper.SpiderJobMapper;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@Order(Order.BACKGROUND_TASK - 100)
public class SpiderJobScheduler implements Lifecycle {
  private static final String JOB_ID = "JOB_ID";
  private static final Cache<String, Spider> cache = new Cache<>();

  @Resource
  private Scheduler scheduler;
  @Resource
  private SpiderJobMapper mapper;

  public void add(SpiderJob spiderJob) {
    if (scheduler.has(spiderJob.getId())) {
      scheduler.delete(spiderJob.getId());
    }
    Scheduler.Task task = new Scheduler.Task() {
      @Override
      public void run() {
      }

      @Override
      public String id() {
        return spiderJob.getId();
      }

      @Override
      public JobDataMap data() {
        JobDataMap data = new JobDataMap();
        data.put(JOB_ID, id());
        return data;
      }

      @Override
      public Class<? extends Job> jobClass() {
        return JobImpl.class;
      }
    };
    if (spiderJob.getRate() == null) {
      scheduler.scheduleOnce(spiderJob.getStartTime(), task);
    } else {
      Scheduler.Rate rate = Scheduler.parseRate(spiderJob.getRate());
      scheduler.schedule(spiderJob.getStartTime(), rate.value, rate.unit, task);
    }
  }

  public void delete(String taskId) {
    Spider spider = cache.get(taskId);
    if (spider != null) {
      spider.stop();
      cache.remove(taskId);
    }
    scheduler.delete(taskId);
  }

  public void pause(String taskId) {
    Spider spider = cache.get(taskId);
    if (spider != null) {
      spider.stop();
    }
    scheduler.pause(taskId);
  }

  public Spider getSpider(String taskId) {
    return cache.get(taskId);
  }

  @Override
  public void startup() {
    mapper.findAll().stream().filter(SpiderJob::isPublish).forEach(this::add);
  }

  public static class JobImpl implements Job {
    @Override
    public void execute(JobExecutionContext context) {
      JobDataMap data = context.getJobDetail().getJobDataMap();
      String id = (String) data.get(JOB_ID);
      SpiderJobMapper mapper = Injector.getInstance(SpiderJobMapper.class);
      SpiderJob job = mapper.findById(id);
      String name = job.getGroup() + Spider.Processor.SUFFIX;
      Spider.Processor processor = (Spider.Processor) Injector.getInstance(name);
      Spider.Factory factory = Injector.getInstance(Spider.Factory.class);
      Spider spider = cache.get(id, () -> factory.create(job.getSetting(), job.getSchema(), processor));
      spider.setSetting(job.getSetting());
      spider.setSchema(job.getSchema());
      spider.setProcessorProvider(() -> processor);
      spider.start();
    }
  }

  @Override
  public void shutdown() {
  }
}
