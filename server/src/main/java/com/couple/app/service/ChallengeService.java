package com.couple.app.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.couple.app.common.BusinessException;
import com.couple.app.entity.ChallengeCompletion;
import com.couple.app.entity.ChallengeItem;
import com.couple.app.entity.ChallengeModule;
import com.couple.app.mapper.ChallengeCompletionMapper;
import com.couple.app.mapper.ChallengeItemMapper;
import com.couple.app.mapper.ChallengeModuleMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChallengeService {
    private final ChallengeModuleMapper moduleMapper;
    private final ChallengeItemMapper itemMapper;
    private final ChallengeCompletionMapper completionMapper;
    private final FileStorageService fileStorageService;

    public ChallengeService(ChallengeModuleMapper moduleMapper, ChallengeItemMapper itemMapper,
                            ChallengeCompletionMapper completionMapper, FileStorageService fileStorageService) {
        this.moduleMapper = moduleMapper;
        this.itemMapper = itemMapper;
        this.completionMapper = completionMapper;
        this.fileStorageService = fileStorageService;
    }

    public List<Map<String, Object>> modules() {
        List<ChallengeModule> modules = moduleMapper.selectList(new LambdaQueryWrapper<ChallengeModule>()
                .orderByAsc(ChallengeModule::getSortOrder));
        List<ChallengeItem> items = itemMapper.selectList(null);
        List<ChallengeCompletion> completions = completionMapper.selectList(null);
        Set<Long> doneIds = completions.stream().map(ChallengeCompletion::getItemId).collect(Collectors.toSet());
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

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChallengeItem item : items) {
            ChallengeCompletion c = completionMap.get(item.getId());
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", item.getId());
            dto.put("title", item.getTitle());
            dto.put("extraHint", item.getExtraHint());
            dto.put("completed", c != null);
            if (c != null) {
                dto.put("note", c.getNote());
                dto.put("photoUrl", c.getPhotoFile() == null ? null : "/uploads/" + c.getPhotoFile());
                dto.put("completedAt", c.getCompletedAt() == null ? null : c.getCompletedAt().toString());
                dto.put("completedBy", c.getCompletedBy());
            }
            result.add(dto);
        }
        return result;
    }

    public Map<String, Object> complete(Long itemId, String note, MultipartFile photo, Long partnerId) {
        ChallengeItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException("挑战项不存在");
        }
        ChallengeCompletion existing = completionMapper.selectOne(new LambdaQueryWrapper<ChallengeCompletion>()
                .eq(ChallengeCompletion::getItemId, itemId));
        if (existing != null) {
            throw new BusinessException("该项已完成");
        }
        ChallengeCompletion c = new ChallengeCompletion();
        c.setItemId(itemId);
        c.setNote(note);
        if (photo != null && !photo.isEmpty()) {
            c.setPhotoFile(fileStorageService.store(photo));
        }
        c.setCompletedBy(partnerId);
        c.setCompletedAt(LocalDateTime.now());
        completionMapper.insert(c);
        return Map.of(
                "itemId", itemId,
                "completed", true,
                "note", note == null ? "" : note,
                "photoUrl", c.getPhotoFile() == null ? "" : "/uploads/" + c.getPhotoFile()
        );
    }

    public void uncomplete(Long itemId) {
        ChallengeCompletion existing = completionMapper.selectOne(new LambdaQueryWrapper<ChallengeCompletion>()
                .eq(ChallengeCompletion::getItemId, itemId));
        if (existing == null) {
            return;
        }
        completionMapper.deleteById(existing.getId());
        fileStorageService.deleteQuietly(existing.getPhotoFile());
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

    private ChallengeModule requireModule(String moduleKey) {
        ChallengeModule module = moduleMapper.selectOne(new LambdaQueryWrapper<ChallengeModule>()
                .eq(ChallengeModule::getModuleKey, moduleKey));
        if (module == null) {
            throw new BusinessException("模块不存在");
        }
        return module;
    }
}
