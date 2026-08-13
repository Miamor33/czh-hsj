<script setup>
import { computed, ref } from 'vue'
import { storeToRefs } from 'pinia'
import DollViewer from './DollViewer.vue'
import { DOLL_META, otherPartnerKey } from '../dolls/meta'
import { useDollStore } from '../stores/doll'

const props = defineProps({
  /** cover | home */
  mode: { type: String, default: 'cover' },
  /** 登录身份，home 默认主玩偶 */
  focusKey: { type: String, default: '' },
  selfName: { type: String, default: '' },
})

const dollStore = useDollStore()
const { rotating } = storeToRefs(dollStore)
const bubbleMap = ref({ czh: '', hsj: '' })
const mainRef = ref(null)
const asideRef = ref(null)
const coverLeftRef = ref(null)
const coverRightRef = ref(null)

const mainWho = computed(() => props.focusKey || 'czh')
const asideWho = computed(() => otherPartnerKey(mainWho.value))

function clearBubblesSoon() {
  window.clearTimeout(clearBubblesSoon._t)
  clearBubblesSoon._t = window.setTimeout(() => {
    bubbleMap.value = { czh: '', hsj: '' }
  }, 2600)
}

function onSelect(who) {
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

  bubbleMap.value = {
    czh: '',
    hsj: '',
    [who]: who === props.focusKey
      ? `${meta.greet}·${props.selfName || meta.name}`
      : `${meta.miss}`,
  }
  clearBubblesSoon()
}

function toggleRotate() {
  dollStore.toggleRotate()
}

function resetCameras() {
  mainRef.value?.resetFraming?.()
  asideRef.value?.resetFraming?.()
  coverLeftRef.value?.resetFraming?.()
  coverRightRef.value?.resetFraming?.()
}
</script>

<template>
  <section class="duo" :class="`duo--${mode}`">
    <div class="duo__stage" :class="{ 'duo__stage--equal': mode === 'cover' }">
      <div class="duo__controls">
        <button type="button" class="duo__ctrl" title="复位视角" @click="resetCameras">复位</button>
        <button type="button" class="duo__ctrl" :title="rotating ? '暂停旋转' : '继续旋转'" @click="toggleRotate">
          {{ rotating ? '暂停' : '旋转' }}
        </button>
      </div>
      <template v-if="mode === 'cover'">
        <DollViewer
          ref="coverLeftRef"
          who="czh"
          size="equal"
          :rotating="rotating"
          :allow-zoom="false"
          :bubble="bubbleMap.czh"
          @select="onSelect"
        />
        <DollViewer
          ref="coverRightRef"
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
          :allow-zoom="false"
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

.duo__stage {
  position: relative;
}

.duo__controls {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 5;
  display: flex;
  gap: 6px;
}

.duo__ctrl {
  padding: 3px 8px;
  font-size: 0.68rem;
  line-height: 1.3;
  color: var(--ink-muted);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(26, 26, 31, 0.08);
  border-radius: 999px;
  backdrop-filter: blur(8px);
}

.duo__ctrl:active {
  color: var(--coral-deep);
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

.duo--home .duo__main {
  width: 68%;
  max-width: 240px;
  margin: 0 auto;
}

.duo__aside {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 22%;
  max-width: 86px;
  z-index: 4;
  transition: transform 0.3s ease;
}

@media (min-width: 420px) {
  .duo--home .duo__main {
    width: 62%;
    max-width: 252px;
  }

  .duo__aside {
    width: 18%;
    max-width: 92px;
  }
}
</style>
