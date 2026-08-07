import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '../api/http'

const STORAGE_KEY = 'czh_hsj_auth'

function loadStored() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}

export const useAuthStore = defineStore('auth', () => {
  const stored = loadStored()
  const token = ref(stored.token || '')
  const partnerKey = ref(stored.partnerKey || '')
  const displayName = ref(stored.displayName || '')

  const isLoggedIn = computed(() => !!token.value)

  function persist() {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        token: token.value,
        partnerKey: partnerKey.value,
        displayName: displayName.value,
      }),
    )
  }

  async function login(partnerKeyVal, password) {
    const data = await http.post('/auth/login', {
      partnerKey: partnerKeyVal,
      password,
    })
    token.value = data.token
    partnerKey.value = data.partnerKey
    displayName.value = data.displayName
    persist()
    return data
  }

  function logout() {
    token.value = ''
    partnerKey.value = ''
    displayName.value = ''
    localStorage.removeItem(STORAGE_KEY)
  }

  return {
    token,
    partnerKey,
    displayName,
    isLoggedIn,
    login,
    logout,
  }
})
