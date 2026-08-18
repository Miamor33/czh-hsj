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
  // 不重复且已过期：显示「X 天前」
  if (days < 0) return `已经过去 ${Math.abs(days)} 天`
  if (days === 1) return '还有 1 天'
  return `还有 ${days} 天`
}

onMounted(loadCover)
</script>

<template>
  <div class="cover-page">
    <div class="cover-atmosphere" aria-hidden="true" />
    <div class="cover-floats" aria-hidden="true">
      <span class="float float--1">♡</span>
      <span class="float float--2">✦</span>
      <span class="float float--3">♡</span>
      <span class="float float--4">★</span>
      <span class="float float--5">♡</span>
    </div>

    <div v-if="loading" class="loading">加载中…</div>

    <template v-else-if="cover">
      <header class="cover-hero">
        <p class="cover-hero__sub">我们在一起</p>
        <h1 class="cover-hero__brand" aria-label="czh and hsj">
          <span class="pair--him">czh</span>
          <span class="pair__amp">♡</span>
          <span class="pair--her">hsj</span>
        </h1>
        <div class="cover-hero__days">
          <span class="stat-num">{{ cover.loveDays }}</span>
          <span class="cover-hero__days-unit">天啦</span>
        </div>
        <p class="cover-hero__tag">每一天都闪闪发光</p>
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
            <h2 class="modal__title">悄悄登录 ✦</h2>
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
            {{ loginLoading ? '登录中…' : '进入我们的小宇宙' }}
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
    linear-gradient(180deg, rgba(255, 248, 245, 0) 42%, #fff5f8 100%),
    radial-gradient(70% 55% at 85% 18%, rgba(255, 143, 171, 0.55), transparent 62%),
    radial-gradient(55% 45% at 12% 40%, rgba(91, 127, 209, 0.35), transparent 58%),
    radial-gradient(40% 35% at 50% 60%, rgba(255, 214, 165, 0.4), transparent 70%),
    linear-gradient(155deg, #1a1a22 0%, #2a3d6a 38%, #6b8fd4 72%, #ffb3c6 100%);
  opacity: 0.28;
  pointer-events: none;
  z-index: 0;
  animation: ambience-shift 12s ease-in-out infinite alternate;
}

.cover-floats {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 0;
  overflow: hidden;
}

.float {
  position: absolute;
  font-size: 1.1rem;
  opacity: 0.45;
  color: var(--blush);
  animation: float-y 4s ease-in-out infinite;
}

.float--1 { top: 12%; left: 8%; animation-delay: 0s; color: var(--blush); }
.float--2 { top: 22%; right: 10%; animation-delay: 0.8s; color: var(--coral); font-size: 0.9rem; }
.float--3 { top: 48%; left: 6%; animation-delay: 1.4s; font-size: 0.85rem; opacity: 0.35; }
.float--4 { top: 38%; right: 6%; animation-delay: 0.4s; color: var(--peach); }
.float--5 { top: 18%; left: 42%; animation-delay: 1.1s; font-size: 0.75rem; opacity: 0.3; }

.cover-page > :not(.cover-atmosphere):not(.cover-floats):not(.login-entry) {
  position: relative;
  z-index: 1;
  margin-left: 20px;
  margin-right: 20px;
}

.cover-hero {
  text-align: center;
  padding: 52px 8px 20px;
  animation: cover-rise 0.7s cubic-bezier(0.22, 1.2, 0.36, 1) both;
}

.cover-hero__brand {
  font-family: var(--font-display);
  font-size: clamp(2.9rem, 13vw, 4rem);
  line-height: 1.12;
  margin-bottom: 22px;
  font-weight: 400;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 0.28em;
}

.pair--him {
  color: var(--him);
  text-shadow: 0 6px 0 rgba(26, 26, 31, 0.06);
  animation: wiggle 3.5s ease-in-out infinite;
}

.pair--her {
  color: var(--coral-deep);
  text-shadow: 0 6px 0 rgba(61, 95, 173, 0.1);
  animation: wiggle 3.5s ease-in-out infinite reverse;
}

.pair__amp {
  color: var(--blush);
  font-size: 0.55em;
  animation: heart-pulse 1.8s ease-in-out infinite;
}

.cover-hero__sub {
  font-size: 0.8125rem;
  color: var(--ink-muted);
  letter-spacing: 0.36em;
  margin-bottom: 12px;
  font-weight: 700;
}

.cover-hero__days {
  display: inline-flex;
  align-items: baseline;
  justify-content: center;
  gap: 6px;
  padding: 14px 30px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(255, 238, 242, 0.85));
  border: 2px solid rgba(255, 194, 209, 0.7);
  box-shadow: 0 6px 0 rgba(255, 143, 171, 0.18), 0 14px 28px rgba(91, 127, 209, 0.1);
  backdrop-filter: blur(8px);
  animation: heart-pulse 3.5s ease-in-out infinite;
}

.cover-hero__days .stat-num {
  background: linear-gradient(120deg, var(--him), var(--blush), var(--coral-deep));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.cover-hero__days-unit {
  font-family: var(--font-display);
  font-size: 1.25rem;
  color: var(--blush);
}

.cover-hero__tag {
  margin-top: 14px;
  font-size: 0.8rem;
  color: var(--ink-muted);
  font-weight: 600;
}

.cover-next {
  animation-delay: 0.12s;
}

.cover-next__title {
  font-size: 1.08rem;
  font-weight: 700;
  margin-bottom: 4px;
  color: var(--him);
  font-family: var(--font-display);
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
  border-radius: 18px;
  box-shadow: 0 8px 18px rgba(255, 143, 171, 0.2);
  border: 3px solid #fff;
  transition: transform 0.25s ease;
  transform: rotate(-1.5deg);
}

.featured-item:nth-child(even) img {
  transform: rotate(1.8deg);
}

.featured-item:active img {
  transform: rotate(0deg) scale(0.98);
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
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(160deg, #fff, #ffeef2);
  border: 2px solid rgba(255, 194, 209, 0.7);
  box-shadow: 0 5px 0 rgba(255, 143, 171, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.75;
  transition: opacity 0.2s, transform 0.2s;
  margin: 0 !important;
  animation: float-y 3s ease-in-out infinite;
}

.login-entry:active {
  opacity: 1;
  transform: scale(0.94);
  animation: none;
}

.login-entry__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--blush) 0%, var(--coral) 100%);
  box-shadow: 0 0 0 4px rgba(255, 143, 171, 0.2);
}

@keyframes cover-rise {
  from {
    opacity: 0;
    transform: translateY(18px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

@keyframes ambience-shift {
  from { filter: saturate(1) hue-rotate(0deg); }
  to { filter: saturate(1.12) hue-rotate(-8deg); }
}
</style>
