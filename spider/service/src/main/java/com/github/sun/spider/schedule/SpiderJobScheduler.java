package com.github.sun.spider.schedule;

import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.Lifecycle;
import com.github.sun.foundation.boot.Order;
import com.github.sun.foundation.boot.utility.Cache;
import com.github.sun.foundation.quartz.Scheduler;
import com.github.sun.spider.Spider;
import com.github.sun.spider.SpiderJob;
import com.github.sun.spider.mapper.SpiderJobMapper;
import org.quartz.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
@Order(Order.BACKGROUND_TASK - 100)
public class SpiderJobScheduler implements Lifecycle {
  private static final String JOB_ID = "JOB_ID";
  private static final Cache<String, Spider> cache = new Cache<>();

  @Resource
  private Scheduler scheduler;
  @Resource
  private org.quartz.Scheduler quartz;
  @Resource
  private SpiderJobMapper mapper;

  public void add(SpiderJob spiderJob) {
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
      scheduler.scheduleOnce(new Date(spiderJob.getStartTime()), task);
    } else {
      Scheduler.Rate rate = Scheduler.parseRate(spiderJob.getRate());
      scheduler.schedule(new Date(spiderJob.getStartTime()), rate.value, rate.unit, task);
    }
  }

  public boolean has(String jobId) {
    try {
      return quartz.getJobDetail(new JobKey(jobId)) != null;
    } catch (SchedulerException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void update(SpiderJob spiderJob) {
    if (spiderJob.getRate() == null) {
      scheduler.rescheduleOnce(new Date(spiderJob.getStartTime()), spiderJob.getId());
    } else {
      Scheduler.Rate rate = Scheduler.parseRate(spiderJob.getRate());
      scheduler.reschedule(new Date(spiderJob.getStartTime()), rate.value, rate.unit, spiderJob.getId());
    }
  }

  public void delete(String jobId) {
    Spider spider = cache.get(jobId);
    if (spider != null) {
      spider.stop();
      cache.remove(jobId);
    }
    try {
      quartz.deleteJob(new JobKey(jobId));
    } catch (SchedulerException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void pause(String jobId) {
    Spider spider = cache.get(jobId);
    if (spider != null) {
      spider.stop();
    }
    try {
      quartz.pauseJob(new JobKey(jobId));
    } catch (SchedulerException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Date getNextTime(String jobId) {
    try {
      Trigger trigger = quartz.getTrigger(new TriggerKey(jobId));
      return trigger == null ? null : trigger.getNextFireTime();
    } catch (SchedulerException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Spider getSpider(String jobId) {
    return cache.get(jobId);
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
