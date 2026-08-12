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

const emit = defineEmits(['select'])

const meta = computed(() => DOLL_META[props.who] || DOLL_META.czh)
const viewerRef = ref(null)
const pointerDown = ref(null)

onMounted(() => {
  import('@google/model-viewer')
})

function resetFraming() {
  const el = viewerRef.value
  if (!el) return
  el.cameraTarget = 'auto auto auto'
  el.cameraOrbit = 'auto auto auto'
  el.fieldOfView = 'auto'
  el.jumpCameraToGoal?.()
}

watch(
  () => props.rotating,
  (on) => {
    const el = viewerRef.value
    if (!el) return
    el.autoRotate = on
  },
)

watch(
  () => props.who,
  () => {
    requestAnimationFrame(resetFraming)
  },
)

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
  if (dx < 10 && dy < 10 && Date.now() - start.t < 450) {
    emit('select', props.who)
  }
}

/** 轻量「点头」：短暂停转并微抖相机 */
async function playNod() {
  const el = viewerRef.value
  if (!el) return
  const was = el.autoRotate
  el.autoRotate = false
  const orbit = el.getCameraOrbit?.()
  if (orbit && el.cameraOrbit !== undefined) {
    const theta = orbit.theta
    const phi = orbit.phi
    const radius = orbit.radius
    el.cameraOrbit = `${theta}rad ${Math.max(0.2, phi - 0.12)}rad ${radius}m`
    await new Promise((r) => setTimeout(r, 180))
    el.cameraOrbit = `${theta}rad ${phi}rad ${radius}m`
    await new Promise((r) => setTimeout(r, 180))
  }
  el.autoRotate = was && props.rotating
}

defineExpose({ playNod })
</script>

<template>
  <div
    class="doll"
    :class="[`doll--${meta.theme}`, `doll--${size}`]"
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
      camera-orbit="auto auto auto"
      camera-target="auto auto auto"
      field-of-view="auto"
      :auto-rotate="rotating"
      rotation-per-second="18deg"
      shadow-intensity="0.7"
      exposure="1.05"
      environment-image="neutral"
      touch-action="pan-y"
      :camera-controls="allowDrag"
      :disable-zoom="!allowZoom"
      interaction-prompt="none"
      @load="resetFraming"
    />
    <div class="doll__meta">
      <span class="doll__name">{{ meta.name }}</span>
      <span class="doll__role">{{ meta.role }}</span>
    </div>
  </div>
</template>

<style scoped>
.doll {
  position: relative;
  display: block;
  width: 100%;
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

.doll__meta {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 6px;
  display: flex;
  align-items: baseline;
  justify-content: center;
  gap: 8px;
  pointer-events: none;
  opacity: 0.9;
}

.doll__name {
  font-family: var(--font-display);
  font-weight: 600;
  font-size: 0.88rem;
}

.doll--him .doll__name {
  color: var(--him);
}

.doll--her .doll__name {
  color: var(--coral-deep);
}

.doll--aside .doll__meta {
  gap: 4px;
}

.doll--aside .doll__name {
  font-size: 0.75rem;
}

.doll--aside .doll__role {
  display: none;
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
