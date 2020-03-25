package com.github.sun.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@Service("PING" + SchedulerTask.SUFFIX)
public class PingTask implements SchedulerTask {
  @Resource
  private SchedulerJobMapper mapper;

  @Override
  public void start() {
  }

  @Override
  public void stop() {
  }

  @Override
  public Progress progress() {
    return null;
  }

  @Override
  public List<Progress> latestProgress() {
    return null;
  }
}
