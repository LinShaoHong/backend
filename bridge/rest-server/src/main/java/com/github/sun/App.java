package com.github.sun;

import com.github.sun.foundation.rest.JerseyApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class App extends JerseyApplication {
  public App(ApplicationContext context) {
    super(context);
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(App.class).run(args);
  }
}
