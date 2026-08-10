# 挑战打卡多图必填设计

> 日期：2026-08-11  
> 项目：czh & hsj 情侣 H5  
> 状态：已确认  
> 范围：挑战清单标记完成须上传 1–3 张照片，列表以 CSS 缩略图展示

## 1. 目标

将挑战项「标记完成」从「照片可选、单张原图展示」改为：

1. **至少 1 张、最多 3 张**照片才能完成打卡  
2. 列表用 **CSS 小方块缩略图**展示（加载原图 URL，不做服务端缩略图）  
3. 历史「已完成但无图」记录 **视为未完成**，需重新打卡并上传照片

## 2. 产品决策

| 项 | 决策 |
|----|------|
| 照片数量 | 1–3 张（必填下限 1） |
| 缩略图 | A：原图 + CSS 裁切小方块，非服务端生成 thumb |
| 历史无图完成 | B：删除完成记录，进度回退为未完成 |
| 存储结构 | 独立表 `challenge_completion_photo` |
| 备注 | 仍可选 |
| 旧 API 字段 `photo` | 废弃，仅接受 `photos` |

## 3. 数据模型

### 3.1 保留 `challenge_completion`

字段用途不变：`item_id`、`note`、`completed_by`、`completed_at`。

- 列 `photo_file`：**停止写入**；迁移读完后可保留空列（避免破坏 H2 `schema.sql` 已有库的手工改表），实体字段可标废弃或移除读写。
- 「已完成」判定：存在 completion **且** 至少 1 条关联照片。

### 3.2 新建 `challenge_completion_photo`

```sql
CREATE TABLE IF NOT EXISTS challenge_completion_photo (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    completion_id BIGINT NOT NULL,
    file_name VARCHAR(128) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0
);
```

- `completion_id` → `challenge_completion.id`
- `file_name`：与现有 `FileStorageService` 一致的磁盘文件名
- `sort_order`：0–2，展示顺序

### 3.3 启动迁移（一次性）

在应用启动后执行（可挂在 `DataInitializer` / 专用 `ApplicationRunner`，幂等）：

1. 对每条 `challenge_completion`：  
   - 若 `photo_file` 非空 → 若该 completion 尚无 photo 行，则插入一条 `sort_order=0`，`file_name=photo_file`  
   - 若 `photo_file` 为空 **且** 无任何 photo 行 → **删除**该 completion  
2. 迁移后不再依赖 `photo_file` 做业务判断  
3. `modules()` / `items()` 的完成计数只统计「有 ≥1 张照片」的 completion

取消完成：删除 completion、其 photo 行，并对每个 `file_name` 调用 `deleteQuietly`。

## 4. API

### 4.1 `POST /api/challenges/items/{itemId}/complete`

- Content-Type：`multipart/form-data`
- 参数：
  - `note`（可选）
  - `photos`（必填，1–3 个 `MultipartFile`，同名字段多次提交）
- 校验：
  - `photos` 为空或全 empty → `BusinessException`（如「请至少上传一张照片」）
  - 超过 3 张 → `BusinessException`（如「最多上传 3 张照片」）
  - 类型/大小：沿用 `FileStorageService` + 现有 multipart 限制
- 行为（建议 `@Transactional`）：校验未完成 → 插入 completion → 按顺序 store 并插入 photo 行
- 响应示例字段：

```json
{
  "itemId": 1,
  "completed": true,
  "note": "...",
  "photoUrls": ["/uploads/a.jpg", "/uploads/b.jpg"]
}
```

不再返回单个 `photoUrl`。

### 4.2 `GET /api/challenges/modules/{moduleKey}/items`

每项：

- `completed`：boolean（有 completion 且 `photoUrls.length >= 1`）
- `note`、`completedAt`、`completedBy`（仅 completed 时）
- `photoUrls`：`string[]`，按 `sort_order` 升序；未完成可不返回或返回 `[]`

### 4.3 `GET /api/challenges/modules`

`completedCount` 与列表同一套「有图才算完成」规则。

### 4.4 `DELETE /api/challenges/items/{itemId}/complete`

删除 completion、全部关联照片记录与磁盘文件（多文件）。

### 4.5 Controller 签名变更

```java
@RequestParam(value = "photos") List<MultipartFile> photos
```

（或等价数组）；`note` 仍 `required = false`。

## 5. 前端（`ChallengePage.vue`）

### 5.1 打卡表单

- 文案：「照片（必填，1–3 张）」
- `<input type="file" accept="image/*" multiple>`
- 状态：`completePhotos: File[]`（最多 3；多选截断并设置 `error` 提示）
- 选中后本地 `URL.createObjectURL` 预览小方块；离开/取消时 `revokeObjectURL`
- 「完成」按钮：`completePhotos.length === 0` 或提交中时禁用
- 提交：`FormData` 对每个文件 `append('photos', file)`，可选 `note`

### 5.2 列表

- 已完成：`photoUrls` 以约 64–80px 正方形网格展示（`object-fit: cover`）
- 点击缩略图：简单遮罩放大原图（不引入新依赖）
- 去掉整宽 `.item-photo` 大图布局
- 兼容：不再读 `photoUrl`；仅用 `photoUrls`

### 5.3 取消完成

交互不变；依赖后端清多图。

## 6. 错误与边界

| 场景 | 行为 |
|------|------|
| 0 张图提交 | 前端禁用 + 后端拒绝 |
| >3 张 | 前端截断到 3 并提示；后端拒绝 >3 |
| 非图片类型 | 沿用现有错误文案 |
| 重复完成 | 现有「该项已完成」 |
| 迁移后无图 completion | 已删除，不会出现半完成态 |
| 上传部分失败 | 事务回滚；已落盘文件尽量 deleteQuietly（实现计划中写清） |

## 7. 非目标

- 不做服务端缩略图文件  
- 不把挑战照片写入相册 `photo` 表  
- 不支持完成后补传/改图（需取消后重新打卡）  
- 不改挑战模块种子数据本身  

## 8. 涉及文件（预期）

| 层 | 文件 |
|----|------|
| DB | `server/src/main/resources/db/schema.sql` |
| 实体/Mapper | 新建 `ChallengeCompletionPhoto` + Mapper；`ChallengeCompletion` 停用 `photoFile` 写入 |
| 服务 | `ChallengeService`；迁移 runner；`ChallengeController` |
| 前端 | `web/src/views/ChallengePage.vue`（样式含缩略图网格与灯箱） |

## 9. 验收标准

1. 不选照片无法完成；选 1–3 张可完成并刷新列表  
2. 列表显示对应数量缩略图；点击可看大图  
3. 重启后：原无图完成项变为未完成；原单图完成项仍完成且缩略图可见  
4. 取消完成清除全部照片文件与记录，进度正确回退  
5. 模块进度计数与清单 completed 状态一致  
