<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { campaignApi, spotlightApi, tournamentApi } from '@/api'
import { loadSnukData } from '@/snuk/snukData'
import type { SpotlightPlatform } from '@/api/types'

/**
 * 시안 셸 오케스트레이터:
 *  1) 실 API 로 window.__SNUK_DATA 구성 → 2) 시안 렌더러(home-snuk-init.js) 주입
 *  3) window.__snukActions 로 실제 신청/후기/스포트라이트/굿즈 액션 제공
 */
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const theme = ref<'light' | 'dark'>(
  (localStorage.getItem('snuk-theme') as 'light' | 'dark') || 'dark',
)
const shellTop = ref('')
const shellBottom = ref('')

const w = window as unknown as Record<string, unknown>

function syncAuthGlobals(): void {
  w.__snukLoggedIn = auth.isLoggedIn
  w.__snukUser = auth.me ? { nickname: auth.me.nickname, role: auth.me.role } : null
}

function reflectLoginState(): void {
  syncAuthGlobals()
  const authBtns = document.getElementById('rs-auth-btns')
  const profile = document.getElementById('rs-profile')
  if (!authBtns || !profile) return
  if (auth.isLoggedIn) {
    const name = auth.me?.nickname || 'SNUK유저'
    authBtns.style.display = 'none'
    profile.style.display = 'block'
    const uname = document.getElementById('rs-uname')
    if (uname) uname.textContent = name
    const av = document.getElementById('rs-av')
    if (av) {
      const img = auth.me?.profileImageUrl
      av.innerHTML = img
        ? `<img src="${img}" alt="" style="width:100%;height:100%;object-fit:cover;border-radius:50%;">`
        : name.slice(0, 1)
    }
    // 모바일 상단바 로그인 버튼 → 마이페이지
    const mobileBtn = document.querySelector<HTMLButtonElement>('.mobile-login-btn')
    if (mobileBtn) {
      mobileBtn.textContent = name
      mobileBtn.setAttribute('onclick', 'openMypage()')
    }
  } else {
    authBtns.style.display = 'flex'
    profile.style.display = 'none'
  }
  // 관리자 메뉴 — ADMIN 에게만 노출 (실제 인가는 백엔드 강제)
  document.querySelectorAll<HTMLElement>('.rs-admin-item').forEach((el) => {
    el.style.display = auth.isAdmin ? '' : 'none'
  })
}

function applyTheme(next: 'light' | 'dark'): void {
  theme.value = next
  document.documentElement.setAttribute('data-theme', next)
  localStorage.setItem('snuk-theme', next)
  const btn = document.getElementById('themeBtn')
  if (btn) btn.textContent = next === 'light' ? '다크 모드' : '라이트 모드'
}

function errorMessage(e: unknown): string {
  const err = e as { response?: { data?: { message?: string }; status?: number } }
  if (err?.response?.status === 401) return '로그인이 필요합니다'
  return err?.response?.data?.message ?? '요청에 실패했습니다'
}

async function reloadData(): Promise<void> {
  w.__SNUK_DATA = await loadSnukData()
  const init = w.__snukInit as (() => void) | undefined
  init?.()
  reflectLoginState()
}

onMounted(async () => {
  if (!auth.loaded) await auth.fetchMe()

  // 시안 CSS 1회 주입 (셀렉터가 .snuk-page / [data-theme] 스코프)
  if (!document.getElementById('snuk-home-css')) {
    const link = document.createElement('link')
    link.id = 'snuk-home-css'
    link.rel = 'stylesheet'
    link.href = '/home-snuk.css'
    document.head.appendChild(link)
  }
  document.documentElement.setAttribute('data-theme', theme.value)

  // 라우팅/인증/액션 전역 — 렌더러(전역 JS)가 사용
  w.__snukNav = (path: string) => {
    const drawer = document.getElementById('mobile-drawer')
    const overlay = document.getElementById('mobile-overlay')
    if (drawer) drawer.classList.remove('open')
    if (overlay) overlay.classList.remove('open')
    router.push(path)
  }
  w.__snukLogout = async () => {
    await auth.logout()
    location.href = '/'
  }
  w.__snukActions = {
    // 신청 후 선착순 키 발급 여부 확인 → 발급 키 즉시 노출
    applyCampaign: async (id: number) => {
      const applied = await campaignApi.apply(id)
      try {
        const mine = await campaignApi.myApplication(id, true)
        return { ...applied, assignedKey: mine.hasAssignedKey ? mine.assignedKey : null }
      } catch {
        return { ...applied, assignedKey: null }
      }
    },
    applyTournament: (id: number) => tournamentApi.apply(id),
    writeReview: (campaignId: number, title: string, content: string) =>
      campaignApi.writeReview(campaignId, { title, content }),
    createSpotlight: (body: { title: string; platform: SpotlightPlatform; streamUrl: string }) =>
      spotlightApi.create(body),
    buyGoods: (id: number) => {
      router.push({ path: '/goods', query: { buy: String(id) } })
    },
    errorMessage,
    reloadData,
  }
  syncAuthGlobals()

  // 셸 HTML + 실데이터 동시 로드 → 데이터 준비 후에만 렌더러 주입
  const [top, bottom, data] = await Promise.all([
    fetch('/snuk-shell-top.html').then((r) => r.text()),
    fetch('/snuk-shell-bottom.html').then((r) => r.text()),
    loadSnukData(),
  ])
  w.__SNUK_DATA = data
  shellTop.value = top
  shellBottom.value = bottom

  // 시안 렌더러 주입 (스크립트 말미에서 __snukInit 자동 실행)
  if (!document.getElementById('snuk-init-js')) {
    const script = document.createElement('script')
    script.id = 'snuk-init-js'
    script.src = '/home-snuk-init.js'
    script.onload = () => {
      // 함수 선언(toggleTheme)을 Vue 상태 연동 버전으로 교체
      w.toggleTheme = () => applyTheme(theme.value === 'light' ? 'dark' : 'light')
      applyTheme(theme.value)
      reflectLoginState()
      const setNav = w.__snukSetActiveNav as ((p: string) => void) | undefined
      setNav?.(route.path)
    }
    document.body.appendChild(script)
  } else {
    const init = w.__snukInit as (() => void) | undefined
    init?.()
    reflectLoginState()
  }
})

watch(() => route.path, (path) => {
  const setNav = w.__snukSetActiveNav as ((p: string) => void) | undefined
  setNav?.(path)
})

watch(() => auth.me, () => reflectLoginState())
</script>

<template>
  <div class="snuk-page" :data-theme="theme">
    <div v-html="shellTop"></div>
    <slot />
    <div class="snuk-bottom" v-html="shellBottom"></div>
  </div>
</template>
