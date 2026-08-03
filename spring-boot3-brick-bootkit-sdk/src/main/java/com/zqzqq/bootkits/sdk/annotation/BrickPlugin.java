package com.zqzqq.bootkits.sdk.annotation;

import java.lang.annotation.*;

/**
 * 插件注解
 * 标记一个类为 brick-bootkit 插件
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BrickPlugin {

    /**
     * 插件唯一标识
     * 建议使用反向域名格式，如 com.example.myplugin
     */
    String id();

    /**
     * 插件名称
     */
    String name();

    /**
     * 插件描述
     */
    String description() default "";

    /**
     * 插件作者
     */
    String author() default "";

    /**
     * 插件版本
     */
    String version() default "1.0.0";

    /**
     * 插件依赖的其他插件 ID 列表
     */
    String[] dependsOn() default {};

    /**
     * 插件需要的最小框架版本
     */
    String minFrameworkVersion() default "4.0.0";

    /**
     * 插件是否需要 Spring 环境
     */
    boolean requiresSpring() default false;
}
