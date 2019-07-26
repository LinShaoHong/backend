package com.github.sun.admin;

import com.github.sun.foundation.rest.JerseyApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class AdminApp extends JerseyApplication {
  public AdminApp(ApplicationContext context) {
    super(context);
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(AdminApp.class).run(args);
  }
}
