<script setup>
import { computed, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '../api/http'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const tip = ref('')
const detail = ref(null)

/** answer | reply | null */
const composingMode = ref(null)
const draft = ref('')
const submitting = ref(false)
const nudging = ref(false)

const questionId = computed(() => Number(route.params.id))

function formatDate(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value).slice(0, 10)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function formatDateTime(value) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value).replace('T', ' ').slice(0, 19)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  const ss = String(d.getSeconds()).padStart(2, '0')
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`
}

async function loadDetail() {
  loading.value = true
  error.value = ''
  tip.value = ''
  try {
    detail.value = await http.get(`/qa/questions/${questionId.value}`)
  } catch (e) {
    error.value = e.message
    detail.value = null
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push({ name: 'qa' })
}

function openAnswerComposer() {
  if (detail.value?.status !== 'pending_me') return
  composingMode.value = 'answer'
  draft.value = detail.value.myAnswer || ''
}

function openReplyComposer() {
  if (detail.value?.status !== 'done') return
  composingMode.value = 'reply'
  draft.value = ''
}

function closeComposer() {
  composingMode.value = null
  draft.value = ''
}

async function submit() {
  const text = draft.value.trim()
  if (!text) {
    error.value = composingMode.value === 'answer' ? '请输入答案' : '请输入回复'
    return
  }
  submitting.value = true
  error.value = ''
  tip.value = ''
  try {
    if (composingMode.value === 'answer') {
      await http.post(`/qa/questions/${questionId.value}/answer`, { content: text })
    } else if (composingMode.value === 'reply') {
      await http.post(`/qa/questions/${questionId.value}/replies`, { content: text })
    }
    closeComposer()
    await loadDetail()
  } catch (e) {
    error.value = e.message
  } finally {
    submitting.value = false
  }
}

async function nudgeOther() {
  nudging.value = true
  error.value = ''
  tip.value = ''
  try {
    await http.post(`/qa/questions/${questionId.value}/nudge`)
    tip.value = '已催一下对方'
    await loadDetail()
  } catch (e) {
    error.value = e.message
  } finally {
    nudging.value = false
  }
}

const showDivider = computed(() => {
  const s = detail.value?.status
  return s === 'pending_other' || s === 'done'
})

watch(questionId, () => {
  closeComposer()
  loadDetail()
})
onMounted(loadDetail)
</script>

<template>
  <div class="qa-detail">
    <header class="qa-detail__header">
      <button type="button" class="qa-detail__back" aria-label="返回" @click="goBack">‹</button>
      <h1 class="qa-detail__title">情侣问答</h1>
      <span class="qa-detail__header-spacer" />
    </header>

    <p v-if="error" class="error-banner">{{ error }}</p>
    <p v-else-if="tip" class="tip-banner">{{ tip }}</p>
    <div v-if="loading" class="loading">加载中…</div>

    <template v-else-if="detail">
      <div class="qa-detail__body">
        <div class="qa-detail__badge">Q{{ detail.questionIndex }}</div>
        <p class="qa-detail__question">{{ detail.content }}</p>
        <p class="qa-detail__meta">
          第{{ detail.questionIndex }}个问题
          <span v-if="detail.createdAt"> {{ formatDate(detail.createdAt) }}</span>
        </p>

        <p v-if="detail.nudgedMe && detail.status === 'pending_me'" class="qa-detail__nudge-tip">
          对方催你回答啦
        </p>

        <!-- 上方：首答区，待我答时可点击作答 -->
        <div
          class="qa-detail__upper"
          :class="{ 'qa-detail__upper--tap': detail.status === 'pending_me' && composingMode !== 'answer' }"
          @click="detail.status === 'pending_me' && composingMode !== 'answer' && openAnswerComposer()"
        >
          <div v-if="detail.answers?.length" class="qa-detail__answers">
            <div
              v-for="(a, idx) in detail.answers"
              :key="idx"
              class="qa-first"
              :class="{ 'qa-first--mine': a.mine }"
            >
              <div class="qa-first__head">
                <span class="qa-first__name">{{ a.displayName }}</span>
                <span class="qa-first__time">{{ formatDateTime(a.createdAt) }}</span>
              </div>
              <p class="qa-first__content">{{ a.content }}</p>
            </div>
          </div>

          <div
            v-else-if="detail.status === 'pending_me' && composingMode !== 'answer'"
            class="qa-detail__answer-slot"
          >
            <p class="qa-detail__answer-slot-text">点击这里写下你的回答</p>
          </div>
        </div>

        <div
          v-if="composingMode === 'answer'"
          class="qa-detail__inline-composer"
          @click.stop
        >
          <textarea v-model="draft" rows="4" placeholder="写下你的回答…" autofocus />
          <div class="qa-detail__composer-actions">
            <button type="button" class="btn btn--ghost btn--sm" @click="closeComposer">取消</button>
            <button
              type="button"
              class="btn btn--primary btn--sm"
              :disabled="submitting"
              @click="submit"
            >
              {{ submitting ? '提交中…' : '提交回答' }}
            </button>
          </div>
        </div>

        <div v-if="showDivider" class="qa-detail__divider" />

        <!-- 下方：等对方时催一催；双方完成则追聊 -->
        <div v-if="detail.status === 'pending_other'" class="qa-detail__below">
          <p class="qa-detail__wait">已回答，等待对方…</p>
          <button
            type="button"
            class="qa-detail__nudge"
            :disabled="nudging || detail.alreadyNudged"
            @click="nudgeOther"
          >
            {{ detail.alreadyNudged ? '已催过对方' : nudging ? '催一下…' : '催一催对方' }}
          </button>
        </div>

        <template v-else-if="detail.status === 'done'">
          <div v-for="r in detail.replies || []" :key="r.id" class="qa-chat">
            <div class="qa-chat__head">
              <span>{{ r.displayName }}</span>
              <span>{{ formatDateTime(r.createdAt) }}</span>
            </div>
            <div class="qa-chat__bubble" :class="{ 'qa-chat__bubble--mine': r.mine }">
              {{ r.content }}
            </div>
          </div>

          <div v-if="composingMode === 'reply'" class="qa-detail__inline-composer">
            <textarea v-model="draft" rows="3" placeholder="继续说点什么…" />
            <div class="qa-detail__composer-actions">
              <button type="button" class="btn btn--ghost btn--sm" @click="closeComposer">取消</button>
              <button
                type="button"
                class="btn btn--primary btn--sm"
                :disabled="submitting"
                @click="submit"
              >
                {{ submitting ? '发送中…' : '发送' }}
              </button>
            </div>
          </div>
        </template>
      </div>

      <div v-if="detail.status === 'done' && composingMode !== 'reply'" class="qa-detail__footer">
        <button type="button" class="qa-detail__action" @click="openReplyComposer">
          继续交流
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.qa-detail {
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
}

.qa-detail__header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: grid;
  grid-template-columns: 44px 1fr 44px;
  align-items: center;
  padding: 10px 8px;
  background: rgba(249, 250, 253, 0.9);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(26, 26, 31, 0.07);
}

.qa-detail__back {
  border: none;
  background: transparent;
  font-size: 1.75rem;
  line-height: 1;
  color: var(--him);
  cursor: pointer;
  padding: 4px 8px;
}

.qa-detail__title {
  margin: 0;
  text-align: center;
  font-family: var(--font-display);
  font-size: 1.05rem;
  font-weight: 600;
  background: linear-gradient(105deg, var(--him) 35%, var(--coral-deep) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.qa-detail__header-spacer {
  width: 44px;
}

.tip-banner {
  margin: 8px 16px 0;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  background: rgba(74, 111, 181, 0.1);
  border: 1px solid rgba(74, 111, 181, 0.14);
  color: var(--coral-deep);
  font-size: 0.875rem;
}

.qa-detail__body {
  flex: 1;
  padding: 20px 16px 120px;
}

.qa-detail__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 42px;
  height: 28px;
  padding: 0 10px;
  margin-bottom: 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--him) 0%, var(--coral) 100%);
  color: #fff;
  font-size: 0.8rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.qa-detail__question {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.18rem;
  font-weight: 600;
  line-height: 1.55;
  color: var(--him);
}

.qa-detail__meta {
  margin: 10px 0 0;
  font-size: 0.8125rem;
  color: var(--ink-muted);
}

.qa-detail__nudge-tip {
  margin: 14px 0 0;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  background: rgba(74, 111, 181, 0.1);
  border: 1px solid rgba(74, 111, 181, 0.14);
  font-size: 0.875rem;
  color: var(--coral-deep);
}

.qa-detail__upper {
  margin-top: 22px;
  min-height: 72px;
}

.qa-detail__upper--tap {
  cursor: pointer;
  border-radius: var(--radius);
}

.qa-detail__answer-slot {
  padding: 28px 16px;
  border: 1px dashed rgba(74, 111, 181, 0.35);
  border-radius: var(--radius);
  text-align: center;
  background: rgba(255, 255, 255, 0.55);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.qa-detail__answer-slot-text {
  margin: 0;
  color: var(--coral-deep);
  font-size: 0.9rem;
}

.qa-detail__answers {
  margin-top: 4px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.qa-first {
  padding: 14px 16px;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(42, 74, 140, 0.1);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.85);
}

.qa-first--mine {
  background: rgba(74, 111, 181, 0.1);
  border-color: rgba(74, 111, 181, 0.16);
}

.qa-first__head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 8px;
}

.qa-first__name {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--him);
}

.qa-first__time {
  font-size: 0.75rem;
  color: var(--ink-muted);
}

.qa-first__content {
  margin: 0;
  font-size: 0.95rem;
  line-height: 1.65;
  color: var(--ink-soft);
  white-space: pre-wrap;
}

.qa-detail__inline-composer {
  margin-top: 12px;
  background: linear-gradient(165deg, rgba(255, 255, 255, 0.96) 0%, rgba(248, 250, 253, 0.92) 100%);
  border-radius: var(--radius);
  padding: 12px 14px;
  border: 1px solid rgba(42, 74, 140, 0.12);
  box-shadow: var(--shadow);
}

.qa-detail__inline-composer textarea {
  width: 100%;
  border: none;
  resize: vertical;
  font: inherit;
  outline: none;
  background: transparent;
  color: var(--ink);
}

.qa-detail__composer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.qa-detail__divider {
  height: 1px;
  margin: 18px 0 20px;
  background: linear-gradient(90deg, transparent, rgba(42, 74, 140, 0.28), transparent);
}

.qa-detail__below {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 8px 0 24px;
}

.qa-detail__wait {
  margin: 0;
  font-size: 0.875rem;
  color: var(--ink-muted);
}

.qa-detail__nudge {
  min-width: 168px;
  border: none;
  border-radius: 999px;
  padding: 12px 22px;
  background: linear-gradient(135deg, var(--coral) 0%, var(--coral-deep) 100%);
  color: #fff;
  font-size: 0.95rem;
  font-weight: 500;
  box-shadow: 0 8px 20px rgba(42, 74, 140, 0.22);
  cursor: pointer;
}

.qa-detail__nudge:disabled {
  opacity: 0.55;
  box-shadow: none;
  cursor: default;
  background: rgba(42, 74, 140, 0.2);
  color: var(--coral-deep);
}

.qa-chat {
  margin-bottom: 16px;
}

.qa-chat__head {
  display: flex;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 0.75rem;
  color: var(--ink-muted);
}

.qa-chat__bubble {
  display: inline-block;
  max-width: 100%;
  padding: 10px 14px;
  border-radius: 14px;
  background: rgba(74, 111, 181, 0.12);
  border: 1px solid rgba(74, 111, 181, 0.12);
  font-size: 0.95rem;
  line-height: 1.55;
  color: var(--ink-soft);
  white-space: pre-wrap;
}

.qa-chat__bubble--mine {
  background: rgba(26, 26, 31, 0.06);
  border-color: rgba(26, 26, 31, 0.08);
}

.qa-detail__footer {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 30;
  padding: 12px 16px calc(12px + var(--safe-bottom, 0px));
  background: linear-gradient(180deg, transparent, rgba(238, 241, 247, 0.92) 32%);
}

.qa-detail__action {
  width: 100%;
  border: none;
  border-radius: 999px;
  padding: 14px 16px;
  background: linear-gradient(135deg, var(--coral) 0%, var(--coral-deep) 100%);
  color: #fff;
  font-size: 0.95rem;
  font-weight: 500;
  box-shadow: 0 10px 24px rgba(42, 74, 140, 0.22);
  cursor: pointer;
}

.qa-detail__action:active {
  transform: scale(0.98);
}
</style>
