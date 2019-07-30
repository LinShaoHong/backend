package com.github.sun.image.config;

import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Transactional(value = ImageDatasourceConfiguration.TX_MANAGER, rollbackFor = Exception.class)
public @interface ImgTransactional {
}
