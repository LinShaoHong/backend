package com.github.sun.spider.spi;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public interface Spider {
  /**
   * 来源
   */
  String source();

  /**
   * 开始采集
   */
  void start();

  /**
   * 停止采集
   */
  void stop();

  /**
   * @return 是否在采集
   */
  boolean isRunning();

  /**
   * 增加一个线程，加快采集速率
   */
  void add();

  /**
   * 移除一个线程，减缓采集速率
   */
  void remove();

  /**
   * @return 采集进度
   */
  Progress progress();

  @Data
  @Builder
  @JsonPropertyOrder({"source", "running", "parallelism", "total", "finished", "startTime", "endTime", "usedTime"})
  class Progress {
    /**
     * 来源
     */

    private String source;

    /**
     * 并行数
     */
    private int parallelism;

    /**
     * 采集页总数
     */
    private int total;

    /**
     * 是否在执行采集
     */
    private boolean isRunning;

    /**
     * 采集的条数
     */
    private int finished;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 耗时
     */
    private String usedTime;
  }

  interface Processor {
    void process(List<JsonNode> values);
  }

  interface Factory {
    Spider create(Setting setting, JsonNode schema);
  }
}
