package com.github.sun.spider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.IO;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.spider.spi.BasicSpider;
import com.github.sun.spider.spi.Setting;
import com.github.sun.spider.spi.Spider;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

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
  public void test_mv_review_douban() {
    Spider spider = newSpider("mv_review_douban");
    spider.start();
    progress(spider);
  }



  private Spider newSpider(String file) {
    return new SpiderImpl(setting(), schema(file));
  }

  private static class SpiderImpl extends BasicSpider {
    public SpiderImpl(Setting setting, JsonNode schema) {
      super(setting, schema);
    }

    @Override
    protected Processor create() {
      return new ProcessorImpl();
    }

    private class ProcessorImpl implements Processor {
      @Override
      public void process(List<JsonNode> values) {
        System.out.println(JSON.serialize(values));
      }
    }
  }

  private void progress(Spider spider) {
    for (int i = 0; i < 100; i++) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
    }
  }

  private Setting setting() {
    return Setting.builder()
      .parallelism(2)
      .enable(true)
      .build();
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
