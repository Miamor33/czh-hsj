<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import DollDuoStage from '../components/DollDuoStage.vue'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const error = ref('')
const home = ref(null)

const showManage = ref(false)
const anniversaries = ref([])
const manageLoading = ref(false)
const manageError = ref('')

const togetherDate = ref('')
const formTitle = ref('')
const formDate = ref('')
const formYearly = ref(true)
const editingId = ref(null)

async function loadHome() {
  loading.value = true
  error.value = ''
  try {
    home.value = await http.get('/home')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

async function openManage() {
  showManage.value = true
  manageError.value = ''
  await loadAnniversaries()
}

function closeManage() {
  showManage.value = false
  resetForm()
}

async function loadAnniversaries() {
  manageLoading.value = true
  try {
    const list = await http.get('/anniversaries')
    anniversaries.value = list
    if (home.value?.togetherDate) {
      togetherDate.value = home.value.togetherDate
    }
  } catch (e) {
    manageError.value = e.message
  } finally {
    manageLoading.value = false
  }
}

function resetForm() {
  editingId.value = null
  formTitle.value = ''
  formDate.value = ''
  formYearly.value = true
}

function startEdit(item) {
  editingId.value = item.id
  formTitle.value = item.title
  formDate.value = item.eventDate
  formYearly.value = item.yearly ?? true
}

async function saveAnniversary() {
  if (!formTitle.value || !formDate.value) {
    manageError.value = '请填写标题和日期'
    return
  }
  manageLoading.value = true
  manageError.value = ''
  try {
    const body = {
      title: formTitle.value,
      eventDate: formDate.value,
      yearly: formYearly.value,
    }
    if (editingId.value) {
      await http.put(`/anniversaries/${editingId.value}`, body)
    } else {
      await http.post('/anniversaries', body)
    }
    resetForm()
    await loadAnniversaries()
    await loadHome()
  } catch (e) {
    manageError.value = e.message
  } finally {
    manageLoading.value = false
  }
}

async function deleteAnniversary(id) {
  if (!confirm('确定删除这个纪念日？')) return
  manageLoading.value = true
  try {
    await http.delete(`/anniversaries/${id}`)
    if (editingId.value === id) resetForm()
    await loadAnniversaries()
    await loadHome()
  } catch (e) {
    manageError.value = e.message
  } finally {
    manageLoading.value = false
  }
}

async function saveTogetherDate() {
  if (!togetherDate.value) return
  manageLoading.value = true
  manageError.value = ''
  try {
    await http.put('/anniversaries/together-date', { togetherDate: togetherDate.value })
    await loadHome()
  } catch (e) {
    manageError.value = e.message
  } finally {
    manageLoading.value = false
  }
}

function moduleProgress(m) {
  const total = m.targetCount || m.totalItems || 1
  return Math.min(100, Math.round((m.completedCount / total) * 100))
}

function formatDaysLeft(days) {
  if (days === 0) return '今天'
  return `${days} 天后`
}

onMounted(loadHome)
</script>

<template>
  <div class="home-page">
    <div v-if="loading" class="loading">加载中…</div>
    <p v-else-if="error" class="error-banner">{{ error }}</p>

    <template v-else-if="home">
      <DollDuoStage mode="home" :focus-key="auth.partnerKey" :self-name="auth.displayName" />

      <section class="card home-days">
        <p class="stat-label">相爱第</p>
        <p class="stat-num">{{ home.loveDays }}</p>
        <p class="stat-label">天 · 始于 {{ home.togetherDate }}</p>
      </section>

      <section class="card">
        <div class="flex-between mb-md">
          <p class="section-title" style="margin: 0">即将到来</p>
          <button class="btn btn--ghost btn--sm" @click="openManage">管理纪念日</button>
        </div>
        <div v-if="home.upcomingAnniversaries?.length">
          <div
            v-for="item in home.upcomingAnniversaries.slice(0, 5)"
            :key="item.id"
            class="list-item flex-between"
          >
            <div>
              <p class="ann-title">{{ item.title }}</p>
              <p class="ann-meta">{{ item.nextDate }}</p>
            </div>
            <span class="chip">{{ formatDaysLeft(item.daysLeft) }}</span>
          </div>
        </div>
        <p v-else class="empty" style="padding: 16px">暂无纪念日</p>
      </section>

      <section class="card" @click="router.push('/app/challenges')">
        <p class="section-title">挑战进度</p>
        <div
          v-for="m in home.challengeModules"
          :key="m.moduleKey"
          class="challenge-row"
        >
          <div class="flex-between">
            <span>{{ m.title }}</span>
            <span class="challenge-count">{{ m.completedCount }}/{{ m.targetCount || m.totalItems }}</span>
          </div>
          <div class="progress-bar">
            <div class="progress-bar__fill" :style="{ width: moduleProgress(m) + '%' }" />
          </div>
        </div>
      </section>

      <section class="card qa-card" @click="router.push('/app/qa')">
        <div class="flex-between">
          <div>
            <p class="section-title" style="margin: 0">待答问题</p>
            <p class="qa-hint">互相了解的小问答</p>
          </div>
          <span v-if="home.pendingQuestions > 0" class="qa-badge">{{ home.pendingQuestions }}</span>
          <span v-else class="chip chip--done">全部完成</span>
        </div>
      </section>
    </template>

    <div v-if="showManage" class="modal-overlay" @click.self="closeManage">
      <div class="modal manage-modal">
        <div class="modal__header">
          <h2 class="modal__title">纪念日管理</h2>
          <button class="btn btn--text" @click="closeManage">关闭</button>
        </div>
        <p v-if="manageError" class="error-banner">{{ manageError }}</p>

        <div class="field">
          <label>在一起日期</label>
          <div class="flex-gap">
            <input v-model="togetherDate" type="date" style="flex: 1" />
            <button class="btn btn--ghost btn--sm" :disabled="manageLoading" @click="saveTogetherDate">
              保存
            </button>
          </div>
        </div>

        <hr class="divider" />

        <p class="sub-label">{{ editingId ? '编辑纪念日' : '新增纪念日' }}</p>
        <div class="field">
          <label>标题</label>
          <input v-model="formTitle" placeholder="例如：第一次约会" />
        </div>
        <div class="field">
          <label>日期</label>
          <input v-model="formDate" type="date" />
        </div>
        <div class="field">
          <label class="checkbox-label">
            <input v-model="formYearly" type="checkbox" />
            每年重复
          </label>
        </div>
        <div class="flex-gap mb-md">
          <button class="btn btn--primary" :disabled="manageLoading" @click="saveAnniversary">
            {{ editingId ? '更新' : '添加' }}
          </button>
          <button v-if="editingId" class="btn btn--ghost" @click="resetForm">取消编辑</button>
        </div>

        <p class="sub-label">已有纪念日</p>
        <div v-if="manageLoading && !anniversaries.length" class="empty">加载中…</div>
        <div v-else-if="anniversaries.length">
          <div v-for="item in anniversaries" :key="item.id" class="manage-item flex-between">
            <div>
              <p>{{ item.title }}</p>
              <p class="ann-meta">{{ item.eventDate }}{{ item.yearly ? ' · 每年' : '' }}</p>
            </div>
            <div class="flex-gap">
              <button class="btn btn--text btn--sm" @click="startEdit(item)">编辑</button>
              <button class="btn btn--danger btn--sm" @click="deleteAnniversary(item.id)">删除</button>
            </div>
          </div>
        </div>
        <p v-else class="empty">还没有纪念日</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-days {
  text-align: center;
  padding: 28px 16px 24px;
  background:
    radial-gradient(80% 90% at 100% 0%, rgba(255, 143, 171, 0.22), transparent 55%),
    radial-gradient(70% 80% at 0% 100%, rgba(91, 127, 209, 0.14), transparent 50%),
    radial-gradient(40% 40% at 50% 50%, rgba(255, 214, 165, 0.25), transparent 70%),
    linear-gradient(165deg, rgba(255, 255, 255, 0.98), rgba(255, 240, 244, 0.94));
  border-color: rgba(255, 143, 171, 0.35);
}

.home-days .stat-num {
  background: linear-gradient(120deg, var(--him) 10%, var(--blush) 50%, var(--coral-deep) 95%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: heart-pulse 3s ease-in-out infinite;
}

.ann-title {
  font-weight: 700;
  font-family: var(--font-display);
}

.ann-meta {
  font-size: 0.8125rem;
  color: var(--ink-muted);
}

.challenge-row {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(255, 194, 209, 0.2), rgba(168, 189, 232, 0.18));
  border: 1.5px solid rgba(255, 255, 255, 0.7);
}

.challenge-row:nth-child(2) {
  background: linear-gradient(135deg, rgba(184, 242, 230, 0.3), rgba(255, 243, 176, 0.25));
}

.challenge-row:nth-child(3) {
  background: linear-gradient(135deg, rgba(168, 189, 232, 0.28), rgba(255, 194, 209, 0.2));
}

.challenge-row:last-child {
  margin-bottom: 0;
}

.challenge-count {
  font-size: 0.8125rem;
  color: var(--coral-deep);
  font-weight: 800;
}

.qa-card {
  cursor: pointer;
  background:
    linear-gradient(135deg, rgba(255, 143, 171, 0.14), transparent 48%),
    linear-gradient(165deg, rgba(255, 255, 255, 0.97), rgba(245, 248, 255, 0.94));
}

.qa-hint {
  font-size: 0.8125rem;
  color: var(--ink-muted);
  margin-top: 4px;
}

.qa-badge {
  min-width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--blush), var(--coral));
  color: #fff;
  font-size: 0.875rem;
  font-weight: 800;
  box-shadow: 0 4px 0 rgba(61, 95, 173, 0.2), 0 8px 16px rgba(255, 143, 171, 0.35);
  animation: heart-pulse 2s ease-in-out infinite;
}

.manage-modal {
  max-height: 90dvh;
}

.divider {
  border: none;
  border-top: 2px dashed rgba(255, 194, 209, 0.55);
  margin: 16px 0;
}

.sub-label {
  font-size: 0.8125rem;
  color: var(--ink-muted);
  margin-bottom: 10px;
  font-weight: 700;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox-label input {
  width: auto;
}

.manage-item {
  padding: 10px 0;
  border-bottom: 1px dashed rgba(255, 194, 209, 0.45);
}

.manage-item:last-child {
  border-bottom: none;
}
</style>
