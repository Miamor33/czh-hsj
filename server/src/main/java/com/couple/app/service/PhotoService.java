package com.couple.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.app.common.BusinessException;
import com.couple.app.config.CoupleProperties;
import com.couple.app.entity.Photo;
import com.couple.app.mapper.PhotoMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PhotoService {
    private final PhotoMapper photoMapper;
    private final FileStorageService fileStorageService;
    private final CoupleProperties properties;

    public PhotoService(PhotoMapper photoMapper, FileStorageService fileStorageService, CoupleProperties properties) {
        this.photoMapper = photoMapper;
        this.fileStorageService = fileStorageService;
        this.properties = properties;
    }

    public List<Map<String, Object>> list() {
        return photoMapper.selectList(new LambdaQueryWrapper<Photo>().orderByDesc(Photo::getCreatedAt))
                .stream().map(this::toDto).toList();
    }

    public Map<String, Object> upload(MultipartFile file, String caption, Long partnerId) {
        String fileName = fileStorageService.store(file);
        Photo photo = new Photo();
        photo.setFileName(fileName);
        photo.setCaption(caption);
        photo.setFeatured(false);
        photo.setUploadedBy(partnerId);
        photo.setCreatedAt(LocalDateTime.now());
        photoMapper.insert(photo);
        return toDto(photo);
    }

    public Map<String, Object> setFeatured(Long id, boolean featured) {
        Photo photo = photoMapper.selectById(id);
        if (photo == null) {
            throw new BusinessException("照片不存在");
        }
        if (featured) {
            long count = photoMapper.selectCount(new LambdaQueryWrapper<Photo>().eq(Photo::getFeatured, true));
            if (!Boolean.TRUE.equals(photo.getFeatured()) && count >= properties.getUpload().getMaxFeatured()) {
                throw new BusinessException("精选照片最多 " + properties.getUpload().getMaxFeatured() + " 张");
            }
        }
        photo.setFeatured(featured);
        photoMapper.updateById(photo);
        return toDto(photo);
    }

    public void delete(Long id) {
        Photo photo = photoMapper.selectById(id);
        if (photo == null) {
            return;
        }
        photoMapper.deleteById(id);
        fileStorageService.deleteQuietly(photo.getFileName());
    }

    private Map<String, Object> toDto(Photo p) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("url", "/uploads/" + p.getFileName());
        m.put("caption", p.getCaption() == null ? "" : p.getCaption());
        m.put("featured", Boolean.TRUE.equals(p.getFeatured()));
        m.put("createdAt", p.getCreatedAt() == null ? null : p.getCreatedAt().toString());
        return m;
    }
}
