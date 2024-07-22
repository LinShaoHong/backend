package com.github.sun.conf;

import com.github.sun.foundation.mybatis.config.PersistenceConfiguration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class DataSourceConfiguration extends PersistenceConfiguration {
  @Primary
  @Bean(name = SQL_SESSION_FACTORY_NAME)
  public SqlSessionFactoryBean sqlSessionFactoryBean(@Qualifier(DATASOURCE_NAME) DataSource dataSource) {
    return super.sqlSessionFactoryBean(dataSource);
  }

  @Primary
  @Bean(name = DATASOURCE_NAME)
  public DataSource dataSource(Environment env) {
    return super.dataSource(env);
  }

  @Primary
  @Bean(name = TRANSACTION_MANAGER_NAME)
  public PlatformTransactionManager transactionManager(@Qualifier(DATASOURCE_NAME) DataSource dataSource) {
    return super.transactionManager(dataSource);
  }

  @Primary
  @Bean(name = SCANNER_NAME)
  protected MapperScannerConfigurer scannerConfigurer() {
    return super.scannerConfigurer(SQL_SESSION_FACTORY_NAME);
  }
}
