package com.github.sun.spider.spi;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.SSL;
import com.github.sun.spider.Fetcher;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class JSoupFetcher implements Fetcher {
  private static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
  private static final String ACCEPT_CHARSET = "GB2312,utf-8;q=0.7,*;q=0.7";
  private static final String ENCODING = "gzip,deflate,sdch";
  private static final String LANGUAGE = "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3";
  private static final String CONNECTION = "keep-alive";
  private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
  private static final String USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36";

  @Override
  public String fetch(String uri, int timeout, String method, JsonNode body, String charset) throws IOException {
    String host = new URL(uri).getHost();
    Connection connection = Jsoup.connect(uri)
      .header("Accept", ACCEPT)
      .header("Accept-Charset", ACCEPT_CHARSET)
      .header("Accept-Encoding", ENCODING)
      .header("Accept-Language", LANGUAGE)
      .header("Connection", CONNECTION)
      .header("Content-Type", CONTENT_TYPE + ";" + charset)
      .header("Referer", uri)
      .header("Host", host)
      .userAgent(USER_AGENT)
      .timeout(timeout)
      .sslSocketFactory(SSL.getContext().getSocketFactory())
      .ignoreContentType(true);
    switch (method.toUpperCase()) {
      case "GET":
        return connection.get().body().html();
      case "POST":
        Map<String, String> data = body != null ?
          JSON.deserializeAsMap(body, String.class) : Collections.emptyMap();
        return connection.data(data).post().body().html();
      default:
        throw new SpiderException("Unknown method: " + method);
    }
  }
}
