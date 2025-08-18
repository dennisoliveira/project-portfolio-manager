package com.github.dennisoliveira.portfolio.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("dev")
public class RootControllerDev {
    @GetMapping("/")
    public String index() {
        return "redirect:/swagger-ui.html";
    }
}
