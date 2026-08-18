import { defineStore } from 'pinia'
import { ref } from 'vue'

const STORAGE_KEY = 'czh_hsj_doll_rotating'

function loadRotating() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw === '0') return false
    if (raw === '1') return true
  } catch {
    /* ignore */
  }
  return true
}

export const useDollStore = defineStore('doll', () => {
  const rotating = ref(loadRotating())

  function persist() {
    try {
      localStorage.setItem(STORAGE_KEY, rotating.value ? '1' : '0')
    } catch {
      /* ignore */
    }
  }

  function toggleRotate() {
    rotating.value = !rotating.value
    persist()
  }

  return {
    rotating,
    toggleRotate,
  }
})
