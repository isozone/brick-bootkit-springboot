package com.zqzqq.bootkits.web.controller.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

/**
 * 页面控制器 - 重定向到静态 HTML 页面
 * 
 * @author brick-bootkit
 */
@Controller
@RequestMapping("${brick.web.page-prefix:/brick-web}")
public class IndexController {

    /**
     * 首页/仪表盘
     */
    @GetMapping("/index")
    public RedirectView index() {
        return new RedirectView("/brick-web/index.html");
    }

    /**
     * 插件列表页
     */
    @GetMapping("/plugins")
    public RedirectView plugins() {
        return new RedirectView("/brick-web/plugins/index.html");
    }

    /**
     * 上传插件页
     */
    @GetMapping("/plugins/upload")
    public RedirectView uploadPlugin() {
        return new RedirectView("/brick-web/plugins/upload.html");
    }

    /**
     * 监控概览页
     */
    @GetMapping("/monitor")
    public RedirectView monitor() {
        return new RedirectView("/brick-web/monitor/overview.html");
    }

    /**
     * 内存监控页
     */
    @GetMapping("/monitor/memory")
    public RedirectView memory() {
        return new RedirectView("/brick-web/monitor/memory.html");
    }

    /**
     * CPU 监控页
     */
    @GetMapping("/monitor/cpu")
    public RedirectView cpu() {
        return new RedirectView("/brick-web/monitor/cpu.html");
    }

    /**
     * 线程监控页
     */
    @GetMapping("/monitor/threads")
    public RedirectView threads() {
        return new RedirectView("/brick-web/monitor/threads.html");
    }
}
