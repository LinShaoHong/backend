package com.github.sun.whispered;

import com.github.sun.foundation.rest.JerseyApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@ComponentScans({
  @ComponentScan({"com.github.sun.image"}),
  @ComponentScan({"com.github.sun.whispered"})
})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class WhisperedApp extends JerseyApplication {
  public WhisperedApp(ApplicationContext context) {
    super(context);
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(WhisperedApp.class).run(args);
  }
}
