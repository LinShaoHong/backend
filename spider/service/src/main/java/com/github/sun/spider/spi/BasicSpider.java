package com.github.sun.spider.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.Dates;
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
  private Listener listener;

  private int total;
  private Date startTime;
  private Date finishTime;
  private Thread monitor;
  private ExecutorService executor;
  private volatile Checkpoint checkpoint;
  private volatile NodeHolder latestConsumed;
  private volatile Producer producer;
  private volatile CheckpointHandler checkpointHandler;
  private final List<Throwable> errors = new ArrayList<>();
  private final List<Progress> latest = new ArrayList<>();
  private final List<Consumer> consumers = new ArrayList<>();
  private final AtomicInteger finished = new AtomicInteger(0);
  private final AtomicBoolean isRunning = new AtomicBoolean(false);
  private final ConcurrentLinkedQueue<NodeHolder> queue = new ConcurrentLinkedQueue<>();

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
    this.latestConsumed = null;
    this.total = 0;
    this.finished.set(0);
    this.queue.clear();
    this.errors.clear();
    this.consumers.clear();
    this.monitor = new Thread(() -> {
      for (; ; ) {
        if (producer.interrupted() && consumers.stream().allMatch(Consumer::interrupted)) {
          break;
        } else if (setting.getExecuteTime() > 0
          && (System.currentTimeMillis() - startTime.getTime() >= setting.getExecuteTime())) {
          break;
        } else {
          sleep(setting.getMonitorInterval());
        }
      }
      if (isRunning()) {
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
    Setting old = this.setting == null ? null : this.setting.clone();
    this.setting = setting;
    this.setting.reCorrect();
    addListener(new ListenerImpl(old));
    this.listener.onUpdate(this.setting);
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
  public void addListener(Listener listener) {
    this.listener = listener;
  }

  @Override
  public Checkpoint checkPointing() {
    return latestConsumed == null ? null : latestConsumed.checkpoint;
  }

  @Override
  public Checkpoint checkpoint() {
    return this.checkpoint;
  }

  @Override
  public void setCheckpoint(Checkpoint checkpoint) {
    this.checkpoint = checkpoint;
  }

  @Override
  public void setCheckpointHandler(CheckpointHandler handler) {
    this.checkpointHandler = handler;
  }

  private class ListenerImpl implements Listener {
    private final Setting old;

    private ListenerImpl(Setting old) {
      this.old = old;
    }

    @Override
    public void onUpdate(Setting setting) {
      if (old != null && isRunning()) {
        int added = setting.getParallelism() - old.getParallelism();
        if (added != 0) {
          boolean add = added > 0;
          Iterators.slice(Math.abs(added)).forEach(v -> {
            if (add) {
              add();
            } else {
              remove();
            }
          });
        }
      }
    }
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
      finishTime = new Date();
      pushProgress();
      checkpoint = latestConsumed == null ? null : latestConsumed.checkpoint;
      if (checkpointHandler != null && checkpoint != null) {
        checkpointHandler.apply(checkpoint);
      }
      isRunning.compareAndSet(true, false);
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

  private synchronized void pushError(Throwable ex) {
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
    private final AtomicBoolean running = new AtomicBoolean(false);

    private void interrupt() {
      running.compareAndSet(true, false);
    }

    private boolean interrupted() {
      return !running.get();
    }

    @Override
    public void run() {
      running.set(true);
      try {
        String type = process.get("type").asText();
        String xpath = process.get("xpath").asText();
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
            try {
              List<String> uris = category == null ?
                Collections.singletonList(baseUrl) : categoryUrl(root, category);
              int index = 0;
              boolean canSkipped = true;
              for (String uri : uris) {
                if (interrupted()) {
                  break;
                }
                if (category != null && canSkipped) {
                  if (stop(uri)) {
                    canSkipped = false;
                  }
                  continue;
                }
                if (paging == null) {
                  Node node = uri.equals(baseUrl) ? root : get(req.set(uri));
                  total += getEntityNum(type, xpath, node, process);
                  queue.add(NodeHolder.from(index++, node, category == null ? null : uri));
                } else {
                  Paging vp = uri.equals(baseUrl) ? paging : parsePaging(get(req.set(uri)), process);
                  for (int page = vp.start; page < vp.end; page++) {
                    if (interrupted()) {
                      break;
                    }
                    if (skip(page)) {
                      continue;
                    }
                    String url = pagingUrl(uri, page, vp);
                    Node node;
                    node = get(req.set(url));
                    total += getEntityNum(type, xpath, node, process);
                    queue.add(NodeHolder.from(index++, node, category == null ? null : uri, page));
                    sleep(setting.getTaskInterval());
                  }
                }
              }
            } catch (SpiderException ex) {
              pushError(ex);
              // skip
              log.warn(ex.getMessage(), ex.getCause());
            }
            break;
          case "POST":
            try {
              int index = 0;
              List<String> uris = category == null ?
                Collections.singletonList(baseUrl) : categoryUrl(get(baseUrl), category);
              boolean canSkipped = true;
              for (String uri : uris) {
                if (interrupted()) {
                  break;
                }
                if (category != null && canSkipped) {
                  if (stop(uri)) {
                    canSkipped = false;
                  }
                  continue;
                }
                for (Request r : parseRequest(uri, process)) {
                  if (interrupted()) {
                    break;
                  }
                  Node node = get(r);
                  total += getEntityNum(type, xpath, node, process);
                  queue.add(NodeHolder.from(index, node, category == null ? null : uri, null));
                  sleep(setting.getTaskInterval());
                }
              }
            } catch (SpiderException ex) {
              pushError(ex);
              // skip
              log.warn(ex.getMessage(), ex.getCause());
            }
            break;
          default:
            throw new SpiderException("Unknown method: " + method);
        }
      } finally {
        running.set(false);
      }
    }
  }

  private boolean skip(int pageNum) {
    if (this.checkpoint != null) {
      Integer page = this.checkpoint.getPageNum();
      return page != null && pageNum <= page;
    }
    return false;
  }

  private boolean stop(String uri) {
    if (this.checkpoint != null) {
      String categoryUrl = this.checkpoint.getCategoryUrl();
      return uri.equals(categoryUrl);
    }
    return true;
  }

  private class Consumer implements Runnable {
    private final AtomicBoolean running = new AtomicBoolean(false);

    private void interrupt() {
      running.compareAndSet(true, false);
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
        NodeHolder holder = queue.poll();
        if (holder == null && producer.interrupted()) {
          running.set(false);
          break;
        }
        // break out of this loop if canceled
        if (Thread.currentThread().isInterrupted()) {
          running.set(false);
          break;
        }
        if (holder != null) {
          try {
            JsonNode value = crawl(holder.node, process);
            List<JsonNode> nodes = Iterators.asList(value);
            if (!nodes.isEmpty()) {
              int count = processor.process(source, nodes, setting, BasicSpider.this::pushError);
              finished.addAndGet(count);
            }
            synchronized (BasicSpider.this) {
              if (BasicSpider.this.latestConsumed == null ||
                BasicSpider.this.latestConsumed.index < holder.index) {
                BasicSpider.this.latestConsumed = holder;
              }
            }
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

  private static class NodeHolder {
    private final int index;
    private final Node node;
    private final Checkpoint checkpoint;

    private NodeHolder(int index, Node node, Checkpoint checkpoint) {
      this.index = index;
      this.node = node;
      this.checkpoint = checkpoint;
    }

    private static NodeHolder from(int index, Node node, String category) {
      return from(index, node, category, null);
    }

    private static NodeHolder from(int index, Node node, String category, Integer pageNum) {
      return new NodeHolder(index, node, new Checkpoint(category, pageNum));
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
      .usedTime(startTime == null ? "" : Dates.formatTime((finishTime == null ? System.currentTimeMillis() : finishTime.getTime()) - startTime.getTime()))
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

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
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
