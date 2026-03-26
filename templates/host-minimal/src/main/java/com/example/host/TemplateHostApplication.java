package com.example.host;

import com.zqzqq.bootkits.web.annotation.EnableBrickWeb;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableBrickWeb
public class TemplateHostApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateHostApplication.class, args);
    }
}
