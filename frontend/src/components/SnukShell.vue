<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { campaignApi, spotlightApi, tournamentApi, uploadApi } from '@/api'
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
  w.__snukMe = auth.me ? { id: auth.me.id, role: auth.me.role } : null
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
  // 스트리머 등록 버튼 노출 갱신 (렌더러 로드 후에만 존재)
  ;(w.__snukInitStreamerPost as (() => void) | undefined)?.()
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

/**
 * 데이터 동기화 3중화 — 메인 섹션과 메뉴 페이지가 항상 같은 최신 데이터를 보도록:
 * ① 라우트 이동마다 재로딩 ② 신청/등록 액션 후 재로딩 ③ 60초 주기 갱신(탭 활성 시).
 * (기존엔 셸 최초 마운트 1회 로드뿐이라 페이지·메인이 따로 놀았음 — 2026-07-14)
 */
let reloading = false
async function reloadData(): Promise<void> {
  if (reloading) return
  reloading = true
  try {
    w.__SNUK_DATA = await loadSnukData()
    const init = w.__snukInit as (() => void) | undefined
    init?.()
    reflectLoginState()
  } finally {
    reloading = false
  }
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
    // 신청 후 선착순 키 발급 여부 확인 → 발급 키 즉시 노출 (완료 후 전 화면 데이터 동기화)
    applyCampaign: async (id: number) => {
      const applied = await campaignApi.apply(id)
      try {
        const mine = await campaignApi.myApplication(id, true)
        return { ...applied, assignedKey: mine.hasAssignedKey ? mine.assignedKey : null }
      } catch {
        return { ...applied, assignedKey: null }
      } finally {
        void reloadData()
      }
    },
    applyTournament: async (id: number) => {
      const res = await tournamentApi.apply(id)
      void reloadData()
      return res
    },
    writeReview: (campaignId: number, title: string, content: string) =>
      campaignApi.writeReview(campaignId, { title, content }),
    createSpotlight: (body: { title: string; platform: SpotlightPlatform; streamUrl: string; scheduledAt?: string | null }) =>
      spotlightApi.create(body),
    buyGoods: (id: number) => {
      router.push({ path: '/goods', query: { buy: String(id) } })
    },
    // 스트리머 본인 컨텐츠·대회 등록/수정/삭제 (백엔드가 소유자·등급 재검증)
    uploadImage: (file: File) => uploadApi.image(file),
    getContent: (kind: string, id: number) =>
      kind === 'tournament' ? tournamentApi.detail(id) : campaignApi.detail(id),
    createContent: async (kind: string, body: Record<string, unknown>) => {
      const r = kind === 'tournament' ? await tournamentApi.create(body) : await campaignApi.create(body)
      void reloadData()
      return r
    },
    updateContent: async (kind: string, id: number, body: Record<string, unknown>) => {
      const r = kind === 'tournament' ? await tournamentApi.update(id, body) : await campaignApi.update(id, body)
      void reloadData()
      return r
    },
    deleteContent: async (kind: string, id: number) => {
      if (kind === 'tournament') await tournamentApi.remove(id)
      else await campaignApi.remove(id)
      void reloadData()
    },
    // 주최자 참가자 관리 (본인 대회 한정 — 백엔드 재검증)
    manageParticipants: (id: number) => tournamentApi.manageParticipants(id),
    exportParticipants: (id: number) => tournamentApi.exportParticipants(id),
    decideParticipant: async (id: number, pid: number, approve: boolean) => {
      if (approve) await tournamentApi.approveParticipant(id, pid)
      else await tournamentApi.rejectParticipant(id, pid)
      void reloadData()
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
  // v-html DOM 이 실제로 붙은 뒤에 렌더러/로그인 반영 실행 (재마운트 시 else 분기가
  // flush 전에 돌면 사이드바가 로그아웃 상태로 남는 버그 — 2026-07-11)
  await nextTick()

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
  // 페이지 이동 시 최신 데이터로 재로딩 — 어디서 뭘 바꿔도 다음 화면에 바로 반영
  void reloadData()
})

// 60초 주기 갱신(탭이 보일 때만) — 라이브 상태/모집 현황을 머무는 화면에서도 최신화
const refreshTimer = window.setInterval(() => {
  if (document.visibilityState === 'visible') void reloadData()
}, 60_000)
onBeforeUnmount(() => window.clearInterval(refreshTimer))

watch(() => auth.me, () => reflectLoginState())
</script>

<template>
  <div class="snuk-page" :data-theme="theme">
    <div v-html="shellTop"></div>
    <slot />
    <div class="snuk-bottom" v-html="shellBottom"></div>
  </div>
</template>
