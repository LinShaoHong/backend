package com.github.sun.spider;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.function.Supplier;

public interface Spider {
  void clear();

  /**
   * 来源
   */
  String source();

  /**
   * 设置运行参数
   */
  void setSetting(Setting setting);

  /**
   * @return 获取运行参数
   */
  Setting getSetting();

  /**
   * 设置采集规则
   */
  void setSchema(JsonNode schema);

  /**
   * 设置结果处理器
   */
  void setProcessorProvider(Supplier<Processor> provider);

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
   * 错误异常
   */
  List<Throwable> errors();

  void addListener(Listener listener);

  interface Listener {
    void onUpdate(Setting setting);
  }

  /**
   * @return 当前采集进度
   */
  Progress progress();

  /**
   * @return 最近采集情况
   */
  List<Progress> latestProgress();

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

    /**
     * 错误异常
     */
    private List<Throwable> errors;
  }

  interface Processor {
    String SUFFIX = ":SPIDER:PROCESSOR";

    void process(String source, List<JsonNode> values, Setting setting);
  }

  interface Factory {
    Spider create(Setting setting, JsonNode schema, Processor processor);
  }
}
