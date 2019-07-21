package com.github.sun.spider;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Setting {
  /**
   * 采集线程相关参数
   */
  private int parallelism; // 线程处理数量
  private int poolSize; // 线程池大小
  private long monitorInterval; // 监控线程睡眠量
  private long taskInterval; // 线程单次处理睡眠量
  private long executeTime; // 采集时间量
  private boolean enable; // 是否允许采集

  /**
   * url访问相关参数
   */
  private int retryCount; // 重试次数
  private long retryDelays; // 重试睡眠毫秒量

  /**
   * processor 相关参数
   */
  private int batchSize; // 单次批处理量
  private int fetchSize; // jdbc fetchSize
  private long batchInterval; // 批处理睡眠量

  public void reCorrect() {
    if (this.retryCount == 0) {
      this.retryCount = 10;
    }
    if (this.retryDelays == 0) {
      this.retryDelays = 5000;
    }
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
    if (this.executeTime == 0) {
      this.executeTime = Long.MAX_VALUE;
    }
  }
}
