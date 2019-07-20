package com.github.sun.spider.spi;

class SpiderException extends RuntimeException {
  SpiderException(String message) {
    super(message);
  }

  SpiderException(String message, Throwable cause) {
    super(message, cause);
  }
}
