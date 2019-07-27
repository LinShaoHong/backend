package com.github.sun.spider.config;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Transactional(value = SpiderDatasourceConfiguration.TX_MANAGER, rollbackFor = Exception.class)
public @interface SpiderTransactional {
}
