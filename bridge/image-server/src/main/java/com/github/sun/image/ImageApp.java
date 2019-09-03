package com.github.sun.image;

import com.github.sun.foundation.rest.JerseyApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;

@ComponentScans({
  @ComponentScan({"com.github.sun.layout"})
})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ImageApp extends JerseyApplication {
  public ImageApp(ApplicationContext context) {
    super(context);
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(ImageApp.class).run(args);
  }
}
