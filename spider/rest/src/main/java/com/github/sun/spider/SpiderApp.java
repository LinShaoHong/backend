package com.github.sun.spider;

import com.github.sun.foundation.rest.JerseyApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"com.github.sun.spider", "com.github.sun.picture"})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SpiderApp extends JerseyApplication<SpiderApp> {
  public SpiderApp(ApplicationContext context) {
    super(context);
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(SpiderApp.class).run(args);
  }
}
