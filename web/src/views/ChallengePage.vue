<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import http from '../api/http'

const moduleLabels = {
  little_things: '100件小事',
  cities_70: '70个城市',
  movie_tickets: '电影票根',
}

const modules = ref([])
const activeModule = ref('')
const items = ref([])
const loadingModules = ref(true)
const loadingItems = ref(false)
const error = ref('')

const completingId = ref(null)
const completeNote = ref('')
const completePhotos = ref([])
const completePreviewUrls = ref([])
const completeLoading = ref(false)
const lightboxUrl = ref('')
const fileInputRef = ref(null)

const showAddItem = ref(false)
const newItemTitle = ref('')
const addLoading = ref(false)

function revokePreviews() {
  completePreviewUrls.value.forEach((u) => URL.revokeObjectURL(u))
  completePreviewUrls.value = []
}

async function loadModules() {
  loadingModules.value = true
  error.value = ''
  try {
    modules.value = await http.get('/challenges/modules')
    if (modules.value.length && !activeModule.value) {
      activeModule.value = modules.value[0].moduleKey
    }
  } catch (e) {
    error.value = e.message
  } finally {
    loadingModules.value = false
  }
}

async function loadItems() {
  if (!activeModule.value) return
  loadingItems.value = true
  error.value = ''
  try {
    items.value = await http.get(`/challenges/modules/${activeModule.value}/items`)
  } catch (e) {
    error.value = e.message
  } finally {
    loadingItems.value = false
  }
}

function moduleLabel(key) {
  return moduleLabels[key] || key
}

function moduleProgress(m) {
  const total = m.targetCount || m.totalItems || 1
  return Math.min(100, Math.round((m.completedCount / total) * 100))
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

function openPhotoPicker() {
  if (completePhotos.value.length >= 3) {
    error.value = '最多上传 3 张照片'
    return
  }
  const el = fileInputRef.value
  const input = Array.isArray(el) ? el[0] : el
  input?.click()
}

function onPhotoSelect(event) {
  const files = Array.from(event.target.files || [])
  event.target.value = ''
  if (!files.length) return
  const merged = [...completePhotos.value, ...files]
  if (merged.length > 3) {
    error.value = '最多上传 3 张照片'
  } else if (error.value === '最多上传 3 张照片') {
    error.value = ''
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
  if (error.value === '最多上传 3 张照片') {
    error.value = ''
  }
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
      timeout: 60000,
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

async function uncomplete(item) {
  if (!confirm('确定取消完成状态？')) return
  try {
    await http.delete(`/challenges/items/${item.id}/complete`)
    await loadModules()
    await loadItems()
  } catch (e) {
    error.value = e.message
  }
}

async function addItem() {
  if (!newItemTitle.value.trim()) {
    error.value = '请输入标题'
    return
  }
  addLoading.value = true
  try {
    await http.post(`/challenges/modules/${activeModule.value}/items`, {
      title: newItemTitle.value.trim(),
    })
    newItemTitle.value = ''
    showAddItem.value = false
    await loadModules()
    await loadItems()
  } catch (e) {
    error.value = e.message
  } finally {
    addLoading.value = false
  }
}

watch(activeModule, loadItems)

onMounted(async () => {
  await loadModules()
  await loadItems()
})

onUnmounted(revokePreviews)
</script>

<template>
  <div class="challenge-page">
    <p class="section-title">我们的挑战</p>

    <div v-if="loadingModules" class="loading">加载中…</div>

    <div v-else class="module-picker">
      <button
        v-for="m in modules"
        :key="m.moduleKey"
        class="module-chip"
        :class="{ active: activeModule === m.moduleKey }"
        @click="activeModule = m.moduleKey"
      >
        <span class="module-chip__title">{{ moduleLabel(m.moduleKey) }}</span>
        <span class="module-chip__progress">{{ m.completedCount }}/{{ m.targetCount || m.totalItems }}</span>
        <div class="progress-bar">
          <div class="progress-bar__fill" :style="{ width: moduleProgress(m) + '%' }" />
        </div>
      </button>
    </div>

    <div class="flex-between mb-md mt-md">
      <p class="sub-title">{{ moduleLabel(activeModule) }}清单</p>
      <button class="btn btn--ghost btn--sm" @click="showAddItem = true">添加</button>
    </div>

    <input
      ref="fileInputRef"
      type="file"
      accept="image/jpeg,image/png,image/webp,image/*"
      multiple
      class="sr-only"
      @change="onPhotoSelect"
    />

    <p v-if="error" class="error-banner">{{ error }}</p>
    <div v-if="loadingItems" class="loading">加载中…</div>

    <div v-else-if="items.length">
      <div
        v-for="item in items"
        :key="item.id"
        class="card challenge-item"
        :class="{ 'challenge-item--done': item.completed }"
      >
        <div class="flex-between">
          <p class="item-title">{{ item.title }}</p>
          <span v-if="item.completed" class="chip chip--done">已完成</span>
        </div>
        <p v-if="item.extraHint" class="item-hint">{{ item.extraHint }}</p>

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

        <template v-else>
          <div v-if="completingId === item.id" class="complete-form mt-md">
            <div class="field">
              <label>备注（可选）</label>
              <textarea v-model="completeNote" placeholder="记录一下…" />
            </div>
            <div class="field">
              <label>照片（必填，1–3 张，jpg/png/webp）</label>
              <div class="thumb-grid mt-sm">
                <div v-for="(url, idx) in completePreviewUrls" :key="url" class="thumb-wrap">
                  <img :src="url" class="thumb" alt="预览" />
                  <button type="button" class="thumb-remove" @click="removePhoto(idx)">×</button>
                </div>
                <button
                  v-if="completePhotos.length < 3"
                  type="button"
                  class="thumb-add"
                  @click="openPhotoPicker"
                >
                  <span class="thumb-add__plus">+</span>
                  <span class="thumb-add__hint">{{ completePhotos.length ? '继续添加' : '添加照片' }}</span>
                </button>
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
          </div>
          <button v-else class="btn btn--primary btn--sm mt-sm" @click="startComplete(item)">
            标记完成
          </button>
        </template>
      </div>
    </div>
    <p v-else class="empty">暂无挑战项</p>

    <div v-if="showAddItem" class="modal-overlay" @click.self="showAddItem = false">
      <div class="modal">
        <div class="modal__header">
          <h2 class="modal__title">添加挑战项</h2>
          <button class="btn btn--text" @click="showAddItem = false">关闭</button>
        </div>
        <div class="field">
          <label>标题</label>
          <input v-model="newItemTitle" placeholder="例如：一起看电影" />
        </div>
        <button
          class="btn btn--primary"
          style="width: 100%"
          :disabled="addLoading"
          @click="addItem"
        >
          {{ addLoading ? '添加中…' : '添加' }}
        </button>
      </div>
    </div>

    <div v-if="lightboxUrl" class="lightbox" @click.self="lightboxUrl = ''">
      <img :src="lightboxUrl" alt="查看大图" class="lightbox__img" />
      <button type="button" class="btn btn--ghost lightbox__close" @click="lightboxUrl = ''">关闭</button>
    </div>
  </div>
</template>

<style scoped>
.module-picker {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.module-chip {
  text-align: left;
  padding: 14px 16px;
  border-radius: var(--radius);
  background: var(--paper-card);
  border: 1px solid rgba(26, 26, 31, 0.08);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.module-chip.active {
  border-color: var(--coral-soft);
  box-shadow: 0 6px 22px rgba(42, 74, 140, 0.18);
}

.module-chip__title {
  display: block;
  font-weight: 500;
  margin-bottom: 4px;
}

.module-chip__progress {
  font-size: 0.8125rem;
  color: var(--ink-muted);
}

.sub-title {
  font-size: 0.9rem;
  color: var(--ink-soft);
}

.challenge-item {
  margin-bottom: 12px;
}

.challenge-item--done {
  opacity: 0.85;
}

.item-title {
  font-weight: 500;
}

.item-hint {
  font-size: 0.8125rem;
  color: var(--ink-muted);
  margin-top: 4px;
}

.item-note {
  margin-top: 10px;
  font-size: 0.9rem;
  color: var(--ink-soft);
}

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
  width: 80px;
  height: 80px;
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
  background: rgba(42, 37, 32, 0.72);
  color: #fff;
  line-height: 22px;
  font-size: 14px;
  cursor: pointer;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.thumb-add {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-sm);
  border: 1.5px dashed rgba(201, 109, 92, 0.45);
  background: rgba(224, 139, 122, 0.06);
  color: var(--coral-deep);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  transition: background 0.15s, border-color 0.15s;
}

.thumb-add:active {
  background: rgba(224, 139, 122, 0.14);
  border-color: var(--coral);
}

.thumb-add__plus {
  font-size: 1.35rem;
  line-height: 1;
  font-weight: 400;
}

.thumb-add__hint {
  font-size: 0.65rem;
  color: var(--ink-muted);
  line-height: 1.2;
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
</style>
