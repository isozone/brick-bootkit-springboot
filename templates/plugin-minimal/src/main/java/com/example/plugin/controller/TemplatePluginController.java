package com.example.plugin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/template-plugin")
public class TemplatePluginController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Brick BootKit plugin template";
    }
}
