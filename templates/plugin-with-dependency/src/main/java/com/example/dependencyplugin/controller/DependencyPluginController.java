package com.example.dependencyplugin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dependency-plugin")
public class DependencyPluginController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from dependency plugin template";
    }
}
