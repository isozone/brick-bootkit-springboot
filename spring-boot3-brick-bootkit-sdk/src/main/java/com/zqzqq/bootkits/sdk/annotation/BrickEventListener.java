package com.zqzqq.bootkits.sdk.annotation;

import java.lang.annotation.*;

/**
 * 事件监听器注解
 * 标记方法为事件监听器
 * 
 * @author brick-bootkit
 * @since 4.2.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface BrickEventListener {

    /**
     * 监听的事件类型
     */
    com.zqzqq.bootkits.core.eventbus.PluginEvent.EventType[] value();

    /**
     * 监听器优先级（数值越小优先级越高）
     */
    int priority() default 0;

    /**
     * 是否异步处理
     */
    boolean async() default false;
}
