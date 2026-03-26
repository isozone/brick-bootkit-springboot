package com.example.brokenplugin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/broken-plugin")
public class BrokenPluginController {

    @GetMapping("/hello")
    public String hello() {
        return "This plugin template is intentionally mispackaged";
    }
}
