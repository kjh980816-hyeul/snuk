import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api'
import { clearTokens, getAccessToken } from '@/api/client'
import type { Me } from '@/api/types'

export type LoginProvider = 'chzzk' | 'cime' | 'soop'

export const useAuthStore = defineStore('auth', () => {
  const me = ref<Me | null>(null)
  const loaded = ref(false)

  const isLoggedIn = computed(() => me.value !== null)
  const isStreamer = computed(
    () => me.value?.role === 'STREAMER' || me.value?.role === 'REPORTER' || me.value?.role === 'ADMIN')
  /** 스눅 뉴스 작성 자격(REPORTER 이상) */
  const isReporter = computed(() => me.value?.role === 'REPORTER' || me.value?.role === 'ADMIN')
  const isAdmin = computed(() => me.value?.role === 'ADMIN')

  async function fetchMe(): Promise<void> {
    if (!getAccessToken()) {
      me.value = null
      loaded.value = true
      return
    }
    try {
      me.value = await authApi.me()
    } catch {
      me.value = null
      clearTokens()
    } finally {
      loaded.value = true
    }
  }

  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } catch {
      /* stateless — 무시 */
    }
    clearTokens()
    me.value = null
  }

  function login(provider: LoginProvider = 'chzzk'): void {
    // 백엔드 OAuth 시작점으로 이동(전체 페이지 리다이렉트)
    window.location.href = `/oauth2/authorization/${provider}`
  }

  return { me, loaded, isLoggedIn, isStreamer, isReporter, isAdmin, fetchMe, logout, login }
})
