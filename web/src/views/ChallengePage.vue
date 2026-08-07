<script setup>
import { ref, onMounted, watch } from 'vue'
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
const completePhoto = ref(null)
const completeLoading = ref(false)

const showAddItem = ref(false)
const newItemTitle = ref('')
const addLoading = ref(false)

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
  completePhoto.value = null
}

function cancelComplete() {
  completingId.value = null
  completeNote.value = ''
  completePhoto.value = null
}

function onPhotoSelect(event) {
  completePhoto.value = event.target.files?.[0] || null
}

async function submitComplete(itemId) {
  completeLoading.value = true
  error.value = ''
  try {
    const form = new FormData()
    if (completeNote.value.trim()) {
      form.append('note', completeNote.value.trim())
    }
    if (completePhoto.value) {
      form.append('photo', completePhoto.value)
    }
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
          <img
            v-if="item.photoUrl"
            :src="item.photoUrl"
            alt="完成照片"
            class="item-photo"
            loading="lazy"
          />
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
              <label>照片（可选）</label>
              <input type="file" accept="image/*" @change="onPhotoSelect" />
            </div>
            <div class="flex-gap">
              <button
                class="btn btn--primary btn--sm"
                :disabled="completeLoading"
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
  border: 1px solid rgba(42, 37, 32, 0.08);
  transition: border-color 0.2s, box-shadow 0.2s;
}

.module-chip.active {
  border-color: var(--coral-soft);
  box-shadow: 0 4px 20px rgba(224, 139, 122, 0.2);
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

.item-photo {
  margin-top: 10px;
  width: 100%;
  max-height: 200px;
  object-fit: cover;
  border-radius: var(--radius-sm);
}
</style>
