package com.github.sun.scheduler;

import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.Lifecycle;
import com.github.sun.foundation.boot.Order;
import com.github.sun.foundation.quartz.Scheduler;
import org.quartz.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
@Order(Order.BACKGROUND_TASK - 100)
public class SchedulerJobStarter implements Lifecycle {
  private static final String JOB_ID = "JOB_ID";

  @Resource
  private Scheduler scheduler;
  @Resource
  private org.quartz.Scheduler quartz;
  @Resource
  private SchedulerJobMapper mapper;

  public void add(SchedulerJob schedulerJob) {
    Scheduler.Task task = new Scheduler.Task() {
      @Override
      public void run() {
      }

      @Override
      public String id() {
        return schedulerJob.getId();
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
    if (schedulerJob.getRate() == null) {
      scheduler.scheduleOnce(new Date(schedulerJob.getStartTime()), task);
    } else {
      Scheduler.Rate rate = Scheduler.parseRate(schedulerJob.getRate());
      scheduler.schedule(new Date(schedulerJob.getStartTime()), rate.value, rate.unit, task);
    }
  }

  public boolean has(String jobId) {
    try {
      return quartz.getJobDetail(new JobKey(jobId)) != null;
    } catch (SchedulerException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void update(SchedulerJob schedulerJob) {
    if (schedulerJob.getRate() == null) {
      scheduler.rescheduleOnce(new Date(schedulerJob.getStartTime()), schedulerJob.getId());
    } else {
      Scheduler.Rate rate = Scheduler.parseRate(schedulerJob.getRate());
      scheduler.reschedule(new Date(schedulerJob.getStartTime()), rate.value, rate.unit, schedulerJob.getId());
    }
  }

  public void delete(String jobId) {
    try {
      SchedulerTask task = (SchedulerTask) Injector.getInstance(jobId + SchedulerTask.SUFFIX);
      task.stop();
      quartz.deleteJob(new JobKey(jobId));
    } catch (SchedulerException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void pause(String jobId) {
    try {
      SchedulerTask task = (SchedulerTask) Injector.getInstance(jobId + SchedulerTask.SUFFIX);
      task.stop();
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

  @Override
  public void startup() {
    mapper.findAll().forEach(this::add);
  }

  public static class JobImpl implements Job {
    @Override
    public void execute(JobExecutionContext context) {
      JobDataMap data = context.getJobDetail().getJobDataMap();
      String id = (String) data.get(JOB_ID);
      SchedulerTask task = (SchedulerTask) Injector.getInstance(id + SchedulerTask.SUFFIX);
      task.start();
    }
  }

  @Override
  public void shutdown() {
  }
}
