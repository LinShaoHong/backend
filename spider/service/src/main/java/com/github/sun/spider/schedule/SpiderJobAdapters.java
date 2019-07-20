package com.github.sun.spider.schedule;

import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.Lifecycle;
import com.github.sun.foundation.boot.Order;
import com.github.sun.foundation.quartz.Scheduler;
import com.github.sun.spider.Spider;
import com.github.sun.spider.SpiderJob;
import com.github.sun.spider.mapper.SpiderJobMapper;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
@Order(Order.BACKGROUND_TASK - 100)
public class SpiderJobAdapters implements Lifecycle {
  private static final String JOB_ID = "JOB_ID";

  @Resource
  private SpiderJobMapper mapper;
  @Autowired
  private Spider.Factory factory;

  public void startup() {
    List<SpiderJob> jobs = mapper.findAll();
    jobs.stream().filter(SpiderJob::isPublish).forEach(job -> {
      Scheduler.JobAdapter adapter = new Scheduler.JobAdapter() {
        @Override
        public String id() {
          return job.getId();
        }

        @Override
        public Date start() {
          return job.getStartTime();
        }

        @Override
        public String rate() {
          return job.getRate();
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
      Injector.inject(job.getId() + ":job:spider", adapter);
    });
  }

  @Component
  private class JobImpl implements Job {
    @Override
    public void execute(JobExecutionContext context) {
      JobDataMap data = context.getJobDetail().getJobDataMap();
      String id = (String) data.get(JOB_ID);
      SpiderJob job = mapper.findById(id);
      String name = job.getGroup() + Spider.Processor.SUFFIX;
      Spider.Processor processor = (Spider.Processor) Injector.getInstance(name);
      Spider spider = factory.create(job.getSetting(), job.getSchema(), processor);
      spider.start();
    }
  }

  @Override
  public void shutdown() {
  }
}
