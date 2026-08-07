<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

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
    <div v-if="loading" class="loading">加载中…</div>

    <template v-else-if="cover">
      <header class="cover-hero">
        <p class="cover-hero__sub">我们在一起</p>
        <h1 class="cover-hero__brand">{{ cover.brand || 'czh & hsj' }}</h1>
        <div class="cover-hero__days">
          <span class="stat-num">{{ cover.loveDays }}</span>
          <span class="cover-hero__days-unit">天</span>
        </div>
      </header>

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
  </div>
</template>

<style scoped>
.cover-page {
  min-height: 100dvh;
  padding: 32px 20px 48px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cover-hero {
  text-align: center;
  padding: 24px 0 8px;
}

.cover-hero__sub {
  font-size: 0.875rem;
  color: var(--ink-muted);
  letter-spacing: 0.2em;
  margin-bottom: 8px;
}

.cover-hero__brand {
  font-family: var(--font-display);
  font-size: clamp(2.8rem, 12vw, 4rem);
  color: var(--ink);
  line-height: 1.15;
  margin-bottom: 20px;
  text-shadow: 0 2px 20px rgba(224, 139, 122, 0.25);
}

.cover-hero__days {
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 4px;
}

.cover-hero__days-unit {
  font-family: var(--font-display);
  font-size: 1.25rem;
  color: var(--coral-deep);
}

.cover-next__title {
  font-size: 1.05rem;
  font-weight: 500;
  margin-bottom: 4px;
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
  width: 140px;
}

.featured-item img {
  width: 140px;
  height: 140px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow);
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
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 252, 248, 0.6);
  border: 1px solid rgba(224, 139, 122, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0.5;
  transition: opacity 0.2s;
}

.login-entry:active {
  opacity: 1;
}

.login-entry__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--coral);
}
</style>
