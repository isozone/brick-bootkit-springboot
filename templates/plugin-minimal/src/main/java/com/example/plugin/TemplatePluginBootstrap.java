package com.example.plugin;

import com.zqzqq.bootkits.bootstrap.SpringPluginBootstrap;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TemplatePluginBootstrap extends SpringPluginBootstrap {

    public static void main(String[] args) {
        new TemplatePluginBootstrap().run(args);
    }
}
