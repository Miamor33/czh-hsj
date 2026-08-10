package com.couple.app.service;

import com.couple.app.common.BusinessException;
import com.couple.app.entity.ChallengeItem;
import com.couple.app.mapper.ChallengeCompletionMapper;
import com.couple.app.mapper.ChallengeCompletionPhotoMapper;
import com.couple.app.mapper.ChallengeItemMapper;
import com.couple.app.mapper.ChallengeModuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeServiceTest {
    @Mock ChallengeModuleMapper moduleMapper;
    @Mock ChallengeItemMapper itemMapper;
    @Mock ChallengeCompletionMapper completionMapper;
    @Mock ChallengeCompletionPhotoMapper photoMapper;
    @Mock FileStorageService fileStorageService;

    ChallengeService service;

    @BeforeEach
    void setUp() {
        service = new ChallengeService(moduleMapper, itemMapper, completionMapper, photoMapper, fileStorageService);
    }

    @Test
    void complete_rejectsEmptyPhotos() {
        ChallengeItem item = new ChallengeItem();
        item.setId(1L);
        when(itemMapper.selectById(1L)).thenReturn(item);
        when(completionMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.complete(1L, null, Collections.emptyList(), 9L));
        assertTrue(ex.getMessage().contains("至少"));
    }

    @Test
    void complete_rejectsMoreThanThree() {
        ChallengeItem item = new ChallengeItem();
        item.setId(1L);
        when(itemMapper.selectById(1L)).thenReturn(item);
        when(completionMapper.selectOne(any())).thenReturn(null);
        List<MultipartFile> photos = List.of(
                new MockMultipartFile("photos", "a.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("photos", "b.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("photos", "c.jpg", "image/jpeg", new byte[]{1}),
                new MockMultipartFile("photos", "d.jpg", "image/jpeg", new byte[]{1})
        );
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.complete(1L, "n", photos, 9L));
        assertTrue(ex.getMessage().contains("最多"));
    }
}
