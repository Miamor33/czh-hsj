package com.couple.app.service;

import com.couple.app.common.BusinessException;
import com.couple.app.config.CoupleProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp", "image/jpg");
    private final Path root;

    public FileStorageService(CoupleProperties properties) throws IOException {
        this.root = Paths.get(properties.getUpload().getDir()).toAbsolutePath().normalize();
        Files.createDirectories(this.root);
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (contentType.contains("heic") || contentType.contains("heif")
                || originalNameLooksLike(file.getOriginalFilename(), ".heic", ".heif")) {
            throw new BusinessException("不支持 iPhone HEIC，请先转为 jpg 再上传");
        }
        if (!ALLOWED.contains(contentType) && !looksLikeImage(file.getOriginalFilename())) {
            throw new BusinessException("仅支持 jpg/png/webp");
        }
        String ext = extension(file.getOriginalFilename(), contentType);
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Path target = root.resolve(name).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("非法文件路径");
            }
            file.transferTo(target);
            return name;
        } catch (IOException e) {
            throw new BusinessException("上传失败");
        }
    }

    public void deleteQuietly(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            Path target = root.resolve(fileName).normalize();
            if (target.startsWith(root)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException ignored) {
        }
    }

    private boolean originalNameLooksLike(String name, String... suffixes) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        for (String suffix : suffixes) {
            if (lower.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private boolean looksLikeImage(String name) {
        return originalNameLooksLike(name, ".jpg", ".jpeg", ".png", ".webp");
    }

    private String extension(String original, String contentType) {
        if (original != null) {
            int idx = original.lastIndexOf('.');
            if (idx >= 0) {
                return original.substring(idx).toLowerCase();
            }
        }
        if (contentType.contains("png")) return ".png";
        if (contentType.contains("webp")) return ".webp";
        return ".jpg";
    }
}
