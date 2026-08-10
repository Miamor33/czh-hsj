package com.couple.app.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.app.entity.ChallengeCompletion;
import com.couple.app.entity.ChallengeCompletionPhoto;
import com.couple.app.mapper.ChallengeCompletionMapper;
import com.couple.app.mapper.ChallengeCompletionPhotoMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(100)
public class ChallengePhotoMigration implements ApplicationRunner {
    private final ChallengeCompletionMapper completionMapper;
    private final ChallengeCompletionPhotoMapper photoMapper;

    public ChallengePhotoMigration(ChallengeCompletionMapper completionMapper,
                                   ChallengeCompletionPhotoMapper photoMapper) {
        this.completionMapper = completionMapper;
        this.photoMapper = photoMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<ChallengeCompletion> completions = completionMapper.selectList(null);
        for (ChallengeCompletion c : completions) {
            Long photoCount = photoMapper.selectCount(new LambdaQueryWrapper<ChallengeCompletionPhoto>()
                    .eq(ChallengeCompletionPhoto::getCompletionId, c.getId()));
            if (photoCount != null && photoCount > 0) {
                continue;
            }
            String legacy = c.getPhotoFile();
            if (legacy != null && !legacy.isBlank()) {
                ChallengeCompletionPhoto row = new ChallengeCompletionPhoto();
                row.setCompletionId(c.getId());
                row.setFileName(legacy);
                row.setSortOrder(0);
                photoMapper.insert(row);
            } else {
                completionMapper.deleteById(c.getId());
            }
        }
    }
}
