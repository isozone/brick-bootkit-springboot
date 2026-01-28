package com.zqzqq.bootkits.web.demo;

import com.zqzqq.bootkits.web.annotation.EnableBrickWeb;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Brick Web 管理控制台测试应用
 * 
 * @author brick-bootkit
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.zqzqq.bootkits.web",
    "com.zqzqq.bootkits.web.demo"
})
@EnableBrickWeb
public class DemoApplication {
    
    public static void main(String[] args) {
        try {
            ApplicationContext context = SpringApplication.run(DemoApplication.class, args);
            System.out.println("========================================");
            System.out.println("应用启动成功！");
            System.out.println("访问地址: http://localhost:8080/brick-web/index.html");
            System.out.println("========================================");
        } catch (Throwable e) {
            System.err.println("应用启动失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
