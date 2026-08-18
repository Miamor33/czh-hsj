<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const tabs = [
  { name: 'home', path: '/app', label: '首页', icon: '♡' },
  { name: 'album', path: '/app/album', label: '相册', icon: '◈' },
  { name: 'qa', path: '/app/qa', label: '问答', icon: '✦' },
  { name: 'challenges', path: '/app/challenges', label: '挑战', icon: '★' },
]

const hideChrome = computed(() => !!route.meta.hideChrome)

const activeTab = computed(() => {
  if (route.path.startsWith('/app/album')) return 'album'
  if (route.path.startsWith('/app/qa')) return 'qa'
  if (route.path.startsWith('/app/challenges')) return 'challenges'
  return 'home'
})

function goTab(tab) {
  router.push(tab.path)
}

function logout() {
  const audio = audioRef.value
  if (audio) {
    audio.pause()
  }
  auth.logout()
  router.replace('/')
}

const audioRef = ref(null)
/** 用户期望有声；被浏览器拦截或主动静音时为 false */
const soundOn = ref(true)
/** 背景音乐默认音量（0~1），略低于浏览器默认的 1.0 */
const DEFAULT_BGM_VOLUME = 0.28

function applyBgmVolume(audio) {
  if (audio) {
    audio.volume = DEFAULT_BGM_VOLUME
  }
}

async function tryPlay() {
  const audio = audioRef.value
  if (!audio) return false
  applyBgmVolume(audio)
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
  <div class="app-layout" :class="{ 'app-layout--immersive': hideChrome }">
    <header v-if="!hideChrome" class="app-header">
      <span class="app-header__brand">
        <span class="app-header__heart" aria-hidden="true">♡</span>
        czh & hsj
      </span>
      <div class="app-header__right">
        <span class="app-header__user">{{ auth.displayName }}</span>
        <button type="button" class="app-header__logout" @click="logout">退出</button>
      </div>
    </header>
    <main class="page" :class="{ 'page--with-tab': !hideChrome }">
      <router-view />
    </main>

    <audio ref="audioRef" src="/bgm.mp3" loop preload="auto" playsinline />

    <button
      type="button"
      class="bgm-toggle"
      :class="{ 'bgm-toggle--off': !soundOn, 'bgm-toggle--immersive': hideChrome }"
      :aria-label="soundOn ? '关闭背景音乐' : '开启背景音乐'"
      :title="soundOn ? '关闭音乐' : '开启音乐'"
      @click="toggleBgm"
    >
      <span class="bgm-toggle__icon" aria-hidden="true">♪</span>
    </button>

    <nav v-if="!hideChrome" class="tabs">
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
  background: rgba(255, 250, 248, 0.92);
  backdrop-filter: blur(16px);
  border-bottom: 2px solid rgba(255, 194, 209, 0.4);
}

.app-header__brand {
  font-family: var(--font-display);
  font-size: 1.2rem;
  font-weight: 400;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(105deg, var(--him) 20%, var(--blush) 55%, var(--coral-deep) 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.app-header__heart {
  background: none;
  -webkit-background-clip: unset;
  background-clip: unset;
  color: var(--blush);
  animation: heart-pulse 2s ease-in-out infinite;
}

.app-header__right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-header__user {
  font-size: 0.75rem;
  color: var(--coral-deep);
  font-weight: 700;
  padding: 5px 12px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(255, 194, 209, 0.45), rgba(168, 189, 232, 0.35));
  border: 1.5px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 2px 0 rgba(255, 143, 171, 0.2);
}

.app-header__logout {
  border: none;
  background: transparent;
  color: var(--ink-muted);
  font-size: 0.75rem;
  font-weight: 600;
  padding: 4px 6px;
  cursor: pointer;
}

.app-header__logout:active {
  opacity: 0.7;
}

.bgm-toggle {
  position: fixed;
  right: 16px;
  bottom: calc(var(--tab-height) + var(--safe-bottom) + 16px);
  z-index: 120;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 2px solid rgba(255, 194, 209, 0.7);
  background: linear-gradient(160deg, #fff 0%, #ffeef2 55%, #eef3ff 100%);
  box-shadow: 0 6px 0 rgba(255, 143, 171, 0.22), 0 12px 24px rgba(91, 127, 209, 0.12);
  color: var(--blush);
  font-size: 1.2rem;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8px);
  transition: transform 0.15s, opacity 0.15s, box-shadow 0.2s;
  animation: float-y 3.2s ease-in-out infinite;
}

.bgm-toggle:active {
  transform: scale(0.92);
  box-shadow: 0 2px 0 rgba(255, 143, 171, 0.22);
  animation: none;
}

.bgm-toggle--off {
  color: var(--ink-muted);
  opacity: 0.85;
  animation: none;
}

.bgm-toggle--immersive {
  bottom: calc(16px + var(--safe-bottom, 0px));
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
