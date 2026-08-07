package com.couple.app.controller;

import com.couple.app.common.ApiResponse;
import com.couple.app.service.CoverService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public")
public class PublicController {
    private final CoverService coverService;

    public PublicController(CoverService coverService) {
        this.coverService = coverService;
    }

    @GetMapping("/cover")
    public ApiResponse<Map<String, Object>> cover() {
        return ApiResponse.ok(coverService.cover());
    }
}
