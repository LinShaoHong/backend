package com.github.sun;

import com.github.sun.foundation.rest.JerseyApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@EnableSwagger2
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SMSApp extends JerseyApplication {
  public SMSApp(ApplicationContext context) {
    super(context);
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(SMSApp.class).run(args);
  }
}