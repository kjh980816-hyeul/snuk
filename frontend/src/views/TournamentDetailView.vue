<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { tournamentApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import type { ParticipantPublic, Tournament } from '@/api/types'

// 대회 상세: 포스터 원본비율 + 정보 + 홍보 상세 이미지 + 참가자 명단 전체
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const tour = ref<Tournament | null>(null)
const participants = ref<ParticipantPublic[]>([])
const notFound = ref(false)
const applying = ref(false)
const applyMsg = ref('')
const posterExpanded = ref(false)

const statusLabel = computed(() => {
  const s = tour.value?.status
  return s === 'OPEN' ? '모집중' : s === 'SCHEDULED' ? '오픈예정' : s === 'DONE' ? '종료' : '모집마감'
})
const statusCls = computed(() => {
  const s = tour.value?.status
  return s === 'OPEN' ? 'open' : s === 'SCHEDULED' ? 'ongoing' : 'closed'
})
const platLabel: Record<string, string> = { CHZZK: '치지직', SOOP: '숲', CIME: '씨미' }

async function load() {
  const id = Number(route.params.id)
  if (!id) { notFound.value = true; return }
  try {
    tour.value = await tournamentApi.detail(id)
    participants.value = await tournamentApi.participants(id).catch(() => [])
  } catch {
    notFound.value = true
  }
}

async function apply() {
  if (!tour.value) return
  if (!auth.isLoggedIn) { applyMsg.value = '로그인 후 신청할 수 있습니다.'; return }
  applying.value = true
  try {
    await tournamentApi.apply(tour.value.id)
    applyMsg.value = '참가 신청 완료! 관리자 승인 후 확정됩니다.'
  } catch (e) {
    const err = e as { response?: { data?: { message?: string } } }
    applyMsg.value = err?.response?.data?.message ?? '신청에 실패했습니다.'
  } finally {
    applying.value = false
  }
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<template>
  <section class="tour-detail">
    <div class="inner">
      <button class="back" @click="router.push('/championship')">← 대회 목록</button>

      <div v-if="notFound" class="empty-box">존재하지 않는 대회입니다.</div>

      <template v-else-if="tour">
        <!-- 상단: 포스터(원본 비율) + 정보 -->
        <div class="head">
          <div v-if="tour.bannerImageUrl" class="poster" :class="{ expanded: posterExpanded }"
               title="클릭하면 전체 포스터를 볼 수 있어요" @click="posterExpanded = !posterExpanded">
            <img :src="tour.bannerImageUrl" :alt="tour.title" />
          </div>
          <div class="info">
            <div class="meta-row">
              <span class="badge" :class="statusCls">{{ statusLabel }}</span>
              <span v-if="tour.gameName" class="game">{{ tour.gameName }}</span>
            </div>
            <h1>{{ tour.title }}</h1>
            <p v-if="tour.description" class="desc">{{ tour.description }}</p>
            <div class="stats">
              <div class="stat"><strong>{{ tour.filledSlots }}/{{ tour.capacity }}명</strong><span>참가 현황</span></div>
              <div v-if="tour.eventDate" class="stat"><strong>{{ tour.eventDate }}</strong><span>대회일</span></div>
              <div class="stat"><strong>{{ statusLabel }}</strong><span>상태</span></div>
            </div>
            <div class="acts">
              <button v-if="tour.status === 'OPEN'" class="apply" :disabled="applying" @click="apply">
                {{ applying ? '신청 중…' : '참가 신청하기' }}
              </button>
              <span v-if="applyMsg" class="apply-msg">{{ applyMsg }}</span>
            </div>
            <div v-if="tour.status === 'DONE' && tour.resultText" class="result">
              <h3>대회 결과</h3>
              <p>{{ tour.resultText }}</p>
            </div>
          </div>
        </div>

        <!-- 홍보 상세 이미지 (세로 롱폼) -->
        <div v-if="tour.detailImageUrl" class="detail-img">
          <h2 class="sec-title">대회 안내</h2>
          <img :src="tour.detailImageUrl" :alt="tour.title + ' 상세'" />
        </div>

        <!-- 참가자 명단 -->
        <div class="roster">
          <h2 class="sec-title">참가자 명단 <span class="cnt">{{ participants.length }}명</span></h2>
          <div v-if="!participants.length" class="empty-box">확정된 참가자가 아직 없습니다.</div>
          <div v-else class="roster-grid">
            <div v-for="p in participants" :key="p.nickname + p.provider" class="p-card">
              <div class="p-avatar">
                <img v-if="p.profileImageUrl" :src="p.profileImageUrl" :alt="p.nickname" />
                <span v-else>{{ p.nickname.slice(0, 1) }}</span>
              </div>
              <div class="p-name">{{ p.nickname }}</div>
              <div class="p-plat">{{ platLabel[p.provider] ?? p.provider }}</div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
.tour-detail { padding: 28px 0 60px; }
.inner { max-width: 1080px; margin: 0 auto; padding: 0 20px; }
.back { background: none; border: 1px solid var(--border); color: var(--text2); border-radius: 999px; padding: 7px 14px; font-size: 13px; cursor: pointer; margin-bottom: 18px; transition: 0.15s; }
.back:hover { color: var(--text); border-color: var(--border2); }

.head { display: grid; grid-template-columns: minmax(0, 300px) 1fr; gap: 28px; align-items: center; }
.poster { border-radius: var(--radius2); overflow: hidden; border: 1px solid var(--border); background: var(--bg2); cursor: zoom-in; }
.poster img { width: 100%; height: auto; display: block; max-height: 560px; object-fit: cover; object-position: top; }
.poster.expanded { cursor: zoom-out; }
.poster.expanded img { max-height: none; }
.info h1 { font-size: 26px; font-weight: 800; color: var(--text); margin: 10px 0 12px; line-height: 1.3; }
.meta-row { display: flex; align-items: center; gap: 10px; }
.badge { font-size: 11px; font-weight: 800; border-radius: 999px; padding: 3px 10px; }
.badge.open { background: rgba(52, 199, 120, 0.14); color: #34c878; border: 1px solid rgba(52, 199, 120, 0.4); }
.badge.ongoing { background: rgba(212, 212, 212, 0.12); color: var(--text2); border: 1px solid var(--border2); }
.badge.closed { background: rgba(255, 255, 255, 0.06); color: var(--text3); border: 1px solid var(--border); }
.game { font-size: 13px; color: var(--text2); font-weight: 600; }
.desc { font-size: 14px; color: var(--text2); line-height: 1.8; white-space: pre-line; }
.stats { display: flex; gap: 12px; margin-top: 18px; flex-wrap: wrap; }
.stat { background: var(--bg2); border: 1px solid var(--border); border-radius: var(--radius); padding: 12px 18px; display: flex; flex-direction: column; gap: 3px; min-width: 110px; }
.stat strong { font-size: 16px; color: var(--text); }
.stat span { font-size: 11px; color: var(--text3); }
.acts { margin-top: 18px; display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.apply { background: var(--accent); color: #18181c; border: none; border-radius: 10px; padding: 12px 26px; font-size: 14px; font-weight: 800; cursor: pointer; transition: 0.15s; }
.apply:hover { filter: brightness(1.1); }
.apply:disabled { opacity: 0.6; cursor: default; }
.apply-msg { font-size: 13px; color: var(--text2); }
.result { margin-top: 20px; background: var(--bg2); border: 1px solid var(--border2); border-radius: var(--radius2); padding: 16px 20px; }
.result h3 { font-size: 14px; color: var(--text); margin: 0 0 8px; }
.result p { font-size: 14px; color: var(--text2); white-space: pre-line; margin: 0; }

.sec-title { font-size: 18px; font-weight: 800; color: var(--text); margin: 40px 0 16px; }
.sec-title .cnt { font-size: 13px; color: var(--text3); font-weight: 600; margin-left: 6px; }
/* 홍보 상세(세로 롱폼)는 좁은 중앙 컬럼으로 — 풀폭이면 페이지가 과하게 길어짐 */
.detail-img { max-width: 680px; margin: 0 auto; text-align: center; }
.detail-img .sec-title { text-align: center; }
.detail-img img { width: 100%; height: auto; display: block; border-radius: var(--radius2); border: 1px solid var(--border); }

.roster-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(104px, 1fr)); gap: 10px; }
.p-card { background: var(--bg2); border: 1px solid var(--border); border-radius: var(--radius); padding: 14px 6px 10px; display: flex; flex-direction: column; align-items: center; }
.p-avatar { width: 64px; height: 64px; border-radius: 50%; overflow: hidden; background: var(--bg3); display: flex; align-items: center; justify-content: center; border: 2px solid var(--border2); }
.p-avatar img { width: 100%; height: 100%; object-fit: cover; }
.p-avatar span { font-size: 20px; font-weight: 800; color: var(--text2); }
.p-name { font-size: 12px; font-weight: 700; color: var(--text); margin-top: 8px; max-width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.p-plat { font-size: 10px; color: var(--text3); margin-top: 2px; }
.empty-box { border: 1px dashed var(--border2); border-radius: var(--radius2); padding: 40px 20px; text-align: center; color: var(--text3); font-size: 14px; }

@media (max-width: 760px) {
  .head { grid-template-columns: 1fr; }
  .poster { max-width: 420px; }
}
</style>
