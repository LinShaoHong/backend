package com.github.sun.spider.config;

import com.github.sun.foundation.mybatis.config.PersistenceConfiguration;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.mapper.MapperScannerConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
public class SpiderDatasourceConfiguration extends PersistenceConfiguration {
  private static final String ID = "spider";
  public static final String TX_MANAGER = ID + TRANSACTION_MANAGER_NAME;

  @Override
  protected String id() {
    return ID;
  }

  @Override
  protected String basePackage() {
    return "com.github.sun.spider.mapper";
  }

  @Bean(name = ID + SQL_SESSION_FACTORY_NAME)
  public SqlSessionFactoryBean sqlSessionFactoryBean(Environment env) throws Exception {
    return super.sqlSessionFactoryBean(dataSource(env));
  }

  @Bean(name = ID + DATASOURCE_NAME)
  public DataSource dataSource(Environment env) {
    return super.dataSource(env);
  }

  @Bean(name = TX_MANAGER)
  public DataSourceTransactionManager transactionManager(Environment env) {
    return super.transactionManager(dataSource(env));
  }

  @Bean(name = ID + SCANNER_NAME)
  protected MapperScannerConfigurer scannerConfigurer() {
    return super.scannerConfigurer(ID + SQL_SESSION_FACTORY_NAME);
  }
}
