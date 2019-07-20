package com.github.sun.spider;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;

public interface Fetcher {
  default String fetch(String uri) {
    return fetch(uri, "GET", null);
  }

  default String fetch(String uri, String method, JsonNode body) {
    return fetch(uri, 0, method, body);
  }

  default String fetch(String uri, int timeout, String method, JsonNode body) {
    return fetch(uri, timeout, method, body, StandardCharsets.UTF_8.name());
  }

  String fetch(String uri, int timeout, String method, JsonNode body, String charset);
}
