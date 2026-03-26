package com.example.hostbroken;

import com.zqzqq.bootkits.web.annotation.EnableBrickWeb;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableBrickWeb
@Import(TemplateBrokenMainPackageConfiguration.class)
public class TemplateBrokenMainPackageApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateBrokenMainPackageApplication.class, args);
    }
}
