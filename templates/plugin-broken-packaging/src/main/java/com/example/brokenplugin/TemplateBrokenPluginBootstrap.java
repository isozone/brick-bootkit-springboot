package com.example.brokenplugin;

import com.zqzqq.bootkits.bootstrap.SpringPluginBootstrap;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TemplateBrokenPluginBootstrap extends SpringPluginBootstrap {

    public static void main(String[] args) {
        new TemplateBrokenPluginBootstrap().run(args);
    }
}
