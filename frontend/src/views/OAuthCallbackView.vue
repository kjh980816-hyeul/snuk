<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { setTokens } from '@/api/client'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const error = ref<string | null>(null)

onMounted(async () => {
  // 백엔드가 #accessToken=..&refreshToken=.. fragment 로 리다이렉트
  const hash = window.location.hash.startsWith('#') ? window.location.hash.slice(1) : window.location.hash
  const params = new URLSearchParams(hash)
  const access = params.get('accessToken')
  const refresh = params.get('refreshToken')
  const err = params.get('error')

  if (err || !access || !refresh) {
    error.value = '로그인에 실패했습니다. 다시 시도해 주세요.'
    return
  }
  setTokens(access, refresh)
  // URL 의 토큰 흔적 제거
  window.history.replaceState({}, document.title, '/oauth/callback')
  await auth.fetchMe()
  router.replace('/')
})
</script>

<template>
  <div class="wrap callback">
    <p v-if="error" class="err">{{ error }}</p>
    <p v-else>로그인 처리 중…</p>
  </div>
</template>

<style scoped>
.callback { padding: 80px 20px; text-align: center; }
.err { color: var(--label-red); font-weight: 700; }
</style>
