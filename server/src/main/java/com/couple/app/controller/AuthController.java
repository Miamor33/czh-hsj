package com.couple.app.controller;

import com.couple.app.common.ApiResponse;
import com.couple.app.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request.getPartnerKey(), request.getPassword()));
    }

    @Data
    public static class LoginRequest {
        @NotBlank
        private String partnerKey;
        @NotBlank
        private String password;
    }
}
