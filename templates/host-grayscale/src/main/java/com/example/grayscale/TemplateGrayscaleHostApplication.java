package com.example.grayscale;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 灰度发布宿主模板。
 * <p>
 * 启动后：
 * 1. 插件升级以灰度模式（rolloutMode=gray）执行
 * 2. 每次升级会依次运行 {@link com.example.grayscale.rollout.SmokeRolloutProbe} 探针
 * 3. 任一探针未通过则升级失败并自动回滚到备份版本
 * <p>
 * 验证：Web 控制台「灰度发布」页面可查看配置与探针，并可对指定插件模拟灰度决策。
 */
@SpringBootApplication
public class TemplateGrayscaleHostApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateGrayscaleHostApplication.class, args);
    }
}
