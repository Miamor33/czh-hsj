<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const tabs = [
  { name: 'home', path: '/app', label: '首页', icon: '♡' },
  { name: 'album', path: '/app/album', label: '相册', icon: '▣' },
  { name: 'qa', path: '/app/qa', label: '问答', icon: '?' },
  { name: 'challenges', path: '/app/challenges', label: '挑战', icon: '★' },
]

const activeTab = computed(() => {
  if (route.path.startsWith('/app/album')) return 'album'
  if (route.path.startsWith('/app/qa')) return 'qa'
  if (route.path.startsWith('/app/challenges')) return 'challenges'
  return 'home'
})

function goTab(tab) {
  router.push(tab.path)
}

const audioRef = ref(null)
/** 用户期望有声；被浏览器拦截或主动静音时为 false */
const soundOn = ref(true)

async function tryPlay() {
  const audio = audioRef.value
  if (!audio) return false
  audio.muted = !soundOn.value
  try {
    await audio.play()
    return true
  } catch {
    return false
  }
}

async function toggleBgm() {
  const audio = audioRef.value
  if (!audio) return

  if (audio.paused) {
    soundOn.value = true
    audio.muted = false
    await tryPlay()
    return
  }

  soundOn.value = !soundOn.value
  audio.muted = !soundOn.value
}

onMounted(async () => {
  await nextTick()
  const ok = await tryPlay()
  if (!ok) {
    soundOn.value = false
  }
})
</script>

<template>
  <div class="app-layout">
    <header class="app-header">
      <span class="app-header__brand">czh & hsj</span>
      <span class="app-header__user">{{ auth.displayName }}</span>
    </header>
    <main class="page page--with-tab">
      <router-view />
    </main>

    <audio ref="audioRef" src="/bgm.mp3" loop preload="auto" playsinline />

    <button
      type="button"
      class="bgm-toggle"
      :class="{ 'bgm-toggle--off': !soundOn }"
      :aria-label="soundOn ? '关闭背景音乐' : '开启背景音乐'"
      :title="soundOn ? '关闭音乐' : '开启音乐'"
      @click="toggleBgm"
    >
      <span class="bgm-toggle__icon" aria-hidden="true">♪</span>
    </button>

    <nav class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.name"
        class="tab-item"
        :class="{ active: activeTab === tab.name }"
        @click="goTab(tab)"
      >
        <span class="tab-item__icon">{{ tab.icon }}</span>
        <span>{{ tab.label }}</span>
      </button>
    </nav>
  </div>
</template>

<style scoped>
.app-layout {
  min-height: 100dvh;
}

.app-header {
  position: sticky;
  top: 0;
  z-index: 50;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: rgba(249, 250, 253, 0.9);
  backdrop-filter: blur(14px);
  border-bottom: 1px solid rgba(26, 26, 31, 0.07);
}

.app-header__brand {
  font-family: var(--font-display);
  font-size: 1.12rem;
  font-weight: 600;
  background: linear-gradient(105deg, var(--him) 35%, var(--coral-deep) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.app-header__user {
  font-size: 0.75rem;
  color: var(--coral-deep);
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(74, 111, 181, 0.1);
  border: 1px solid rgba(74, 111, 181, 0.12);
}

.bgm-toggle {
  position: fixed;
  right: 16px;
  bottom: calc(var(--tab-height) + var(--safe-bottom) + 16px);
  z-index: 120;
  width: 46px;
  height: 46px;
  border-radius: 50%;
  border: 1px solid rgba(42, 74, 140, 0.16);
  background: linear-gradient(160deg, #fff 0%, #eef2f8 100%);
  box-shadow: 0 10px 28px rgba(20, 20, 28, 0.1);
  color: var(--coral-deep);
  font-size: 1.15rem;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8px);
  transition: transform 0.15s, opacity 0.15s, box-shadow 0.2s;
}

.bgm-toggle:active {
  transform: scale(0.95);
}

.bgm-toggle--off {
  color: var(--ink-muted);
  opacity: 0.85;
}

.bgm-toggle--off .bgm-toggle__icon {
  position: relative;
}

.bgm-toggle--off .bgm-toggle__icon::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 1.35em;
  height: 2px;
  background: currentColor;
  transform: translate(-50%, -50%) rotate(-45deg);
  border-radius: 1px;
}
</style>
