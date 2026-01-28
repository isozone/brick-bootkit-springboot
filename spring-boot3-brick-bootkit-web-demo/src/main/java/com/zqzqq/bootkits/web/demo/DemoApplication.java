package com.zqzqq.bootkits.web.demo;

import com.zqzqq.bootkits.loader.launcher.SpringBootstrap;
import com.zqzqq.bootkits.loader.launcher.SpringMainBootstrap;
import com.zqzqq.bootkits.web.annotation.EnableBrickWeb;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

/**
 * Brick Web 管理控制台测试应用
 * 
 * @author brick-bootkit
 */
@SpringBootApplication
@ServletComponentScan(basePackages = {"com.zqzqq.bootkits.**"
})
@EnableBrickWeb
public class DemoApplication  implements SpringBootstrap {
    
    public static void main(String[] args) {


       // SpringMainBootstrap.launch(DemoApplication.class, args);
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

    @Override
    public void run(String[] args){
        SpringApplication application = new SpringApplicationBuilder(DemoApplication.class).build(args);
        application.run(args);
    }
}
