package com.github.sun.spider.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.spider.Spider;

/**
 * @Author LinSH
 * @Date: 9:53 AM 2019-07-11
 */
public class SpiderImpl implements Spider {
  private final JsonNode schema;
  private final Consumer consumer;

  public SpiderImpl(JsonNode schema, Consumer consumer) {
    this.schema = schema;
    this.consumer = consumer;
  }

  @Override
  public void start() {

  }

  @Override
  public void stop() {

  }

  @Override
  public boolean isRunning() {
    return false;
  }

  @Override
  public void add() {

  }

  @Override
  public void remove() {

  }

  @Override
  public Progress progress() {
    return null;
  }
}
