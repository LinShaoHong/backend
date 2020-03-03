package com.github.sun.conf;

import com.github.sun.foundation.mybatis.config.PersistenceConfiguration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class DataSourceConfiguration extends PersistenceConfiguration {
  @Primary
  @Bean(name = SQL_SESSION_FACTORY_NAME)
  public SqlSessionFactoryBean sqlSessionFactoryBean(Environment env) {
    return super.sqlSessionFactoryBean(dataSource(env));
  }

  @Primary
  @Bean(name = DATASOURCE_NAME)
  public DataSource dataSource(Environment env) {
    return super.dataSource(env);
  }

  @Primary
  @Bean(name = TRANSACTION_MANAGER_NAME)
  public DataSourceTransactionManager transactionManager(Environment env) {
    return super.transactionManager(dataSource(env));
  }

  @Primary
  @Bean(name = SCANNER_NAME)
  protected MapperScannerConfigurer scannerConfigurer() {
    return super.scannerConfigurer(SQL_SESSION_FACTORY_NAME);
  }
}
