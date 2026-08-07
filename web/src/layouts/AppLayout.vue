<script setup>
import { computed } from 'vue'
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
  background: rgba(248, 241, 232, 0.92);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(224, 139, 122, 0.12);
}

.app-header__brand {
  font-family: var(--font-display);
  font-size: 1.1rem;
  color: var(--coral-deep);
}

.app-header__user {
  font-size: 0.8125rem;
  color: var(--ink-muted);
}
</style>
