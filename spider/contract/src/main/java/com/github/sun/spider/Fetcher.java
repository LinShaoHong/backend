package com.github.sun.spider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.sun.foundation.boot.utility.JSON;
import com.github.sun.foundation.boot.utility.SSL;
import lombok.experimental.UtilityClass;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class Fetcher {
    private final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8";
    private final String ACCEPT_CHARSET = "GB2312,utf-8;q=0.7,*;q=0.7";
    private final String ENCODING = "gzip,deflate,sdch";
    private final String LANGUAGE = "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3";
    private final String CONNECTION = "keep-alive";
    private final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.87 Safari/537.36";

    private String fetch(String uri, int timeout, String method, JsonNode body, String charset, Map<String, String> headers) throws IOException {
        headers = headers == null ? new HashMap<>() : headers;
        String host = new URL(uri).getHost();
        Connection connection = Jsoup.connect(uri)
                .header("Accept", ACCEPT)
                .header("Accept-Charset", ACCEPT_CHARSET)
                .header("Accept-Encoding", ENCODING)
                .header("Accept-Language", LANGUAGE)
                .header("Connection", CONNECTION)
                .header("Content-Type", CONTENT_TYPE + ";" + charset)
                .header("Referer", uri)
                .header("Host", host);
        for (Map.Entry<String, String> e : headers.entrySet()) {
            connection = connection.header(e.getKey(), e.getValue());
        }
        connection = connection.userAgent(USER_AGENT)
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

    public Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String uri;
        private int timeout;
        private String method = "GET";
        private JsonNode body;
        private String charset = StandardCharsets.UTF_8.name();
        private Map<String, String> headers;

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder timeout(int timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder body(JsonNode body) {
            this.body = body;
            return this;
        }

        public Builder charset(String charset) {
            this.charset = charset;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public String fetch() throws IOException {
            return Fetcher.fetch(uri, timeout, method, body, charset, headers);
        }
    }
}
