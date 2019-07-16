package com.github.sun.spider.spi;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.function.Consumer;

public interface Spider {
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

  interface Progress {
    /**
     * @return 并行数
     */
    int parallelism();

    /**
     * @return 采集的条数
     */
    int total();

    /**
     * @return 开始时间
     */
    String startTime();

    /**
     * @return 耗时
     */
    String usedTime();
  }

  interface Factory {
    Spider create(JsonNode schema, Consumer consumer);
  }
}
