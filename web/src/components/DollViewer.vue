<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { DOLL_META } from '../dolls/meta'

const props = defineProps({
  who: { type: String, required: true },
  /** equal | focus | aside */
  size: { type: String, default: 'equal' },
  rotating: { type: Boolean, default: true },
  /** 是否允许拖拽绕看 */
  allowDrag: { type: Boolean, default: true },
  /** 是否允许双指缩放 */
  allowZoom: { type: Boolean, default: false },
  bubble: { type: String, default: '' },
})

const emit = defineEmits(['select', 'dblselect'])

const meta = computed(() => DOLL_META[props.who] || DOLL_META.czh)
const viewerRef = ref(null)
const pointerDown = ref(null)
const hopping = ref(false)
let hopTimer = 0
const DBL_MS = 300
let lastTapAt = 0
let singleTapTimer = 0

onMounted(() => {
  import('@google/model-viewer')
})

function applyAutoRotate(el, on) {
  if (!el) return
  el.autoRotate = !!on
  if (on) {
    el.setAttribute('auto-rotate', '')
  } else {
    el.removeAttribute('auto-rotate')
  }
}

function orbitRadius() {
  if (props.size === 'aside') return '155%'
  if (props.size === 'focus') return '138%'
  return '122%'
}

function resetFraming() {
  const el = viewerRef.value
  if (!el) return
  const shouldRotate = props.rotating
  el.autoRotate = false
  el.resetTurntableRotation?.(0)
  el.interpolationDecay = 0
  el.cameraTarget = 'auto auto auto'
  el.cameraOrbit = `0deg 75deg ${orbitRadius()}`
  el.fieldOfView = props.size === 'focus' ? '34deg' : 'auto'
  el.jumpCameraToGoal?.()
  requestAnimationFrame(() => {
    el.interpolationDecay = 50
    applyAutoRotate(el, shouldRotate)
  })
}

watch(
  () => props.rotating,
  (on) => {
    applyAutoRotate(viewerRef.value, on)
  },
)

watch(
  () => props.who,
  () => {
    requestAnimationFrame(resetFraming)
  },
)

function onCameraChange() {
  if (!props.rotating) {
    applyAutoRotate(viewerRef.value, false)
  }
}

function onPointerDown(event) {
  pointerDown.value = {
    x: event.clientX,
    y: event.clientY,
    t: Date.now(),
  }
}

function onPointerUp(event) {
  const start = pointerDown.value
  pointerDown.value = null
  if (!start) return
  const dx = Math.abs(event.clientX - start.x)
  const dy = Math.abs(event.clientY - start.y)
  if (dx >= 10 || dy >= 10 || Date.now() - start.t >= 450) return

  const now = Date.now()
  if (lastTapAt && now - lastTapAt < DBL_MS) {
    window.clearTimeout(singleTapTimer)
    singleTapTimer = 0
    lastTapAt = 0
    playHop()
    emit('dblselect', props.who)
    return
  }

  lastTapAt = now
  window.clearTimeout(singleTapTimer)
  singleTapTimer = window.setTimeout(() => {
    singleTapTimer = 0
    playHop()
    emit('select', props.who)
  }, DBL_MS)
}

/** 点击轻跳一下，不改相机距离 */
function playHop() {
  hopping.value = false
  requestAnimationFrame(() => {
    hopping.value = true
  })
  window.clearTimeout(hopTimer)
  hopTimer = window.setTimeout(() => {
    hopping.value = false
  }, 520)
}

defineExpose({ playHop, resetFraming })
</script>

<template>
  <div
    class="doll"
    :class="[`doll--${meta.theme}`, `doll--${size}`, { 'doll--hop': hopping }]"
    @pointerdown="onPointerDown"
    @pointerup="onPointerUp"
  >
    <div v-if="bubble" class="doll__bubble">{{ bubble }}</div>
    <model-viewer
      :key="who"
      ref="viewerRef"
      class="doll__viewer"
      :src="meta.src"
      :alt="`${meta.name} 玩偶`"
      :camera-orbit="`0deg 75deg ${orbitRadius()}`"
      camera-target="auto auto auto"
      :field-of-view="size === 'focus' ? '34deg' : 'auto'"
      :auto-rotate="rotating ? true : undefined"
      rotation-per-second="8deg"
      shadow-intensity="0.7"
      exposure="1.05"
      environment-image="neutral"
      touch-action="pan-y"
      :camera-controls="allowDrag"
      :disable-zoom="!allowZoom"
      disable-tap
      interaction-prompt="none"
      @load="resetFraming"
      @camera-change="onCameraChange"
    />
  </div>
</template>

<style scoped>
.doll {
  position: relative;
  display: block;
  width: 100%;
}

.doll--hop .doll__viewer {
  animation: doll-hop 0.5s cubic-bezier(0.22, 1.45, 0.36, 1);
}

@keyframes doll-hop {
  0%,
  100% {
    transform: translateY(0);
  }
  32% {
    transform: translateY(-12%);
  }
  58% {
    transform: translateY(0);
  }
  76% {
    transform: translateY(-5%);
  }
}

.doll__viewer {
  display: block;
  width: 100%;
  height: auto;
  aspect-ratio: 1 / 1.22;
  background:
    radial-gradient(55% 22% at 50% 94%, rgba(26, 26, 31, 0.1), transparent 72%),
    radial-gradient(50% 20% at 50% 92%, rgba(74, 111, 181, 0.14), transparent 70%);
  border: none;
  box-shadow: none;
  overflow: visible;
  --poster-color: transparent;
}

.doll--aside .doll__viewer {
  aspect-ratio: 1 / 1.22;
}

.doll--aside .doll__bubble {
  font-size: 0.68rem;
  padding: 5px 8px;
  white-space: normal;
  text-align: center;
  max-width: 120px;
}

.doll__bubble {
  position: absolute;
  top: 4px;
  left: 50%;
  transform: translate(-50%, -100%);
  z-index: 3;
  max-width: 92%;
  padding: 7px 11px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.82);
  border: none;
  box-shadow: 0 6px 20px rgba(20, 20, 28, 0.08);
  backdrop-filter: blur(8px);
  font-size: 0.78rem;
  color: var(--ink);
  white-space: nowrap;
  animation: bubble-in 0.25s ease;
}

.doll__bubble::after {
  display: none;
}

@keyframes bubble-in {
  from {
    opacity: 0;
    transform: translate(-50%, -90%);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -100%);
  }
}
</style>
