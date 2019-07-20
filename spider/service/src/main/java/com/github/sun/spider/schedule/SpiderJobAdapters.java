package com.github.sun.spider.schedule;

import com.github.sun.foundation.boot.Injector;
import com.github.sun.foundation.boot.Lifecycle;
import com.github.sun.foundation.boot.Order;
import com.github.sun.foundation.quartz.Scheduler;
import com.github.sun.spider.Spider;
import com.github.sun.spider.SpiderJob;
import com.github.sun.spider.mapper.SpiderJobMapper;
import com.github.sun.spider.spi.BasicSpider;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
@Order(Order.BACKGROUND_TASK - 100)
public class SpiderJobAdapters implements Lifecycle {
  private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private static final String JOB_ID = "JOB_ID";

  @Resource
  private SpiderJobMapper mapper;

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
          try {
            return df.parse(job.getStartTime());
          } catch (ParseException ex) {
            throw new RuntimeException("invalid datetime: " + job.getStartTime(), ex);
          }
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
      Injector.inject(job.getId() + ":spider", adapter);
    });
  }

  @Component
  private class JobImpl implements Job {
    @Override
    public void execute(JobExecutionContext context) {
      JobDataMap data = context.getJobDetail().getJobDataMap();
      String id = (String) data.get(JOB_ID);
      SpiderJob job = mapper.findById(id);
      Spider spider = new BasicSpider() {
        @Override
        protected Processor create() {
          return (Processor) Injector.getInstance(job.getGroup() + Processor.SUFFIX);
        }
      };
      spider.setSchema(job.getSchema());
      spider.setSetting(job.getSetting());
      spider.start();
    }
  }

  @Override
  public void shutdown() {
  }
}
