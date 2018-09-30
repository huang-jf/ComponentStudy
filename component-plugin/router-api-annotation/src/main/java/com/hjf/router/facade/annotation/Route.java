package com.hjf.router.facade.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface Route {
    /**
     * path of one route
     */
    String path();

    /**
     * group
     */
    String group() default "";

    /**
     * description
     */
    String desc() default "";

}
