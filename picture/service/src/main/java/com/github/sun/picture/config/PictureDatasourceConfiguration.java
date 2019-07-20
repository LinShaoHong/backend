package com.github.sun.picture.config;

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
import java.sql.SQLException;

@Configuration
@EnableTransactionManagement
public class PictureDatasourceConfiguration extends PersistenceConfiguration {
  private static final String ID = "picture";

  @Override
  protected String id() {
    return ID;
  }

  @Override
  protected String basePackage() {
    return "com.github.sun.picture.mapper";
  }

  @Primary
  @Bean(name = ID + SQL_SESSION_FACTORY_NAME)
  public SqlSessionFactoryBean sqlSessionFactoryBean(DataSource dataSource) throws Exception {
    return super.sqlSessionFactoryBean(dataSource);
  }

  @Primary
  @Bean(name = ID + DATASOURCE_NAME)
  public DataSource dataSource(Environment env) throws SQLException {
    return super.dataSource(env);
  }

  @Primary
  @Bean(name = ID + TRANSACTION_MANAGER_NAME)
  public DataSourceTransactionManager transactionManager(DataSource dataSource) {
    return super.transactionManager(dataSource);
  }

  @Primary
  @Bean(name = ID + SCANNER_NAME)
  protected MapperScannerConfigurer scannerConfigurer() {
    return super.scannerConfigurer(ID + SQL_SESSION_FACTORY_NAME);
  }
}
