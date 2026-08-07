package com.couple.app.controller;

import com.couple.app.common.ApiResponse;
import com.couple.app.entity.Anniversary;
import com.couple.app.security.AuthSupport;
import com.couple.app.service.AnniversaryService;
import com.couple.app.service.CoverService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/anniversaries")
public class AnniversaryController {
    private final AnniversaryService anniversaryService;
    private final CoverService coverService;

    public AnniversaryController(AnniversaryService anniversaryService, CoverService coverService) {
        this.anniversaryService = anniversaryService;
        this.coverService = coverService;
    }

    @GetMapping
    public ApiResponse<List<Anniversary>> list() {
        return ApiResponse.ok(anniversaryService.list());
    }

    @GetMapping("/upcoming")
    public ApiResponse<List<Map<String, Object>>> upcoming() {
        return ApiResponse.ok(anniversaryService.upcoming());
    }

    @PostMapping
    public ApiResponse<Anniversary> create(@RequestBody AnniversaryRequest req) {
        return ApiResponse.ok(anniversaryService.create(
                req.getTitle(), LocalDate.parse(req.getEventDate()),
                Boolean.TRUE.equals(req.getYearly()),
                AuthSupport.requirePartner().getPartnerId()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Anniversary> update(@PathVariable Long id, @RequestBody AnniversaryRequest req) {
        return ApiResponse.ok(anniversaryService.update(
                id, req.getTitle(), LocalDate.parse(req.getEventDate()), Boolean.TRUE.equals(req.getYearly())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        anniversaryService.delete(id);
        return ApiResponse.ok();
    }

    @PutMapping("/together-date")
    public ApiResponse<Void> togetherDate(@RequestBody TogetherRequest req) {
        coverService.updateTogetherDate(req.getTogetherDate());
        return ApiResponse.ok();
    }

    @Data
    public static class AnniversaryRequest {
        private String title;
        private String eventDate;
        private Boolean yearly;
    }

    @Data
    public static class TogetherRequest {
        private String togetherDate;
    }
}
