# Challenge Multi-Photo Completion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Challenge items can only be marked complete with 1–3 photos; list shows CSS thumbnail grid; historical photo-less completions are removed on startup.

**Architecture:** Add `challenge_completion_photo` table; migrate old `photo_file` into it (delete completions with no photo); change `complete` API to multipart `photos` (1–3); frontend multi-select + thumbnail grid + simple lightbox. No server-side thumbnail files.

**Tech Stack:** Spring Boot 3.3, MyBatis-Plus, H2/MySQL, Vue 3, Vite, existing `FileStorageService`

## Global Constraints

- Photos required: **1–3** per completion
- Thumbnails: **CSS crop of originals** (no server thumbs)
- History without photo: **delete completion** (treat as incomplete)
- Field name: multipart **`photos`** only (deprecate `photo`)
- Response/list field: **`photoUrls: string[]`** (not `photoUrl`)
- Note remains optional
- Reuse `FileStorageService` (jpg/png/webp)
- Spec: `docs/superpowers/specs/2026-08-11-challenge-multi-photo-design.md`

## File Structure

| File | Responsibility |
|------|----------------|
| `server/src/main/resources/db/schema.sql` | Add `challenge_completion_photo` table |
| `server/.../entity/ChallengeCompletionPhoto.java` | Photo row entity |
| `server/.../mapper/ChallengeCompletionPhotoMapper.java` | MyBatis-Plus mapper |
| `server/.../config/ChallengePhotoMigration.java` | Idempotent startup migrate/delete |
| `server/.../service/ChallengeService.java` | Multi-photo complete/list/uncomplete/counts |
| `server/.../controller/ChallengeController.java` | Accept `List<MultipartFile> photos` |
| `server/.../entity/ChallengeCompletion.java` | Stop writing `photoFile` (keep field for migrate read) |
| `web/src/views/ChallengePage.vue` | Multi upload UI, thumbnails, lightbox |
| `server/src/test/java/.../ChallengeServiceTest.java` | Unit tests for validation + DTO shape |

---

### Task 1: Schema + Entity + Mapper

**Files:**
- Modify: `server/src/main/resources/db/schema.sql`
- Create: `server/src/main/java/com/couple/app/entity/ChallengeCompletionPhoto.java`
- Create: `server/src/main/java/com/couple/app/mapper/ChallengeCompletionPhotoMapper.java`

**Interfaces:**
- Consumes: existing MyBatis-Plus `BaseMapper` pattern
- Produces: `ChallengeCompletionPhoto` with fields `id`, `completionId`, `fileName`, `sortOrder`; mapper bean `ChallengeCompletionPhotoMapper`

- [ ] **Step 1: Append table to schema.sql** after `challenge_completion` block

```sql
CREATE TABLE IF NOT EXISTS challenge_completion_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    completion_id BIGINT NOT NULL,
    file_name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);
```

Do **not** drop `challenge_completion.photo_file` yet (migration still reads it).

- [ ] **Step 2: Create entity**

```java
package com.couple.app.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("challenge_completion_photo")
public class ChallengeCompletionPhoto {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long completionId;
    private String fileName;
    private Integer sortOrder;
}
```

- [ ] **Step 3: Create mapper**

```java
package com.couple.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.couple.app.entity.ChallengeCompletionPhoto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChallengeCompletionPhotoMapper extends BaseMapper<ChallengeCompletionPhoto> {
}
```

- [ ] **Step 4: Verify compile**

Run (PowerShell, JDK 17):

```powershell
$env:JAVA_HOME = "C:\Users\czh\.jdks\corretto-17.0.18"
$env:Path = "$env:JAVA_HOME\bin;D:\dev\apache-maven-3.6.3\bin;$env:Path"
cd "D:\ideaproject\czh&hsj\czh-hsj\server"
mvn -q -DskipTests compile
```

Expected: exit code 0

- [ ] **Step 5: Commit**

```powershell
cd "D:\ideaproject\czh&hsj\czh-hsj"
git add server/src/main/resources/db/schema.sql `
  server/src/main/java/com/couple/app/entity/ChallengeCompletionPhoto.java `
  server/src/main/java/com/couple/app/mapper/ChallengeCompletionPhotoMapper.java
git commit -m "feat(challenge): add completion photo table and entity"
```

---

### Task 2: Startup migration

**Files:**
- Create: `server/src/main/java/com/couple/app/config/ChallengePhotoMigration.java`

**Interfaces:**
- Consumes: `ChallengeCompletionMapper`, `ChallengeCompletionPhotoMapper`
- Produces: Idempotent `ApplicationRunner`: if `photoFile` set and no photo rows then insert `sortOrder=0`; if no `photoFile` and no photo rows then delete completion

- [ ] **Step 1: Implement migration runner**

```java
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
```

- [ ] **Step 2: Compile check**

```powershell
mvn -q -DskipTests compile
```

Expected: exit 0

- [ ] **Step 3: Commit**

```powershell
git add server/src/main/java/com/couple/app/config/ChallengePhotoMigration.java
git commit -m "feat(challenge): migrate legacy photo_file; drop photo-less completions"
```

---

### Task 3: ChallengeService multi-photo logic + unit tests

**Files:**
- Modify: `server/src/main/java/com/couple/app/service/ChallengeService.java`
- Create: `server/src/test/java/com/couple/app/service/ChallengeServiceTest.java`

**Interfaces:**
- Consumes: `ChallengeCompletionPhotoMapper`, `FileStorageService`
- Produces:
  - `Map<String, Object> complete(Long itemId, String note, List<MultipartFile> photos, Long partnerId)`
  - items DTO uses `photoUrls` (`List<String>`), `completed` only if photos non-empty
  - `modules()` `completedCount` only counts completions with at least 1 photo row
  - `uncomplete` deletes all photo rows + disk files

- [ ] **Step 1: Write failing unit tests**

```java
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
```

- [ ] **Step 2: Run tests — expect fail** (constructor / method signature missing)

```powershell
$env:JAVA_HOME = "C:\Users\czh\.jdks\corretto-17.0.18"
$env:Path = "$env:JAVA_HOME\bin;D:\dev\apache-maven-3.6.3\bin;$env:Path"
cd "D:\ideaproject\czh&hsj\czh-hsj\server"
mvn -q test -Dtest=ChallengeServiceTest
```

Expected: FAIL (compile error or missing method)

- [ ] **Step 3: Rewrite `ChallengeService`**

Replace the whole class with:

```java
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
                            ChallengeCompletionPhotoMapper photoMapper,
                            FileStorageService fileStorageService) {
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
```

- [ ] **Step 4: Run tests — expect pass**

```powershell
mvn -q test -Dtest=ChallengeServiceTest
```

Expected: `BUILD SUCCESS`, both tests PASS

- [ ] **Step 5: Commit**

```powershell
git add server/src/main/java/com/couple/app/service/ChallengeService.java `
  server/src/test/java/com/couple/app/service/ChallengeServiceTest.java
git commit -m "feat(challenge): require 1-3 photos on complete; return photoUrls"
```

---

### Task 4: ChallengeController API

**Files:**
- Modify: `server/src/main/java/com/couple/app/controller/ChallengeController.java`

**Interfaces:**
- Consumes: `ChallengeService.complete(..., List<MultipartFile> photos, ...)`
- Produces: HTTP `POST .../complete` with `@RequestParam("photos") List<MultipartFile> photos`

- [ ] **Step 1: Update controller method**

Replace `complete` with:

```java
@PostMapping("/items/{itemId}/complete")
public ApiResponse<Map<String, Object>> complete(@PathVariable Long itemId,
                                                 @RequestParam(value = "note", required = false) String note,
                                                 @RequestParam("photos") List<MultipartFile> photos) {
    return ApiResponse.ok(challengeService.complete(itemId, note, photos, AuthSupport.requirePartner().getPartnerId()));
}
```

- [ ] **Step 2: Compile**

```powershell
mvn -q -DskipTests compile
```

Expected: exit 0

- [ ] **Step 3: Commit**

```powershell
git add server/src/main/java/com/couple/app/controller/ChallengeController.java
git commit -m "feat(challenge): accept multipart photos list on complete API"
```

---

### Task 5: Frontend ChallengePage multi-photo + thumbnails

**Files:**
- Modify: `web/src/views/ChallengePage.vue`

**Interfaces:**
- Consumes: API `photoUrls: string[]`, `POST` field `photos`
- Produces: UI requiring 1–3 files; CSS thumbnail grid; click-to-lightbox

- [ ] **Step 1: Replace script photo state/handlers**

Replace `completePhoto` with multi-file state and handlers:

```js
const completingId = ref(null)
const completeNote = ref('')
const completePhotos = ref([])
const completePreviewUrls = ref([])
const completeLoading = ref(false)
const lightboxUrl = ref('')

function revokePreviews() {
  completePreviewUrls.value.forEach((u) => URL.revokeObjectURL(u))
  completePreviewUrls.value = []
}

function startComplete(item) {
  completingId.value = item.id
  completeNote.value = ''
  revokePreviews()
  completePhotos.value = []
}

function cancelComplete() {
  completingId.value = null
  completeNote.value = ''
  revokePreviews()
  completePhotos.value = []
}

function onPhotoSelect(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) return
  const merged = [...completePhotos.value, ...files]
  if (merged.length > 3) {
    error.value = '最多上传 3 张照片'
  }
  const next = merged.slice(0, 3)
  revokePreviews()
  completePhotos.value = next
  completePreviewUrls.value = next.map((f) => URL.createObjectURL(f))
}

function removePhoto(index) {
  const next = completePhotos.value.filter((_, i) => i !== index)
  revokePreviews()
  completePhotos.value = next
  completePreviewUrls.value = next.map((f) => URL.createObjectURL(f))
}

async function submitComplete(itemId) {
  if (completePhotos.value.length < 1) {
    error.value = '请至少上传一张照片'
    return
  }
  completeLoading.value = true
  error.value = ''
  try {
    const form = new FormData()
    if (completeNote.value.trim()) {
      form.append('note', completeNote.value.trim())
    }
    completePhotos.value.forEach((file) => form.append('photos', file))
    await http.post(`/challenges/items/${itemId}/complete`, form, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    cancelComplete()
    await loadModules()
    await loadItems()
  } catch (e) {
    error.value = e.message
  } finally {
    completeLoading.value = false
  }
}
```

- [ ] **Step 2: Update template** (completed thumbs, form, lightbox)

Completed block:

```html
<template v-if="item.completed">
  <p v-if="item.note" class="item-note">{{ item.note }}</p>
  <div v-if="item.photoUrls?.length" class="thumb-grid">
    <button
      v-for="(url, idx) in item.photoUrls"
      :key="url + idx"
      type="button"
      class="thumb-btn"
      @click="lightboxUrl = url"
    >
      <img :src="url" alt="完成照片" class="thumb" loading="lazy" />
    </button>
  </div>
  <button class="btn btn--text btn--sm mt-sm" @click="uncomplete(item)">
    取消完成
  </button>
</template>
```

Form photo field + buttons:

```html
<div class="field">
  <label>照片（必填，1–3 张）</label>
  <input type="file" accept="image/*" multiple @change="onPhotoSelect" />
  <div v-if="completePreviewUrls.length" class="thumb-grid mt-sm">
    <div v-for="(url, idx) in completePreviewUrls" :key="url" class="thumb-wrap">
      <img :src="url" class="thumb" alt="预览" />
      <button type="button" class="thumb-remove" @click="removePhoto(idx)">×</button>
    </div>
  </div>
</div>
<div class="flex-gap">
  <button
    class="btn btn--primary btn--sm"
    :disabled="completeLoading || completePhotos.length < 1"
    @click="submitComplete(item.id)"
  >
    完成
  </button>
  <button class="btn btn--ghost btn--sm" @click="cancelComplete">取消</button>
</div>
```

Lightbox near page root end:

```html
<div v-if="lightboxUrl" class="lightbox" @click.self="lightboxUrl = ''">
  <img :src="lightboxUrl" alt="查看大图" class="lightbox__img" />
  <button type="button" class="btn btn--ghost lightbox__close" @click="lightboxUrl = ''">关闭</button>
</div>
```

- [ ] **Step 3: Replace styles** — remove `.item-photo`; add thumb + lightbox CSS:

```css
.thumb-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.thumb-btn {
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
}

.thumb-wrap {
  position: relative;
}

.thumb {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  display: block;
}

.thumb-remove {
  position: absolute;
  top: -6px;
  right: -6px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.65);
  color: #fff;
  line-height: 22px;
  font-size: 14px;
  cursor: pointer;
}

.lightbox {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.75);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.lightbox__img {
  max-width: 100%;
  max-height: 80vh;
  object-fit: contain;
  border-radius: var(--radius-sm);
}

.lightbox__close {
  margin-top: 12px;
  color: #fff;
}
```

- [ ] **Step 4: Manual UI check**

```powershell
cd "D:\ideaproject\czh&hsj\czh-hsj\web"
node ".\node_modules\vite\bin\vite.js"
```

Open `http://localhost:5173/`, login, Challenges:

1. No photo → complete disabled  
2. 1–3 photos → success + thumbs  
3. Click thumb → lightbox  
4. Uncomplete → incomplete  

- [ ] **Step 5: Commit**

```powershell
git add web/src/views/ChallengePage.vue
git commit -m "feat(challenge): multi-photo required upload and thumbnail grid"
```

---

### Task 6: End-to-end verification

**Files:** none (verification only)

- [ ] **Step 1: Run tests + restart backend**

```powershell
$env:JAVA_HOME = "C:\Users\czh\.jdks\corretto-17.0.18"
$env:Path = "$env:JAVA_HOME\bin;D:\dev\apache-maven-3.6.3\bin;$env:Path"
cd "D:\ideaproject\czh&hsj\czh-hsj\server"
mvn -q test
mvn spring-boot:run
```

Expected: tests green; Tomcat on 8080

- [ ] **Step 2: Spec §9 checklist**

| # | Check |
|---|--------|
| 1 | Cannot complete with 0 photos |
| 2 | Complete with 1–3; list shows same count thumbs |
| 3 | Lightbox opens original |
| 4 | Restart: photo-less legacy → incomplete |
| 5 | Restart: legacy single `photo_file` still completed with 1 thumb |
| 6 | Uncomplete clears files/records; progress drops |
| 7 | Module `completedCount` matches list |

- [ ] **Step 3: Commit leftover fixes only if needed**

---

## Spec coverage (self-review)

| Spec requirement | Task |
|------------------|------|
| New photo table | Task 1 |
| Migrate legacy / delete no-photo | Task 2 |
| complete 1–3 + `photoUrls` | Task 3–4 |
| items/modules completed = has photos | Task 3 |
| uncomplete multi-file delete | Task 3 |
| Frontend multi + CSS thumbs + lightbox | Task 5 |
| Acceptance §9 | Task 6 |

No placeholders; `photos` / `photoUrls` / `complete(...)` naming consistent across tasks.
