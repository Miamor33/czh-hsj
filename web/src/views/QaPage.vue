<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'

const router = useRouter()

const filters = [
  { key: 'pending_me', label: '待我回答' },
  { key: 'pending_other', label: '等对方' },
  { key: 'done', label: '已完成' },
  { key: 'all', label: '全部' },
]

const loading = ref(true)
const error = ref('')
const questions = ref([])
const activeFilter = ref('pending_me')

const showAdd = ref(false)
const newQuestion = ref('')
const addLoading = ref(false)

async function loadQuestions() {
  loading.value = true
  error.value = ''
  try {
    questions.value = await http.get('/qa', {
      params: { filter: activeFilter.value },
    })
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function setFilter(key) {
  activeFilter.value = key
  loadQuestions()
}

function statusLabel(status) {
  const map = {
    pending_me: '待我回答',
    pending_other: '等对方',
    done: '已完成',
  }
  return map[status] || status
}

function statusClass(status) {
  if (status === 'done') return 'chip--done'
  return 'chip--pending'
}

function openDetail(item) {
  router.push(`/app/qa/${item.id}`)
}

async function submitQuestion() {
  if (!newQuestion.value.trim()) {
    error.value = '请输入问题'
    return
  }
  addLoading.value = true
  error.value = ''
  try {
    await http.post('/qa/questions', { content: newQuestion.value.trim() })
    newQuestion.value = ''
    showAdd.value = false
    activeFilter.value = 'all'
    await loadQuestions()
  } catch (e) {
    error.value = e.message
  } finally {
    addLoading.value = false
  }
}

onMounted(loadQuestions)
</script>

<template>
  <div class="qa-page">
    <div class="flex-between mb-md">
      <p class="section-title" style="margin: 0">情侣问答</p>
      <button class="btn btn--primary btn--sm" @click="showAdd = true">出题</button>
    </div>

    <div class="filter-row">
      <button
        v-for="f in filters"
        :key="f.key"
        class="filter-btn"
        :class="{ active: activeFilter === f.key }"
        @click="setFilter(f.key)"
      >
        {{ f.label }}
      </button>
    </div>

    <p v-if="error" class="error-banner">{{ error }}</p>
    <div v-if="loading" class="loading">加载中…</div>

    <div v-else-if="questions.length">
      <button
        v-for="item in questions"
        :key="item.id"
        type="button"
        class="card qa-item"
        @click="openDetail(item)"
      >
        <div class="qa-item__top">
          <div class="qa-item__left">
            <span class="qa-item__badge">Q{{ item.questionIndex }}</span>
            <span :class="['chip', statusClass(item.status)]">{{ statusLabel(item.status) }}</span>
          </div>
          <span class="qa-item__arrow" aria-hidden="true">›</span>
        </div>
        <p class="qa-question">{{ item.content }}</p>
      </button>
    </div>
    <p v-else class="empty">暂无问题</p>

    <div v-if="showAdd" class="modal-overlay" @click.self="showAdd = false">
      <div class="modal">
        <div class="modal__header">
          <h2 class="modal__title">出一道题</h2>
          <button class="btn btn--text" @click="showAdd = false">关闭</button>
        </div>
        <div class="field">
          <label>问题内容</label>
          <textarea v-model="newQuestion" placeholder="想更了解对方什么？" />
        </div>
        <button
          class="btn btn--primary"
          style="width: 100%"
          :disabled="addLoading"
          @click="submitQuestion"
        >
          {{ addLoading ? '提交中…' : '发布问题' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.qa-item {
  display: block;
  width: 100%;
  text-align: left;
  margin-bottom: 0;
  cursor: pointer;
  border: none;
  font: inherit;
  color: inherit;
}

.qa-item + .qa-item {
  margin-top: 12px;
}

.qa-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.qa-item__left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qa-item__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 24px;
  padding: 0 8px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--him) 0%, var(--coral) 100%);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.qa-item__arrow {
  color: var(--coral-soft);
  font-size: 1.25rem;
  line-height: 1;
}

.qa-question {
  font-size: 1rem;
  font-weight: 500;
  line-height: 1.6;
  margin: 0;
  color: var(--him);
  padding-left: 4px;
}
</style>
