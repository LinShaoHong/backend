package com.github.sun.spider.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.Iterators;
import com.github.sun.foundation.boot.utility.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class BasicSpider extends AbstractSpider {
  private static final Logger log = LoggerFactory.getLogger(BasicSpider.class);

  private final String source;
  protected final Setting setting;
  private final JSON.Valuer process;
  private final ExecutorService executor;

  private int total;
  private Date startTime;
  private Date finishTime;
  private Thread monitor;
  private Producer producer;
  private AtomicInteger finished = new AtomicInteger(0);
  private AtomicBoolean isRunning = new AtomicBoolean(false);
  private ConcurrentLinkedQueue<Node> queue = new ConcurrentLinkedQueue<>();
  private CopyOnWriteArrayList<Consumer> consumers = new CopyOnWriteArrayList<>();

  public BasicSpider(Setting setting, JsonNode schema) {
    this.setting = setting;
    this.setting.reCorrect();
    JSON.Valuer valuer = JSON.newValuer(schema);
    this.source = valuer.get("source").asText();
    this.process = valuer.get("process");
    this.executor = Executors.newFixedThreadPool(setting.getPoolSize());
    this.monitor = new Thread(() -> {
      for (; ; ) {
        if (producer.interrupted() && consumers.stream().allMatch(Consumer::interrupted)) {
          break;
        } else {
          sleep(1000);
        }
      }
      if (!executor.isShutdown()) {
        executor.shutdown();
      }
      finishTime = new Date();
      isRunning.set(false);
      consumers.clear();
    });
  }

  protected abstract Processor create();

  @Override
  public String source() {
    return source;
  }

  @Override
  public void start() {
    if (isRunning.get()) {
      return;
    }
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
    producer.interrupt();
    consumers.stream().filter(c -> !c.interrupted()).forEach(Consumer::interrupt);
    executor.shutdown();
    isRunning.set(false);
    producer = null;
    consumers.clear();
    startTime = null;
    finishTime = null;
    finished.set(0);
    total = 0;
  }

  @Override
  public boolean isRunning() {
    return consumers.stream().anyMatch(c -> !c.interrupted());
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
      String baseUrl;
      Node root;
      Category category;
      Paging paging;
      try {
        baseUrl = process.get("baseUrl").asText();
        root = parse(baseUrl);
        category = parseCategory(process);
        paging = parsePaging(root, process);
      } catch (SpiderException ex) {
        log.warn(ex.getMessage());
        running.set(false);
        return;
      }
      List<String> uris = category == null ?
        Collections.singletonList(baseUrl) : categoryUrl(root, category);
      for (String uri : uris) {
        if (paging == null) {
          total = uris.size();
          if (running.get()) {
            queue.add(uri.equals(baseUrl) ? root : parse(uri));
          }
        } else {
          try {
            Paging vp = uri.equals(baseUrl) ? paging : parsePaging(parse(uri), process);
            total = vp.end - vp.start;
            total = total < 0 ? 0 : total;
            Iterators.slice(vp.start, vp.end).forEach(page -> {
              String url = pagingUrl(uri, page, vp);
              Node node;
              node = parse(url);
              if (running.get()) {
                queue.add(node);
              }
              sleep(setting.getTaskInterval());

            });
          } catch (SpiderException ex) {
            // skip
            log.warn(ex.getMessage());
          }
        }
      }
      running.set(false);
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
      running.set(true);
      Processor processor = create();
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
            processor.process(nodes);
            finished.addAndGet(nodes.size());
            sleep(setting.getTaskInterval());
          } catch (SpiderException ex) {
            // skip
            log.warn(ex.getMessage());
          } catch (Throwable ex) {
            log.error("Error process source=" + source, ex);
            running.set(false);
            break;
          }
        }
      }
    }
  }

  private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  @Override
  public Progress progress() {
    return Progress.builder()
      .source(source)
      .parallelism(((Long) consumers.stream().filter(c -> !c.interrupted()).count()).intValue())
      .total(total)
      .isRunning(isRunning())
      .finished(finished.get())
      .startTime(startTime == null ? "" : formatter.format(startTime))
      .endTime(finishTime == null ? "" : formatter.format(finishTime))
      .usedTime(startTime == null ? "" : formatTime(System.currentTimeMillis() - startTime.getTime()))
      .build();
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  private String formatTime(long millis) {
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
}
