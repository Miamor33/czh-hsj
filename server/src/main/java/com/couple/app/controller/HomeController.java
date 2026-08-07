package com.couple.app.controller;

import com.couple.app.common.ApiResponse;
import com.couple.app.security.AuthSupport;
import com.couple.app.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/home")
public class HomeController {
    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> home() {
        return ApiResponse.ok(homeService.dashboard(AuthSupport.requirePartner().getPartnerId()));
    }
}
