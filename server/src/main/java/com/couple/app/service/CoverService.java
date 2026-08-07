package com.couple.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.app.entity.Anniversary;
import com.couple.app.entity.Photo;
import com.couple.app.entity.Setting;
import com.couple.app.mapper.AnniversaryMapper;
import com.couple.app.mapper.PhotoMapper;
import com.couple.app.mapper.SettingMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CoverService {
    private final SettingMapper settingMapper;
    private final AnniversaryMapper anniversaryMapper;
    private final PhotoMapper photoMapper;
    private final AnniversaryService anniversaryService;

    public CoverService(SettingMapper settingMapper, AnniversaryMapper anniversaryMapper,
                        PhotoMapper photoMapper, AnniversaryService anniversaryService) {
        this.settingMapper = settingMapper;
        this.anniversaryMapper = anniversaryMapper;
        this.photoMapper = photoMapper;
        this.anniversaryService = anniversaryService;
    }

    public Map<String, Object> cover() {
        LocalDate together = getTogetherDate();
        long days = ChronoUnit.DAYS.between(together, LocalDate.now()) + 1;
        Map<String, Object> next = anniversaryService.nextAnniversary();
        List<Photo> featured = photoMapper.selectList(new LambdaQueryWrapper<Photo>()
                .eq(Photo::getFeatured, true)
                .orderByDesc(Photo::getCreatedAt));
        Map<String, Object> result = new HashMap<>();
        result.put("brand", "czh & hsj");
        result.put("togetherDate", together.toString());
        result.put("loveDays", days);
        result.put("nextAnniversary", next);
        result.put("featuredPhotos", featured.stream().map(this::toPhotoDto).toList());
        return result;
    }

    public LocalDate getTogetherDate() {
        Setting s = settingMapper.selectOne(new LambdaQueryWrapper<Setting>()
                .eq(Setting::getSettingKey, "togetherDate"));
        if (s == null) {
            return LocalDate.of(2024, 1, 1);
        }
        return LocalDate.parse(s.getSettingValue());
    }

    public void updateTogetherDate(String date) {
        Setting s = settingMapper.selectOne(new LambdaQueryWrapper<Setting>()
                .eq(Setting::getSettingKey, "togetherDate"));
        if (s == null) {
            s = new Setting();
            s.setSettingKey("togetherDate");
            s.setSettingValue(date);
            settingMapper.insert(s);
        } else {
            s.setSettingValue(date);
            settingMapper.updateById(s);
        }
    }

    private Map<String, Object> toPhotoDto(Photo p) {
        return Map.of(
                "id", p.getId(),
                "url", "/uploads/" + p.getFileName(),
                "caption", p.getCaption() == null ? "" : p.getCaption()
        );
    }
}
