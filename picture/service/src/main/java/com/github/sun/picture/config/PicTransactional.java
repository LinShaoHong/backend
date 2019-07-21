package com.github.sun.picture.config;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Transactional(value = PictureDatasourceConfiguration.TX_MANAGER, rollbackFor = Exception.class)
public @interface PicTransactional {
}
