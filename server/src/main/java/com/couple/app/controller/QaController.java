package com.couple.app.controller;

import com.couple.app.common.ApiResponse;
import com.couple.app.entity.QaQuestion;
import com.couple.app.security.AuthSupport;
import com.couple.app.service.QaService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/qa")
public class QaController {
    private final QaService qaService;

    public QaController(QaService qaService) {
        this.qaService = qaService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(@RequestParam(value = "filter", required = false) String filter) {
        return ApiResponse.ok(qaService.list(AuthSupport.requirePartner().getPartnerId(), filter));
    }

    @GetMapping("/questions/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.ok(qaService.detail(id, AuthSupport.requirePartner().getPartnerId()));
    }

    @PostMapping("/questions")
    public ApiResponse<QaQuestion> add(@RequestBody ContentRequest req) {
        return ApiResponse.ok(qaService.addQuestion(req.getContent(), AuthSupport.requirePartner().getPartnerId()));
    }

    @PostMapping("/questions/{id}/answer")
    public ApiResponse<Void> answer(@PathVariable Long id, @RequestBody ContentRequest req) {
        qaService.answer(id, req.getContent(), AuthSupport.requirePartner().getPartnerId());
        return ApiResponse.ok();
    }

    @PostMapping("/questions/{id}/replies")
    public ApiResponse<Void> reply(@PathVariable Long id, @RequestBody ContentRequest req) {
        qaService.reply(id, req.getContent(), AuthSupport.requirePartner().getPartnerId());
        return ApiResponse.ok();
    }

    @PostMapping("/questions/{id}/nudge")
    public ApiResponse<Void> nudge(@PathVariable Long id) {
        qaService.nudge(id, AuthSupport.requirePartner().getPartnerId());
        return ApiResponse.ok();
    }

    @Data
    public static class ContentRequest {
        private String content;
    }
}
