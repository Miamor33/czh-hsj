package com.couple.app.controller;

import com.couple.app.common.ApiResponse;
import com.couple.app.security.AuthSupport;
import com.couple.app.service.PhotoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {
    private final PhotoService photoService;

    public PhotoController(PhotoService photoService) {
        this.photoService = photoService;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list() {
        return ApiResponse.ok(photoService.list());
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "caption", required = false) String caption) {
        return ApiResponse.ok(photoService.upload(file, caption, AuthSupport.requirePartner().getPartnerId()));
    }

    @PatchMapping("/{id}/featured")
    public ApiResponse<Map<String, Object>> featured(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        boolean featured = Boolean.TRUE.equals(body.get("featured"));
        return ApiResponse.ok(photoService.setFeatured(id, featured));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        photoService.delete(id);
        return ApiResponse.ok();
    }
}
