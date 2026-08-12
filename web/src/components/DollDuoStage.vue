<script setup>
import { computed, ref, watch } from 'vue'
import DollViewer from './DollViewer.vue'
import { DOLL_META, otherPartnerKey } from '../dolls/meta'

const props = defineProps({
  /** cover | home */
  mode: { type: String, default: 'cover' },
  /** 登录身份，home 默认主玩偶 */
  focusKey: { type: String, default: '' },
  selfName: { type: String, default: '' },
})

const rotating = ref(true)
const bubbleMap = ref({ czh: '', hsj: '' })
const mainRef = ref(null)
const asideRef = ref(null)

/** 当前主舞台身份，可点击切换 */
const activeKey = ref(props.focusKey || 'czh')

watch(
  () => props.focusKey,
  (key) => {
    if (key) activeKey.value = key
  },
)

const mainWho = computed(() => activeKey.value)
const asideWho = computed(() => otherPartnerKey(activeKey.value))

function clearBubblesSoon() {
  window.clearTimeout(clearBubblesSoon._t)
  clearBubblesSoon._t = window.setTimeout(() => {
    bubbleMap.value = { czh: '', hsj: '' }
  }, 2600)
}

async function onSelect(who) {
  const meta = DOLL_META[who]
  if (!meta) return

  if (props.mode === 'cover') {
    bubbleMap.value = {
      czh: '',
      hsj: '',
      [who]: `${meta.name} · ${meta.role}`,
    }
    clearBubblesSoon()
    return
  }

  if (who !== activeKey.value) {
    activeKey.value = who
    bubbleMap.value = {
      czh: '',
      hsj: '',
      [who]: who === props.focusKey
        ? `${meta.greet}，${props.selfName || meta.name}`
        : `${meta.name} · ${meta.miss}`,
    }
    clearBubblesSoon()
    return
  }

  if (who === props.focusKey) {
    bubbleMap.value = {
      czh: '',
      hsj: '',
      [who]: `${meta.greet}，${props.selfName || meta.name}`,
    }
    await mainRef.value?.playNod?.()
  } else {
    bubbleMap.value = {
      czh: '',
      hsj: '',
      [who]: `${meta.name} · ${meta.miss}`,
    }
  }
  clearBubblesSoon()
}

function toggleRotate() {
  rotating.value = !rotating.value
}
</script>

<template>
  <section class="duo" :class="`duo--${mode}`">
    <div class="duo__toolbar">
      <p class="duo__hint">
        {{ mode === 'cover' ? '拖拽可绕看 · 点玩偶看介绍' : '点角落小玩偶可切换主角' }}
      </p>
      <button type="button" class="duo__pause" @click="toggleRotate">
        {{ rotating ? '暂停旋转' : '继续旋转' }}
      </button>
    </div>

    <div class="duo__stage" :class="{ 'duo__stage--equal': mode === 'cover' }">
      <template v-if="mode === 'cover'">
        <DollViewer
          who="czh"
          size="equal"
          :rotating="rotating"
          :allow-zoom="false"
          :bubble="bubbleMap.czh"
          @select="onSelect"
        />
        <DollViewer
          who="hsj"
          size="equal"
          :rotating="rotating"
          :allow-zoom="false"
          :bubble="bubbleMap.hsj"
          @select="onSelect"
        />
      </template>
      <template v-else>
        <DollViewer
          ref="mainRef"
          class="duo__main"
          :who="mainWho"
          size="focus"
          :rotating="rotating"
          :allow-zoom="true"
          :bubble="bubbleMap[mainWho]"
          @select="onSelect"
        />
        <DollViewer
          ref="asideRef"
          class="duo__aside"
          :who="asideWho"
          size="aside"
          :rotating="rotating"
          :allow-zoom="false"
          :bubble="bubbleMap[asideWho]"
          @select="onSelect"
        />
      </template>
    </div>
  </section>
</template>

<style scoped>
.duo {
  margin: 4px 0 8px;
  padding: 0;
  background: transparent;
  border: none;
  box-shadow: none;
  animation: rise-in 0.45s ease both;
}

.duo__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 2px;
  padding: 0 2px;
}

.duo__hint {
  font-size: 0.7rem;
  color: var(--ink-muted);
  line-height: 1.4;
  opacity: 0.85;
}

.duo__pause {
  flex-shrink: 0;
  padding: 4px 2px;
  font-size: 0.72rem;
  color: var(--ink-muted);
  background: transparent;
  border: none;
  text-decoration: underline;
  text-underline-offset: 3px;
  text-decoration-color: rgba(74, 111, 181, 0.35);
}

.duo__pause:active {
  color: var(--coral-deep);
}

.duo__stage {
  position: relative;
}

.duo__stage--equal {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.duo__stage--equal > * {
  flex: 1;
  min-width: 0;
}

.duo__main {
  width: 100%;
}

.duo__aside {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 28%;
  max-width: 120px;
  z-index: 4;
  transition: transform 0.3s ease;
}

.duo__aside:active {
  transform: scale(0.96);
}

@media (min-width: 420px) {
  .duo__aside {
    width: 24%;
    max-width: 128px;
  }
}
</style>
