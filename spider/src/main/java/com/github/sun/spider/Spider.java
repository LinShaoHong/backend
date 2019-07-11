package com.github.sun.spider;

import java.io.IOException;

/**
 * @Author LinSH
 * @Date: 12:00 AM 2019-07-11
 */
public interface Spider {
  void climb();

  interface Crawler {

  }

  interface Fetcher {
    String fetch(String url) throws IOException;

    String fetch(String url, String method, String body) throws IOException;

    String fetch(String url, int timeout, String method, String body) throws IOException;

    String fetch(String url, int readTimeout, int connectTimeout, String method, String body) throws IOException;

    String fetch(String url, int readTimeout, int connectTimeout, String method, String body, String encoding) throws IOException;
  }
}
