package com.zqzqq.bootkits.web.demo;

import com.zqzqq.bootkits.loader.launcher.SpringBootstrap;
import com.zqzqq.bootkits.loader.launcher.SpringMainBootstrap;
import com.zqzqq.bootkits.web.annotation.EnableBrickWeb;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;


/**
 * Brick Web 管理控制台测试应用
 *
 * @author brick-bootkit
 */
@SpringBootApplication(
    scanBasePackages = {"com.zqzqq.bootkits.**","com.zqzqq.bootkits.web.demo.**"},
    exclude = {
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
    }
)
@EnableBrickWeb
@EnableScheduling
public class DemoApplication  implements SpringBootstrap {

    public static void main(String[] args) {
        SpringMainBootstrap.launch(DemoApplication.class, args);
    }

    @Override
    public void run(String[] args){
        SpringApplication application = new SpringApplicationBuilder(DemoApplication.class).build(args);
        application.run(args);
    }
}
