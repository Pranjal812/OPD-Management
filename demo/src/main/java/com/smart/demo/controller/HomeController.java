package com.smart.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home"; // This looks for home.html in templates folder
    }

    @GetMapping("/home")
    public String homePage() {
        return "home"; // This also looks for home.html in templates folder
    }
}