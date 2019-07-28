package com.github.sun.console;

import com.github.sun.foundation.rest.JerseyApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@ComponentScans({
  @ComponentScan({"com.github.sun.console"}),
  @ComponentScan({"com.github.sun.spider"}),
  @ComponentScan({"com.github.sun.picture"})
})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ConsoleApp extends JerseyApplication {
  public ConsoleApp(ApplicationContext context) {
    super(context);
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(ConsoleApp.class).run(args);
  }
}
