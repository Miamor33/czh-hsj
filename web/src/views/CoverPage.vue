<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'
import DollDuoStage from '../components/DollDuoStage.vue'

const router = useRouter()
const auth = useAuthStore()

const loading = ref(true)
const cover = ref(null)
const showLogin = ref(false)
const loginPartner = ref('czh')
const loginPassword = ref('')
const loginError = ref('')
const loginLoading = ref(false)

async function loadCover() {
  loading.value = true
  try {
    cover.value = await http.get('/public/cover')
  } catch {
    cover.value = null
  } finally {
    loading.value = false
  }
}

function openLogin() {
  showLogin.value = true
  loginError.value = ''
  loginPassword.value = ''
}

function closeLogin() {
  showLogin.value = false
}

async function handleLogin() {
  if (!loginPassword.value) {
    loginError.value = '请输入密码'
    return
  }
  loginLoading.value = true
  loginError.value = ''
  try {
    await auth.login(loginPartner.value, loginPassword.value)
    closeLogin()
    router.push('/app')
  } catch (e) {
    loginError.value = e.message
  } finally {
    loginLoading.value = false
  }
}

function formatDaysLeft(days) {
  if (days === 0) return '就是今天'
  if (days === 1) return '还有 1 天'
  return `还有 ${days} 天`
}

onMounted(loadCover)
</script>

<template>
  <div class="cover-page">
    <div class="cover-atmosphere" aria-hidden="true" />

    <div v-if="loading" class="loading">加载中…</div>

    <template v-else-if="cover">
      <header class="cover-hero">
        <p class="cover-hero__sub">我们在一起</p>
        <h1 class="cover-hero__brand" aria-label="czh and hsj">
          <span class="pair--him">czh</span>
          <span class="pair__amp">&</span>
          <span class="pair--her">hsj</span>
        </h1>
        <div class="cover-hero__days">
          <span class="stat-num">{{ cover.loveDays }}</span>
          <span class="cover-hero__days-unit">天</span>
        </div>
      </header>

      <DollDuoStage mode="cover" />

      <section v-if="cover.nextAnniversary" class="card cover-next">
        <p class="section-title">下一个纪念日</p>
        <p class="cover-next__title">{{ cover.nextAnniversary.title }}</p>
        <p class="cover-next__meta">
          {{ cover.nextAnniversary.nextDate }}
          · {{ formatDaysLeft(cover.nextAnniversary.daysLeft) }}
        </p>
      </section>

      <section v-if="cover.featuredPhotos?.length" class="card">
        <p class="section-title">精选瞬间</p>
        <div class="featured-scroll">
          <div
            v-for="photo in cover.featuredPhotos"
            :key="photo.id"
            class="featured-item"
          >
            <img :src="photo.url" :alt="photo.caption || '照片'" loading="lazy" />
            <p v-if="photo.caption" class="featured-caption">{{ photo.caption }}</p>
          </div>
        </div>
      </section>
    </template>

    <div v-else class="empty">暂时无法加载封面</div>

    <button class="login-entry" aria-label="登录" @click="openLogin">
      <span class="login-entry__dot" />
    </button>

    <Teleport to="body">
      <div v-if="showLogin" class="modal-overlay" @click.self="closeLogin">
        <div class="modal">
          <div class="modal__header">
            <h2 class="modal__title">悄悄登录</h2>
            <button class="btn btn--text" @click="closeLogin">关闭</button>
          </div>
          <p v-if="loginError" class="error-banner">{{ loginError }}</p>
          <div class="field">
            <label>选择身份</label>
            <select v-model="loginPartner">
              <option value="czh">czh</option>
              <option value="hsj">hsj</option>
            </select>
          </div>
          <div class="field">
            <label>密码</label>
            <input
              v-model="loginPassword"
              type="password"
              placeholder="输入密码"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            />
          </div>
          <button
            class="btn btn--primary"
            style="width: 100%"
            :disabled="loginLoading"
            @click="handleLogin"
          >
            {{ loginLoading ? '登录中…' : '进入我们的空间' }}
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.cover-page {
  position: relative;
  min-height: 100dvh;
  padding: 0 0 48px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

.cover-atmosphere {
  position: absolute;
  inset: 0 0 auto 0;
  height: min(78vh, 620px);
  background:
    linear-gradient(180deg, rgba(244, 246, 250, 0) 48%, #eef1f7 100%),
    radial-gradient(70% 55% at 82% 22%, rgba(74, 111, 181, 0.45), transparent 62%),
    radial-gradient(55% 45% at 12% 38%, rgba(26, 26, 31, 0.28), transparent 58%),
    linear-gradient(155deg, #121218 0%, #1e2a44 42%, #3d629f 78%, #7a9ad0 100%);
  opacity: 0.34;
  pointer-events: none;
  z-index: 0;
  animation: ambience-shift 14s ease-in-out infinite alternate;
}

.cover-atmosphere::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(115deg, transparent 40%, rgba(255, 255, 255, 0.08) 50%, transparent 60%);
  mix-blend-mode: soft-light;
}

.cover-page > :not(.cover-atmosphere):not(.login-entry) {
  position: relative;
  z-index: 1;
  margin-left: 20px;
  margin-right: 20px;
}

.cover-hero {
  text-align: center;
  padding: 52px 8px 28px;
  animation: cover-rise 0.7s ease both;
}

.cover-hero__brand {
  font-family: var(--font-display);
  font-size: clamp(2.8rem, 12vw, 3.8rem);
  line-height: 1.12;
  margin-bottom: 26px;
  font-weight: 600;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 0.35em;
}

.pair--him {
  color: var(--him);
  text-shadow: 0 8px 28px rgba(26, 26, 31, 0.12);
}

.pair--her {
  color: var(--coral-deep);
  text-shadow: 0 8px 28px rgba(42, 74, 140, 0.18);
}

.pair__amp {
  color: var(--ink-muted);
  font-weight: 500;
  font-size: 0.72em;
}

.cover-hero__sub {
  font-size: 0.8125rem;
  color: var(--ink-muted);
  letter-spacing: 0.32em;
  margin-bottom: 14px;
}

.cover-hero__days {
  display: inline-flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  padding: 14px 28px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.55);
  border: 1px solid rgba(42, 74, 140, 0.14);
  box-shadow: 0 10px 30px rgba(20, 20, 28, 0.06);
  backdrop-filter: blur(8px);
}

.cover-hero__days-unit {
  font-family: var(--font-display);
  font-size: 1.2rem;
  color: var(--coral-deep);
  font-weight: 600;
}

.cover-next {
  animation-delay: 0.12s;
}

.cover-next__title {
  font-size: 1.05rem;
  font-weight: 500;
  margin-bottom: 4px;
  color: var(--him);
}

.cover-next__meta {
  font-size: 0.875rem;
  color: var(--ink-muted);
}

.featured-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  padding-bottom: 4px;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.featured-scroll::-webkit-scrollbar {
  display: none;
}

.featured-item {
  flex-shrink: 0;
  width: 148px;
}

.featured-item img {
  width: 148px;
  height: 148px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
  border: 1px solid rgba(255, 255, 255, 0.5);
  transition: transform 0.25s ease;
}

.featured-item:active img {
  transform: scale(0.98);
}

.featured-caption {
  font-size: 0.75rem;
  color: var(--ink-muted);
  margin-top: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-entry {
  position: fixed;
  bottom: calc(24px + env(safe-area-inset-bottom, 0px));
  right: 20px;
  z-index: 20;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(26, 26, 31, 0.1);
  box-shadow: 0 8px 24px rgba(20, 20, 28, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.6;
  transition: opacity 0.2s, transform 0.2s;
  margin: 0 !important;
}

.login-entry:active {
  opacity: 1;
  transform: scale(0.96);
}

.login-entry__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--him) 35%, var(--coral) 100%);
  box-shadow: 0 0 0 3px rgba(74, 111, 181, 0.15);
}

@keyframes cover-rise {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes ambience-shift {
  from { filter: saturate(1) hue-rotate(0deg); }
  to { filter: saturate(1.08) hue-rotate(-6deg); }
}
</style>
