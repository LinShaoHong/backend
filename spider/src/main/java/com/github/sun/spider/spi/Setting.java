package com.github.sun.spider.spi;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Setting {
  private int parallelism;
  private int poolSize;
  private int batchSize;
  private int fetchSize;
  private long batchInterval;
  private long taskInterval;
  private long monitorInterval;
  private boolean enable;

  public void reCorrect() {
    if (this.parallelism < 0) {
      this.parallelism = 0;
    }
    if (this.poolSize == 0) {
      this.poolSize = 10;
    }
    if (this.fetchSize == 0) {
      this.fetchSize = 10000;
    }
    if (this.batchSize == 0) {
      this.batchSize = 1000;
    }
    if (this.batchInterval == 0) {
      this.batchInterval = 200;
    }
    if (this.taskInterval == 0) {
      this.taskInterval = 10;
    }
    if (this.monitorInterval == 0) {
      this.monitorInterval = 60 * 1000;
    }
  }
}
