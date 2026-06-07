package com.zqzqq.bootkits.sdk.annotation;

import java.lang.annotation.*;

/**
 * 服务提供者注解
 * 标记一个类为插件服务，可被其他插件调用
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BrickService {

    /**
     * 服务接口
     */
    Class<?> value();

    /**
     * 服务版本
     */
    String version() default "1.0.0";

    /**
     * 服务描述
     */
    String description() default "";
}
