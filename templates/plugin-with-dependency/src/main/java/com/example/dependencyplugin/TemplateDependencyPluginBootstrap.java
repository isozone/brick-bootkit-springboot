package com.example.dependencyplugin;

import com.zqzqq.bootkits.bootstrap.SpringPluginBootstrap;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TemplateDependencyPluginBootstrap extends SpringPluginBootstrap {

    public static void main(String[] args) {
        new TemplateDependencyPluginBootstrap().run(args);
    }
}
