package com.couple.app.controller;

import com.couple.app.common.ApiResponse;
import com.couple.app.entity.ChallengeItem;
import com.couple.app.security.AuthSupport;
import com.couple.app.service.ChallengeService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {
    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping("/modules")
    public ApiResponse<List<Map<String, Object>>> modules() {
        return ApiResponse.ok(challengeService.modules());
    }

    @GetMapping("/modules/{moduleKey}/items")
    public ApiResponse<List<Map<String, Object>>> items(@PathVariable String moduleKey) {
        return ApiResponse.ok(challengeService.items(moduleKey));
    }

    @PostMapping("/modules/{moduleKey}/items")
    public ApiResponse<ChallengeItem> addItem(@PathVariable String moduleKey, @RequestBody TitleRequest req) {
        return ApiResponse.ok(challengeService.addItem(moduleKey, req.getTitle()));
    }

    @PostMapping("/items/{itemId}/complete")
    public ApiResponse<Map<String, Object>> complete(@PathVariable Long itemId,
                                                     @RequestParam(value = "note", required = false) String note,
                                                     @RequestParam("photos") List<MultipartFile> photos) {
        return ApiResponse.ok(challengeService.complete(itemId, note, photos, AuthSupport.requirePartner().getPartnerId()));
    }

    @DeleteMapping("/items/{itemId}/complete")
    public ApiResponse<Void> uncomplete(@PathVariable Long itemId) {
        challengeService.uncomplete(itemId);
        return ApiResponse.ok();
    }

    @Data
    public static class TitleRequest {
        private String title;
    }
}
