package com.github.sun.spider;

import com.github.sun.foundation.boot.utility.JSON;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

/**
 * @Author LinSH
 * @Date: 12:35 PM 2019-07-11
 */
@SpringBootApplication
@ComponentScan({"com.github.sun"})
public class App {
  public static void main(String[] args) {
    JSON.asJsonNode("");
    new SpringApplicationBuilder(App.class).run(args);
  }
}
