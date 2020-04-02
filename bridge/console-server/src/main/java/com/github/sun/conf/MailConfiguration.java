package com.github.sun.conf;

import com.github.sun.common.EmailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Slf4j
@Configuration
public class MailConfiguration {
  @Bean(name = "qmailProps")
  @ConfigurationProperties("qmail")
  public Properties properties() {
    return new Properties();
  }

  @Bean(name = "qmail")
  public EmailSender gmailSender(@Qualifier("qmailProps") Properties props) {
    return new EmailSender(props);
  }
}
