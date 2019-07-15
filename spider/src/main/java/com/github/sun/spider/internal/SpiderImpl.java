package com.github.sun.spider.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.spider.Spider;

import java.util.function.Consumer;

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
