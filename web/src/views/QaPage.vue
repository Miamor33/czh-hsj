<script setup>
import { ref, onMounted } from 'vue'
import http from '../api/http'

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

const answeringId = ref(null)
const answerText = ref('')
const answerLoading = ref(false)

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

function startAnswer(item) {
  answeringId.value = item.id
  answerText.value = item.myAnswer || ''
}

function cancelAnswer() {
  answeringId.value = null
  answerText.value = ''
}

async function submitAnswer(id) {
  if (!answerText.value.trim()) {
    error.value = '请输入答案'
    return
  }
  answerLoading.value = true
  error.value = ''
  try {
    await http.post(`/qa/questions/${id}/answer`, {
      content: answerText.value.trim(),
    })
    cancelAnswer()
    await loadQuestions()
  } catch (e) {
    error.value = e.message
  } finally {
    answerLoading.value = false
  }
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
      <div v-for="item in questions" :key="item.id" class="card qa-item">
        <div class="flex-between mb-md">
          <span :class="['chip', statusClass(item.status)]">{{ statusLabel(item.status) }}</span>
        </div>
        <p class="qa-question">{{ item.content }}</p>

        <div v-if="item.myAnswer" class="qa-answer qa-answer--mine">
          <p class="qa-answer__label">我的回答</p>
          <p>{{ item.myAnswer }}</p>
        </div>

        <div v-if="item.status === 'done' && item.otherAnswer" class="qa-answer qa-answer--other">
          <p class="qa-answer__label">{{ item.otherName || '对方' }}的回答</p>
          <p>{{ item.otherAnswer }}</p>
        </div>
        <p v-else-if="item.status === 'pending_other' && item.myAnswer" class="qa-wait">
          已回答，等待对方…
        </p>

        <div v-if="item.status === 'pending_me'" class="mt-md">
          <div v-if="answeringId === item.id">
            <div class="field">
              <textarea v-model="answerText" placeholder="写下你的回答…" />
            </div>
            <div class="flex-gap">
              <button
                class="btn btn--primary btn--sm"
                :disabled="answerLoading"
                @click="submitAnswer(item.id)"
              >
                提交
              </button>
              <button class="btn btn--ghost btn--sm" @click="cancelAnswer">取消</button>
            </div>
          </div>
          <button v-else class="btn btn--primary btn--sm" @click="startAnswer(item)">
            回答
          </button>
        </div>
      </div>
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
  margin-bottom: 12px;
}

.qa-question {
  font-size: 1rem;
  font-weight: 500;
  line-height: 1.6;
}

.qa-answer {
  margin-top: 12px;
  padding: 12px;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
}

.qa-answer--mine {
  background: rgba(224, 139, 122, 0.1);
}

.qa-answer--other {
  background: rgba(42, 37, 32, 0.05);
}

.qa-answer__label {
  font-size: 0.75rem;
  color: var(--ink-muted);
  margin-bottom: 4px;
}

.qa-wait {
  margin-top: 12px;
  font-size: 0.8125rem;
  color: var(--ink-muted);
  font-style: italic;
}
</style>
