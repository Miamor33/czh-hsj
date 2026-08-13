package com.couple.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.app.common.BusinessException;
import com.couple.app.entity.ChallengeCompletion;
import com.couple.app.entity.ChallengeCompletionPhoto;
import com.couple.app.entity.ChallengeItem;
import com.couple.app.entity.ChallengeModule;
import com.couple.app.mapper.ChallengeCompletionMapper;
import com.couple.app.mapper.ChallengeCompletionPhotoMapper;
import com.couple.app.mapper.ChallengeItemMapper;
import com.couple.app.mapper.ChallengeModuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChallengeService {
    private final ChallengeModuleMapper moduleMapper;
    private final ChallengeItemMapper itemMapper;
    private final ChallengeCompletionMapper completionMapper;
    private final ChallengeCompletionPhotoMapper photoMapper;
    private final FileStorageService fileStorageService;

    public ChallengeService(ChallengeModuleMapper moduleMapper, ChallengeItemMapper itemMapper,
                            ChallengeCompletionMapper completionMapper,
                            ChallengeCompletionPhotoMapper photoMapper, FileStorageService fileStorageService) {
        this.moduleMapper = moduleMapper;
        this.itemMapper = itemMapper;
        this.completionMapper = completionMapper;
        this.photoMapper = photoMapper;
        this.fileStorageService = fileStorageService;
    }

    public List<Map<String, Object>> modules() {
        List<ChallengeModule> modules = moduleMapper.selectList(new LambdaQueryWrapper<ChallengeModule>()
                .orderByAsc(ChallengeModule::getSortOrder));
        List<ChallengeItem> items = itemMapper.selectList(null);
        Set<Long> doneIds = completedItemIds();
        Map<Long, Long> itemCountByModule = items.stream()
                .collect(Collectors.groupingBy(ChallengeItem::getModuleId, Collectors.counting()));
        Map<Long, Long> doneByModule = items.stream()
                .filter(i -> doneIds.contains(i.getId()))
                .collect(Collectors.groupingBy(ChallengeItem::getModuleId, Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChallengeModule m : modules) {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", m.getId());
            dto.put("moduleKey", m.getModuleKey());
            dto.put("title", m.getTitle());
            dto.put("targetCount", m.getTargetCount());
            dto.put("totalItems", itemCountByModule.getOrDefault(m.getId(), 0L));
            dto.put("completedCount", doneByModule.getOrDefault(m.getId(), 0L));
            result.add(dto);
        }
        return result;
    }

    public List<Map<String, Object>> items(String moduleKey) {
        ChallengeModule module = requireModule(moduleKey);
        List<ChallengeItem> items = itemMapper.selectList(new LambdaQueryWrapper<ChallengeItem>()
                .eq(ChallengeItem::getModuleId, module.getId())
                .orderByAsc(ChallengeItem::getSortOrder));
        Map<Long, ChallengeCompletion> completionMap = completionMapper.selectList(null).stream()
                .collect(Collectors.toMap(ChallengeCompletion::getItemId, c -> c, (a, b) -> a));
        Map<Long, List<String>> urlsByCompletionId = loadPhotoUrlsByCompletionId();

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChallengeItem item : items) {
            ChallengeCompletion c = completionMap.get(item.getId());
            List<String> urls = c == null ? List.of() : urlsByCompletionId.getOrDefault(c.getId(), List.of());
            boolean completed = c != null && !urls.isEmpty();
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", item.getId());
            dto.put("title", item.getTitle());
            dto.put("extraHint", item.getExtraHint());
            dto.put("completed", completed);
            if (completed) {
                dto.put("note", c.getNote());
                dto.put("photoUrls", urls);
                dto.put("completedAt", c.getCompletedAt() == null ? null : c.getCompletedAt().toString());
                dto.put("completedBy", c.getCompletedBy());
            }
            result.add(dto);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> complete(Long itemId, String note, List<MultipartFile> photos, Long partnerId) {
        ChallengeItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("挑战项不存在");
        }
        ChallengeCompletion existing = completionMapper.selectOne(new LambdaQueryWrapper<ChallengeCompletion>()
                .eq(ChallengeCompletion::getItemId, itemId));
        if (existing != null) {
            throw new BusinessException("该项已完成");
        }
        List<MultipartFile> files = normalizePhotos(photos);
        if (files.isEmpty()) {
            throw new BusinessException("请至少上传一张照片");
        }
        if (files.size() > 3) {
            throw new BusinessException("最多上传 3 张照片");
        }

        ChallengeCompletion c = new ChallengeCompletion();
        c.setItemId(itemId);
        c.setNote(note);
        c.setPhotoFile(null);
        c.setCompletedBy(partnerId);
        c.setCompletedAt(LocalDateTime.now());
        completionMapper.insert(c);

        List<String> stored = new ArrayList<>();
        try {
            int order = 0;
            for (MultipartFile file : files) {
                String name = fileStorageService.store(file);
                stored.add(name);
                ChallengeCompletionPhoto row = new ChallengeCompletionPhoto();
                row.setCompletionId(c.getId());
                row.setFileName(name);
                row.setSortOrder(order++);
                photoMapper.insert(row);
            }
        } catch (RuntimeException ex) {
            for (String name : stored) {
                fileStorageService.deleteQuietly(name);
            }
            throw ex;
        }

        List<String> urls = stored.stream().map(n -> "/uploads/" + n).collect(Collectors.toList());
        Map<String, Object> body = new HashMap<>();
        body.put("itemId", itemId);
        body.put("completed", true);
        body.put("note", note == null ? "" : note);
        body.put("photoUrls", urls);
        return body;
    }

    @Transactional
    public void uncomplete(Long itemId) {
        ChallengeCompletion existing = completionMapper.selectOne(new LambdaQueryWrapper<ChallengeCompletion>()
                .eq(ChallengeCompletion::getItemId, itemId));
        if (existing == null) {
            return;
        }
        List<ChallengeCompletionPhoto> photos = photoMapper.selectList(new LambdaQueryWrapper<ChallengeCompletionPhoto>()
                .eq(ChallengeCompletionPhoto::getCompletionId, existing.getId()));
        for (ChallengeCompletionPhoto p : photos) {
            fileStorageService.deleteQuietly(p.getFileName());
            photoMapper.deleteById(p.getId());
        }
        if (existing.getPhotoFile() != null) {
            fileStorageService.deleteQuietly(existing.getPhotoFile());
        }
        completionMapper.deleteById(existing.getId());
    }

    public ChallengeItem addItem(String moduleKey, String title) {
        ChallengeModule module = requireModule(moduleKey);
        if (title == null || title.isBlank()) {
            throw new BusinessException("标题不能为空");
        }
        Long count = itemMapper.selectCount(new LambdaQueryWrapper<ChallengeItem>()
                .eq(ChallengeItem::getModuleId, module.getId()));
        ChallengeItem item = new ChallengeItem();
        item.setModuleId(module.getId());
        item.setTitle(title.trim());
        item.setSortOrder(count.intValue() + 1);
        itemMapper.insert(item);
        return item;
    }

    private List<MultipartFile> normalizePhotos(List<MultipartFile> photos) {
        if (photos == null) {
            return List.of();
        }
        return photos.stream().filter(f -> f != null && !f.isEmpty()).collect(Collectors.toList());
    }

    private Set<Long> completedItemIds() {
        List<ChallengeCompletion> completions = completionMapper.selectList(null);
        if (completions.isEmpty()) {
            return Set.of();
        }
        Map<Long, List<String>> urls = loadPhotoUrlsByCompletionId();
        return completions.stream()
                .filter(c -> !urls.getOrDefault(c.getId(), List.of()).isEmpty())
                .map(ChallengeCompletion::getItemId)
                .collect(Collectors.toSet());
    }

    private Map<Long, List<String>> loadPhotoUrlsByCompletionId() {
        List<ChallengeCompletionPhoto> all = photoMapper.selectList(new LambdaQueryWrapper<ChallengeCompletionPhoto>()
                .orderByAsc(ChallengeCompletionPhoto::getSortOrder)
                .orderByAsc(ChallengeCompletionPhoto::getId));
        Map<Long, List<String>> map = new HashMap<>();
        for (ChallengeCompletionPhoto p : all) {
            map.computeIfAbsent(p.getCompletionId(), k -> new ArrayList<>())
                    .add("/uploads/" + p.getFileName());
        }
        return map;
    }

    private ChallengeModule requireModule(String moduleKey) {
        ChallengeModule module = moduleMapper.selectOne(new LambdaQueryWrapper<ChallengeModule>()
                .eq(ChallengeModule::getModuleKey, moduleKey));
        if (module == null) {
            throw new BusinessException("模块不存在");
        }
        return module;
    }
}
