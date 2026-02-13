package com.zqzqq.bootkits.web.annotation;

import com.zqzqq.bootkits.web.config.BrickWebAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用 Brick Web 管理控制台
 * 可选使用，不使用则需在 application.yml 配置
 * 
 * @author brick-bootkit
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(BrickWebAutoConfiguration.class)
public @interface EnableBrickWeb {
    
    /**
     * 是否启用 UI 界面
     * 如果为 false，则只提供 REST API
     * @return 是否启用 UI
     */
    boolean enableUI() default true;
    
    /**
     * API 前缀
     * @return API 路径前缀
     */
    String apiPrefix() default "/plugins-web/api";
    
    /**
     * 页面路径前缀
     * @return 页面路径前缀
     */
    String pagePrefix() default "/plugins-web";
}
