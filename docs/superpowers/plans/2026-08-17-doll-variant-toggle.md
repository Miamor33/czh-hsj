# 双击切换玩偶 r 变体 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 封面页左右玩偶均可双击在 `czh↔czhr` / `hsj↔hsjr` 间切换；登录首页仅大模型可切换；状态仅舞台本地、不持久、封面与首页不共用。

**Architecture:** 在 `meta.js` 扩展变体元数据与 `toBaseKey` / `toggleVariantKey`；`DollViewer` 用延迟单击识别双击并 emit `dblselect`；`DollDuoStage` 用本地 `variantMap` 驱动 `who`，封面两侧响应、home 仅 focus 响应。

**Tech Stack:** Vue 3、Pinia（仅沿用旋转开关）、`@google/model-viewer`、Vite；纯函数用 Node `assert` 做无依赖自检。

## Global Constraints

- 默认模型：`czh`、`hsj`；变体：`czhr`、`hsjr`
- 资源路径：`/models/czhr.glb`、`/models/hsjr.glb`
- 封面：两侧均可双击；登录首页：仅 `size=focus` 可双击
- 双击来回切换；不写 localStorage；封面与 home 状态不共用
- 气泡按 `baseKey` 索引；`auth.partnerKey` 不变
- 双击窗口约 300ms；拖拽位移 ≥ 10px 不触发
- Spec：`docs/superpowers/specs/2026-08-17-doll-variant-toggle-design.md`
- 注释用中文 UTF-8；尽量不改无关既有逻辑
- 操作环境：Windows / PowerShell（命令用 `;` 连接，不用 `&&`）

## File Structure

| File | Responsibility |
|------|----------------|
| `web/src/dolls/meta.js` | 变体元数据 + `toBaseKey` / `toggleVariantKey` |
| `web/src/dolls/meta.selftest.mjs` | Node 自检（无测试框架时验证纯函数） |
| `web/src/components/DollViewer.vue` | 双击识别，`dblselect` 事件 |
| `web/src/components/DollDuoStage.vue` | 本地 `variantMap`、按 mode 响应双击 |
| `web/public/models/README.md` | 补充 `czhr`/`hsjr` 命名约定 |
| `web/public/models/czhr.glb` / `hsjr.glb` | 若工作区已有未跟踪文件则纳入提交 |

---

### Task 1: 元数据与切换工具函数

**Files:**
- Modify: `web/src/dolls/meta.js`
- Create: `web/src/dolls/meta.selftest.mjs`

**Interfaces:**
- Consumes: 现有 `DOLL_META`、`otherPartnerKey`
- Produces:
  - `DOLL_META.czh|hsj|czhr|hsjr` 均含 `baseKey: 'czh'|'hsj'`
  - `toBaseKey(key: string): string`
  - `toggleVariantKey(key: string): string`（`czh↔czhr`，`hsj↔hsjr`；未知 key 原样返回）

- [ ] **Step 1: 写失败自检**

创建 `web/src/dolls/meta.selftest.mjs`：

```js
import assert from 'node:assert/strict'
import { DOLL_META, toBaseKey, toggleVariantKey, otherPartnerKey } from './meta.js'

assert.equal(toBaseKey('czh'), 'czh')
assert.equal(toBaseKey('czhr'), 'czh')
assert.equal(toBaseKey('hsj'), 'hsj')
assert.equal(toBaseKey('hsjr'), 'hsj')
assert.equal(toggleVariantKey('czh'), 'czhr')
assert.equal(toggleVariantKey('czhr'), 'czh')
assert.equal(toggleVariantKey('hsj'), 'hsjr')
assert.equal(toggleVariantKey('hsjr'), 'hsj')
assert.equal(DOLL_META.czhr.src, '/models/czhr.glb')
assert.equal(DOLL_META.hsjr.src, '/models/hsjr.glb')
assert.equal(DOLL_META.czhr.baseKey, 'czh')
assert.equal(DOLL_META.hsjr.baseKey, 'hsj')
assert.equal(otherPartnerKey('czh'), 'hsj')
assert.equal(otherPartnerKey('hsj'), 'czh')
console.log('meta.selftest OK')
```

- [ ] **Step 2: 运行自检（应失败）**

```powershell
cd web; node src/dolls/meta.selftest.mjs
```

Expected: 报错（`toBaseKey` / `czhr` 不存在）

- [ ] **Step 3: 实现 `meta.js`**

用以下完整内容替换 `web/src/dolls/meta.js`（保留原 czh/hsj 文案，补 baseKey 与变体）：

```js
export const DOLL_META = {
  czh: {
    key: 'czh',
    baseKey: 'czh',
    name: 'czh',
    role: '一身黑的czh',
    src: '/models/czh.glb',
    theme: 'him',
    greet: '嗨，我是q版毛绒',
    miss: '是不是想 czh 了，我会替你告诉他的',
  },
  hsj: {
    key: 'hsj',
    baseKey: 'hsj',
    name: 'hsj',
    role: '蓝色小裙hsj',
    src: '/models/hsj.glb',
    theme: 'her',
    greet: '嗨，我是q版毛绒',
    miss: '是不是想 hsj 了，我会替你告诉她的',
  },
  czhr: {
    key: 'czhr',
    baseKey: 'czh',
    name: 'czh',
    role: '一身黑的czh',
    src: '/models/czhr.glb',
    theme: 'him',
    greet: '嗨，我是q版毛绒',
    miss: '是不是想 czh 了，我会替你告诉他的',
  },
  hsjr: {
    key: 'hsjr',
    baseKey: 'hsj',
    name: 'hsj',
    role: '蓝色小裙hsj',
    src: '/models/hsjr.glb',
    theme: 'her',
    greet: '嗨，我是q版毛绒',
    miss: '是不是想 hsj 了，我会替你告诉她的',
  },
}

/** 变体 key → 基础身份（登录 / 气泡用） */
export function toBaseKey(key) {
  return DOLL_META[key]?.baseKey || key
}

/** 基础 ↔ r 后缀来回切换 */
export function toggleVariantKey(key) {
  const base = toBaseKey(key)
  if (base === 'czh') return key === 'czhr' ? 'czh' : 'czhr'
  if (base === 'hsj') return key === 'hsjr' ? 'hsj' : 'hsjr'
  return key
}

export function otherPartnerKey(key) {
  return toBaseKey(key) === 'czh' ? 'hsj' : 'czh'
}
```

- [ ] **Step 4: 再跑自检**

```powershell
cd web; node src/dolls/meta.selftest.mjs
```

Expected: 打印 `meta.selftest OK`

- [ ] **Step 5: 更新 README，并确认 glb 存在**

`web/public/models/README.md` 改为：

```markdown
# 占位模型（可替换）

- `czh.glb` → 男主默认玩偶
- `hsj.glb` → 女主默认玩偶
- `czhr.glb` → 男主 r 变体（双击切换）
- `hsjr.glb` → 女主 r 变体（双击切换）

替换时保持文件名不变即可。
```

确认 `web/public/models/czhr.glb` 与 `hsjr.glb` 已存在（工作区若已有未跟踪文件则保留；若缺失需向资源方索取，不可用空文件冒充）。

- [ ] **Step 6: Commit**

```powershell
git add web/src/dolls/meta.js web/src/dolls/meta.selftest.mjs web/public/models/README.md
# 若 glb 已就绪且体积可接受，一并加入：
git add web/public/models/czhr.glb web/public/models/hsjr.glb
# 用 -F 写提交说明（本机 git 对部分 -m 包装可能报 trailer 错误）
Set-Content -Path .git/COMMIT_MSG_TMP -Encoding utf8NoBOM -Value "feat(web): 玩偶变体元数据与切换工具函数"
D:\devTool\Git\cmd\git.exe commit -F .git/COMMIT_MSG_TMP
Remove-Item .git/COMMIT_MSG_TMP
```

---

### Task 2: DollViewer 双击识别

**Files:**
- Modify: `web/src/components/DollViewer.vue`

**Interfaces:**
- Consumes: 现有 pointer 单击阈值（位移 &lt; 10px、时长 &lt; 450ms）
- Produces: 事件 `dblselect(who: string)`；单击仍 `select(who)`；真双击时**不**发射 `select`（延迟单击方案）

- [ ] **Step 1: 扩展 emits 与双击逻辑**

在 `<script setup>` 中：

1. `defineEmits(['select', 'dblselect'])`
2. 增加：

```js
const DBL_MS = 300
let lastTapAt = 0
let singleTapTimer = 0
```

3. 将 `onPointerUp` 中「轻跳 + emit select」改为（保留位移/时长判断）：

```js
function onPointerUp(event) {
  const start = pointerDown.value
  pointerDown.value = null
  if (!start) return
  const dx = Math.abs(event.clientX - start.x)
  const dy = Math.abs(event.clientY - start.y)
  if (dx >= 10 || dy >= 10 || Date.now() - start.t >= 450) return

  const now = Date.now()
  if (lastTapAt && now - lastTapAt < DBL_MS) {
    window.clearTimeout(singleTapTimer)
    singleTapTimer = 0
    lastTapAt = 0
    playHop()
    emit('dblselect', props.who)
    return
  }

  lastTapAt = now
  window.clearTimeout(singleTapTimer)
  singleTapTimer = window.setTimeout(() => {
    singleTapTimer = 0
    playHop()
    emit('select', props.who)
  }, DBL_MS)
}
```

说明：第一次轻点会延迟 ~300ms 再出气泡；双击只 hop + `dblselect`，不弹气泡。符合「双击只换模、抑制单击路径」。

- [ ] **Step 2: 手动冒烟（可选，未接舞台前）**

暂可跳过到 Task 3；若先验证，可在父级临时 `@dblselect="console.log"`。

- [ ] **Step 3: Commit**

```powershell
git add web/src/components/DollViewer.vue
Set-Content -Path .git/COMMIT_MSG_TMP -Encoding utf8NoBOM -Value "feat(web): DollViewer 支持双击 dblselect"
D:\devTool\Git\cmd\git.exe commit -F .git/COMMIT_MSG_TMP
Remove-Item .git/COMMIT_MSG_TMP
```

---

### Task 3: DollDuoStage 本地变体与权限

**Files:**
- Modify: `web/src/components/DollDuoStage.vue`

**Interfaces:**
- Consumes: `toBaseKey`、`toggleVariantKey`、`DOLL_META`、`otherPartnerKey`；`DollViewer` 的 `select` / `dblselect`
- Produces: 本地 `variantMap: { czh: string, hsj: string }`；cover 双侧可切；home 仅 focus 可切

- [ ] **Step 1: 更新 script 导入与状态**

将导入改为：

```js
import { DOLL_META, otherPartnerKey, toBaseKey, toggleVariantKey } from '../dolls/meta'
```

在 `bubbleMap` 旁增加：

```js
/** 当前舞台变体；仅内存，不跨页面共享 */
const variantMap = ref({ czh: 'czh', hsj: 'hsj' })
```

增加计算属性（模板用）：

```js
const coverLeftWho = computed(() => variantMap.value.czh)
const coverRightWho = computed(() => variantMap.value.hsj)
const mainVariantWho = computed(() => variantMap.value[mainWho.value] || mainWho.value)
const asideVariantWho = computed(() => variantMap.value[asideWho.value] || asideWho.value)
```

- [ ] **Step 2: 修正 onSelect 用 baseKey；新增 onDblSelect**

`onSelect` 开头改为：

```js
function onSelect(who) {
  const base = toBaseKey(who)
  const meta = DOLL_META[who] || DOLL_META[base]
  if (!meta) return

  if (props.mode === 'cover') {
    bubbleMap.value = {
      czh: '',
      hsj: '',
      [base]: `${meta.name} · ${meta.role}`,
    }
    clearBubblesSoon()
    return
  }

  bubbleMap.value = {
    czh: '',
    hsj: '',
    [base]: base === props.focusKey
      ? `${meta.greet}·${props.selfName || meta.name}`
      : `${meta.miss}`,
  }
  clearBubblesSoon()
}

function onDblSelect(who) {
  const base = toBaseKey(who)
  // home 模式仅大模型可换；aside 即使误触也不处理
  if (props.mode === 'home' && base !== mainWho.value) return
  const current = variantMap.value[base] || base
  variantMap.value = {
    ...variantMap.value,
    [base]: toggleVariantKey(current),
  }
}
```

气泡绑定保持按基础 key：`bubbleMap.czh` / `bubbleMap.hsj` / `bubbleMap[mainWho]` / `bubbleMap[asideWho]`（`mainWho`/`asideWho` 仍是基础身份）。

- [ ] **Step 3: 更新 template 的 who 与事件**

cover 分支：

```vue
<DollViewer
  ref="coverLeftRef"
  :who="coverLeftWho"
  size="equal"
  :rotating="rotating"
  :allow-zoom="false"
  :bubble="bubbleMap.czh"
  @select="onSelect"
  @dblselect="onDblSelect"
/>
<DollViewer
  ref="coverRightRef"
  :who="coverRightWho"
  size="equal"
  :rotating="rotating"
  :allow-zoom="false"
  :bubble="bubbleMap.hsj"
  @select="onSelect"
  @dblselect="onDblSelect"
/>
```

home 分支（aside **不**绑 `@dblselect`，双保险）：

```vue
<DollViewer
  ref="mainRef"
  class="duo__main"
  :who="mainVariantWho"
  size="focus"
  :rotating="rotating"
  :allow-zoom="false"
  :bubble="bubbleMap[mainWho]"
  @select="onSelect"
  @dblselect="onDblSelect"
/>
<DollViewer
  ref="asideRef"
  class="duo__aside"
  :who="asideVariantWho"
  size="aside"
  :rotating="rotating"
  :allow-zoom="false"
  :bubble="bubbleMap[asideWho]"
  @select="onSelect"
/>
```

- [ ] **Step 4: 浏览器验收**

```powershell
cd web; npm run dev
```

按 Spec §5：

1. 封面双击左/右：模型在默认 ↔ r 间切换，再双击切回  
2. 登录首页：仅中间大模型可切换；右下角小模型双击不换模  
3. 刷新后恢复默认  
4. 封面切过变体再登录进 `/app`，首页仍为默认；从首页回封面亦默认  
5. 单击气泡、轻跳、旋转/复位正常（单击气泡约延迟 300ms）

- [ ] **Step 5: Commit**

```powershell
git add web/src/components/DollDuoStage.vue
Set-Content -Path .git/COMMIT_MSG_TMP -Encoding utf8NoBOM -Value "feat(web): 舞台双击切换玩偶 r 变体"
D:\devTool\Git\cmd\git.exe commit -F .git/COMMIT_MSG_TMP
Remove-Item .git/COMMIT_MSG_TMP
```

---

## Spec Coverage（自审）

| Spec 要求 | 任务 |
|-----------|------|
| czh/hsj ↔ czhr/hsjr + 资源路径 | Task 1 |
| 封面两侧可双击 | Task 3 |
| home 仅大模型 | Task 3（onDblSelect + aside 不绑事件） |
| 不持久 / 不共用 | Task 3（组件内 `ref`） |
| 双击抑制单击气泡 | Task 2（延迟单击） |
| 气泡按 baseKey | Task 3 `onSelect` |
| README / glb | Task 1 |
| 不改 auth / 旋转 store | 未改动 |

## Placeholder 扫描

无 TBD/TODO；步骤含完整代码与 PowerShell 命令。
