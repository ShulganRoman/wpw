package com.wpw.pim.web.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpaFallbackController {

    private static final Resource INDEX = new ClassPathResource("static/index.html");

    // Handles all non-API, non-static paths — returns index.html so React Router takes over
    @GetMapping(value = {
        "/catalog",
        "/catalog/**",
        "/product/**",
        "/export",
        "/admin",
        "/login",
    }, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> spa(HttpServletRequest request) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(INDEX);
    }
}
