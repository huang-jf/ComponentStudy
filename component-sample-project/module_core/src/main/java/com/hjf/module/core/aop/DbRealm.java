package com.hjf.module.core.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据库存库注解
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface DbRealm {
}
