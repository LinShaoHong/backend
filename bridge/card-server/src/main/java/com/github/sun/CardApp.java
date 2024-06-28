package com.github.sun;

import com.github.sun.foundation.rest.JerseyApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.stream.Collectors;

@EnableDiscoveryClient
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class CardApp extends JerseyApplication {
  public CardApp(ApplicationContext context) {
    super(context);
  }

  public static void main(String[] args) {
//    new SpringApplicationBuilder(CardApp.class).run(args);
    String s = "";
    System.out.println(Arrays.stream(s.split("\n")).map(v -> "\"" + v.substring(v.indexOf(". ") + 2) + "\"").collect(Collectors.joining(",")));
  }
}