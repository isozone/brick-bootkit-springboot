package com.zqzqq.bootkits.sdk.annotation;

import java.lang.annotation.*;

/**
 * 服务引用注解
 * 自动注入其他插件提供的服务
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BrickServiceReference {

    /**
     * 服务接口
     */
    Class<?> value();

    /**
     * 服务版本范围，如 "[1.0,2.0)"
     */
    String version() default "";

    /**
     * 是否可选（找不到服务时不报错）
     */
    boolean optional() default false;
}
