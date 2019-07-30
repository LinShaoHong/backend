package com.github.sun.spider.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.spider.Setting;
import com.github.sun.spider.Spider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Slf4j
public class BasicSpider extends AbstractSpider {
  public static final int MAX_ERRORS_SIZE = 30;
  private static final int MAX_LATEST_SIZE = 7;

  private Setting setting;
  private String source;
  private JsonNode schema;
  private JSON.Valuer process;
  private Supplier<Processor> provider;

  private int total;
  private Date nextTime;
  private Date startTime;
  private Date finishTime;
  private Thread monitor;
  private Producer producer;
  private ExecutorService executor;
  private AtomicInteger finished = new AtomicInteger(0);
  private List<Throwable> errors = new CopyOnWriteArrayList<>();
  private List<Progress> latest = new CopyOnWriteArrayList<>();
  private ConcurrentLinkedQueue<Node> queue = new ConcurrentLinkedQueue<>();
  private CopyOnWriteArrayList<Consumer> consumers = new CopyOnWriteArrayList<>();
  private AtomicBoolean isRunning = new AtomicBoolean(false);
  private AtomicBoolean isStopped = new AtomicBoolean(false);

  private void init() {
    if (setting == null) {
      throw new IllegalArgumentException("Require setting");
    }
    if (schema == null) {
      throw new IllegalArgumentException("Require schema");
    }
    if (provider == null) {
      throw new IllegalArgumentException("Require processor provider");
    }
    JSON.Valuer valuer = JSON.newValuer(schema);
    this.source = valuer.get("source").asText();
    this.process = valuer.get("process");
    this.executor = Executors.newFixedThreadPool(setting.getPoolSize());
    this.startTime = null;
    this.finishTime = null;
    this.total = 0;
    this.isStopped.set(false);
    this.finished.set(0);
    this.queue.clear();
    this.errors.clear();
    this.consumers.clear();
    this.monitor = new Thread(() -> {
      for (; ; ) {
        if (producer.interrupted() && consumers.stream().allMatch(Consumer::interrupted)) {
          break;
        } else if (System.currentTimeMillis() - startTime.getTime() >= setting.getExecuteTime()) {
          break;
        } else {
          sleep(setting.getMonitorInterval());
        }
      }
      if (!isStopped.get()) {
        stop();
      }
    });
  }

  @Override
  public void clear() {
    this.setting = null;
    this.schema = null;
  }

  @Override
  public void setSetting(Setting setting) {
    this.setting = setting;
    this.setting.reCorrect();
  }

  @Override
  public Setting getSetting() {
    return setting;
  }

  @Override
  public void setSchema(JsonNode schema) {
    this.schema = schema;
  }

  @Override
  public void setProcessorProvider(Supplier<Processor> provider) {
    this.provider = provider;
  }

  @Override
  public String source() {
    return source;
  }

  @Override
  public List<Throwable> errors() {
    return errors;
  }

  @Override
  public void start() {
    if (isRunning.get()) {
      return;
    }
    init();
    startTime = new Date();
    isRunning.set(true);
    finished.set(0);
    total = 0;
    producer = new Producer();
    executor.submit(producer);
    int parallelism = setting.getParallelism();
    if (parallelism > 0) {
      Iterators.slice(parallelism).forEach(i -> add());
    }
    monitor.start();
  }

  @Override
  public void stop() {
    if (isRunning.get()) {
      producer.interrupt();
      consumers.stream().filter(c -> !c.interrupted()).forEach(Consumer::interrupt);
      consumers.clear();
      executor.shutdown();
      isRunning.set(false);
      isStopped.set(true);
      finishTime = new Date();
      pushProgress();
    }
  }

  @Override
  public boolean isRunning() {
    return isRunning.get();
  }

  @Override
  public void add() {
    Consumer consumer = new Consumer();
    consumers.add(consumer);
    executor.submit(consumer);
  }

  @Override
  public void remove() {
    Consumer consumer = consumers.stream()
      .filter(c -> !c.interrupted())
      .findAny().orElse(null);
    if (consumer != null) {
      consumer.interrupt();
      consumers.remove(consumer);
    }
  }

  private void pushError(Throwable ex) {
    if (errors.size() >= MAX_ERRORS_SIZE) {
      errors.remove(0);
    }
    errors.add(ex);
  }

  private void pushProgress() {
    if (latest.size() >= MAX_LATEST_SIZE) {
      latest.remove(0);
    }
    latest.add(progress());
  }

  private class Producer implements Runnable {
    private AtomicBoolean running = new AtomicBoolean(false);

    private void interrupt() {
      running.set(false);
    }

    private boolean interrupted() {
      return !running.get();
    }

    @Override
    public void run() {
      running.set(true);
      try {
        String baseUrl = process.get("baseUrl").asText();
        String method = process.get("method").asText("GET");
        Category category = parseCategory(process);
        switch (method.toUpperCase()) {
          case "GET":
            Request req;
            Node root;
            Paging paging;
            try {
              req = parseRequest(baseUrl, process).get(0);
              root = get(req);
              paging = parsePaging(root, process);
            } catch (SpiderException ex) {
              pushError(ex);
              log.warn(ex.getMessage(), ex.getCause());
              return;
            }
            List<String> uris = category == null ?
              Collections.singletonList(baseUrl) : categoryUrl(root, category);
            for (String uri : uris) {
              if (paging == null) {
                total = uris.size();
                if (running.get()) {
                  queue.add(uri.equals(baseUrl) ? root : get(req.set(uri)));
                }
              } else {
                try {
                  Paging vp = uri.equals(baseUrl) ? paging : parsePaging(get(req.set(uri)), process);
                  total = vp.end - vp.start;
                  total = total < 0 ? 0 : total;
                  total = total * uris.size();
                  Iterators.slice(vp.start, vp.end).forEach(page -> {
                    String url = pagingUrl(uri, page, vp);
                    Node node;
                    node = get(req.set(url));
                    if (running.get()) {
                      queue.add(node);
                    }
                    sleep(setting.getTaskInterval());
                  });
                } catch (SpiderException ex) {
                  pushError(ex);
                  // skip
                  log.warn(ex.getMessage(), ex.getCause());
                }
              }
            }
            break;
          case "POST":
            uris = category == null ?
              Collections.singletonList(baseUrl) : categoryUrl(get(baseUrl), category);
            uris.forEach(uri -> parseRequest(uri, process).forEach(r -> {
              try {
                if (running.get()) {
                  queue.add(get(r));
                }
                sleep(setting.getTaskInterval());
              } catch (SpiderException ex) {
                pushError(ex);
                // skip
                log.warn(ex.getMessage(), ex.getCause());
              }
            }));
            break;
          default:
            throw new SpiderException("Unknown method: " + method);
        }
      } finally {
        running.set(false);
      }
    }
  }

  private class Consumer implements Runnable {
    private AtomicBoolean running = new AtomicBoolean(false);

    private void interrupt() {
      running.set(false);
    }

    private boolean interrupted() {
      return !running.get();
    }

    @Override
    public void run() {
      Processor processor;
      try {
        processor = provider.get();
      } catch (Throwable ex) {
        pushError(ex);
        log.error("Error create processor.", ex);
        return;
      }
      running.set(true);
      while (running.get()) {
        Node node = queue.poll();
        if (node == null && producer.interrupted()) {
          running.set(false);
          break;
        }
        // break out of this loop if canceled
        if (Thread.currentThread().isInterrupted()) {
          running.set(false);
          break;
        }
        if (node != null) {
          try {
            JsonNode value = parse(node, process);
            List<JsonNode> nodes = Iterators.asList(value);
            if (!nodes.isEmpty()) {
              processor.process(source, nodes, setting);
            }
            finished.addAndGet(nodes.size());
          } catch (SpiderException ex) {
            // skip
            pushError(ex);
            log.warn(ex.getMessage(), ex.getCause());
          } catch (Throwable ex) {
            pushError(ex);
            log.error("Error process source=" + source, ex);
            running.set(false);
            break;
          }
        }
        sleep(setting.getTaskInterval());
      }
    }
  }

  private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Override
  public Progress progress() {
    return Progress.builder()
      .source(source)
      .parallelism(((Long) consumers.stream().filter(c -> !c.interrupted()).count()).intValue())
      .total(total)
      .isRunning(isRunning())
      .finished(finished.get())
      .errors(new ArrayList<>(errors))
      .startTime(startTime == null ? "" : formatter.format(startTime))
      .endTime(finishTime == null ? "" : formatter.format(finishTime))
      .usedTime(startTime == null ? "" : formatTime((finishTime == null ? System.currentTimeMillis() : finishTime.getTime()) - startTime.getTime()))
      .build();
  }

  @Override
  public List<Progress> latestProgress() {
    if (!isRunning() && !latest.isEmpty()) {
      latest.remove(latest.size() - 1);
      latest.add(progress());
    }
    return new ArrayList<>(latest);
  }

  @Override
  public Date nextTime() {
    return nextTime;
  }

  @Override
  public Date setNextTime(Date next) {
    return this.nextTime = next;
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static String formatTime(long millis) {
    millis = millis / 1000;
    StringBuilder sb = new StringBuilder();
    long days = millis / (3600 * 24);
    if (days > 0) {
      sb.append(days).append("天");
      millis = millis % (3600 * 24);
    }
    long hours = millis / 3600;
    if (hours > 0) {
      sb.append(hours).append("时");
      millis = millis % 3600;
    }
    long minutes = millis / 60;
    if (minutes > 0) {
      sb.append(minutes).append("分");
      millis = minutes % 60;
    }
    if (millis > 0) {
      sb.append(millis).append("秒");
    }
    return sb.toString();
  }

  @Component
  public static class SpiderFactory implements Spider.Factory {
    @Override
    public Spider create(Setting setting, JsonNode schema, Spider.Processor processor) {
      BasicSpider spider = new BasicSpider();
      spider.setProcessorProvider(() -> processor);
      spider.setSchema(schema);
      spider.setSetting(setting);
      return spider;
    }
  }
}
