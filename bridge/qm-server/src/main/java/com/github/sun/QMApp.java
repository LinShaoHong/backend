package com.github.sun;

import com.github.sun.foundation.rest.JerseyApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;


@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class QMApp extends JerseyApplication {
    public QMApp(ApplicationContext context) {
        super(context);
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(QMApp.class).run(args);
    }
}
