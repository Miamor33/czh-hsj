# 双击切换玩偶模型变体 — 设计文档

> 日期：2026-08-17  
> 项目：czh & hsj 情侣 H5  
> 状态：待审阅  
> 关联：封面 / 登录首页 3D 玩偶（`DollDuoStage` / `DollViewer`）

## 1. 目标

允许访客在玩偶舞台上通过**双击**在默认模型与 `r` 后缀变体之间切换：

- 默认：`czh`、`hsj`
- 变体：`czhr`、`hsjr`（资源 `/models/czhr.glb`、`/models/hsjr.glb`）

不改变登录身份、气泡文案归属、旋转/复位等既有行为。

## 2. 产品决策

| 项 | 决策 |
|----|------|
| 切换方式 | 双击同一只玩偶，在基础 ↔ r 之间来回切换 |
| 封面页（`mode=cover`） | 左右两只均可双击换模 |
| 登录首页（`mode=home`） | **仅大模型**（`size=focus`）可双击换模；小模型（`aside`）不可 |
| 持久化 | 不记住；刷新后回到默认 `czh` / `hsj` |
| 封面与内部首页 | **不共用**变体状态；各自从默认开始 |
| 缺 glb 文件 | 与现有缺模行为一致，不额外 toast |
| 登录身份 | 仍用 `auth.partnerKey`（`czh`/`hsj`），不受变体影响 |

## 3. 交互规则

1. 双击窗口约 300ms；判定为双击时只触发换模，并抑制第二次单击气泡。
2. 拖拽绕看（位移 ≥ 10px）不触发单击/双击（沿用现有 pointer 阈值）。
3. 单击：轻跳 + 气泡逻辑不变。
4. 换模后 `who` 变化，`DollViewer` 按现有逻辑重载模型并 `resetFraming`。

## 4. 技术方案（方案 A：舞台本地状态）

### 4.1 元数据 `web/src/dolls/meta.js`

- 新增 `czhr`、`hsjr` 条目（`src`、`theme`、文案；文案可先复用基础版）。
- 每条目增加 `baseKey`：`czh`/`hsj`（含基础自身）。
- 工具函数：
  - `toBaseKey(key)` — 变体 → 基础身份
  - `toggleVariantKey(key)` — `czh↔czhr`，`hsj↔hsjr`
- `otherPartnerKey` 继续只处理基础身份（登录 focus / 对方）。

### 4.2 状态 `DollDuoStage` 本地

```js
// 每个舞台组件实例一份；封面与 home 互不共享；不写 localStorage
variantMap = { czh: 'czh', hsj: 'hsj' }
```

- 传给 `DollViewer` 的 `who` = 当前变体 key。
- 气泡 `bubbleMap` 仍按 **baseKey** 索引，避免 r 后缀打乱现有 `czh`/`hsj` 键。
- 组件卸载后状态丢弃 → 满足不持久、不共用。

### 4.3 `DollViewer.vue`

- 新增事件 `dblselect`（payload：当前 `who`）。
- 在现有 pointer 单击检测上叠加双击识别；双击时：
  - `emit('dblselect', who)`
  - 不连发第二次 `select`
- 单击路径保持 `playHop` + `emit('select')`。

### 4.4 `DollDuoStage.vue`

- `cover`：两侧均监听 `@dblselect` → 按该侧 `baseKey` 更新 `variantMap`。
- `home`：仅 `focus`（大）监听/处理 `@dblselect`；`aside` 忽略。
- 模板不再写死 `who="czh"` / `who="hsj"`，改为 `variantMap` 当前值。
- 旋转开关、复位相机逻辑不改。

### 4.5 资源

- `web/public/models/czhr.glb`、`hsjr.glb`（由资源方提供）
- 更新 `web/public/models/README.md` 命名约定

### 4.6 明确不改

- `stores/auth.js`、`stores/doll.js`（旋转）
- 相册 / 问答 / 挑战页（无玩偶舞台）

## 5. 验收清单

1. 封面：双击左/右均可在基础 ↔ r 间切换，再双击切回。
2. 登录首页：仅大模型可切换；小模型双击不换模。
3. 刷新后均为默认 `czh`/`hsj`。
4. 封面切换后进入登录首页，首页仍为默认；反之亦然。
5. 单击气泡、轻跳、旋转/复位仍正常。

## 6. 范围外

- 变体状态云端同步、URL 分享、更多变体后缀、换模动画特效、缺模专用提示 UI。
