package com.github.sun.spider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.IO;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.spider.spi.BasicSpider;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

public class SpiderTest {
  @Test
  public void test_pic_mmmjpg() {
    Spider spider = newSpider("pic_mmmjpg");
    spider.start();
    progress(spider);
  }

  @Test
  public void test_pic_meitulu() {
    Spider spider = newSpider("pic_meitulu");
    spider.start();
    progress(spider);
  }

  @Test
  public void test_douban_mv_review() {
    Spider spider = newSpider("douban_mv_review");
    spider.start();
    progress(spider);
  }

  @Test
  public void test_douban_book_review() {
    Spider spider = newSpider("douban_book_review");
    spider.start();
    progress(spider);
  }

  private Spider newSpider(String file) {
    Spider spider = new BasicSpider();
    spider.setSetting(setting());
    spider.setSchema(schema(file));
    spider.setProcessorProvider(ProcessorImpl::new);
    return spider;
  }

  private class ProcessorImpl implements Spider.Processor {
    @Override
    public int process(String source, List<JsonNode> values, Setting setting, Consumer<Throwable> func) {
      System.out.println(JSON.serialize(values));
      return values.size();
    }
  }

  private void progress(Spider spider) {
    for (int i = 0; i < 10000; i++) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  private Setting setting() {
    Setting setting = Setting.builder()
      .parallelism(1)
      .retryCount(2)
      .retryDelays(2000)
      .enable(true)
      .build();
    setting.reCorrect();
    return setting;
  }

  private JsonNode schema(String file) {
    try (InputStream in = getClass().getResourceAsStream("/schema/" + file + ".json")) {
      String json = IO.read(in);
      return JSON.asJsonNode(json);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
