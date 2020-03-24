package com.github.sun.scheduler;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public interface SchedulerTask {
  String SUFFIX = ":TASK";

  void start();

  void stop();

  Progress progress();

  List<Progress> latestProgress();

  @Data
  @Builder
  @JsonPropertyOrder({"running", "startTime", "endTime", "usedTime", "errors"})
  class Progress implements Cloneable {
    private boolean running;
    private long total;
    private long finished;
    private String startTime;
    private String endTime;
    private String usedTime;
    private List<String> errors;

    @Override
    public Progress clone() {
      try {
        return (Progress) super.clone();
      } catch (CloneNotSupportedException ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
