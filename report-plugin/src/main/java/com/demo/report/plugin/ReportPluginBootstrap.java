package com.demo.report.plugin;

import com.zqzqq.bootkits.bootstrap.SpringPluginBootstrap;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReportPluginBootstrap extends SpringPluginBootstrap {

    public static void main(String[] args) {
        new ReportPluginBootstrap().run(args);
    }
}
