<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { tournamentApi } from '@/api'
import type { Tournament } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const tournaments = ref<Tournament[]>([])
const loading = ref(true)
const applyMsg = ref<string | null>(null)
const applyTarget = ref<number | null>(null)

const featured = computed(() => tournaments.value.find((t) => t.featured && t.status === 'OPEN')
  ?? tournaments.value.find((t) => t.featured) ?? null)
const upcoming = computed(() =>
  tournaments.value.filter((t) => t.id !== featured.value?.id && t.status !== 'DONE'))
const finished = computed(() => tournaments.value.filter((t) => t.status === 'DONE'))

const statusLabel: Record<Tournament['status'], string> = {
  SCHEDULED: '오픈 예정',
  OPEN: '참가 모집중',
  CLOSED: '모집 마감',
  DONE: '종료',
}

async function reload() {
  tournaments.value = await tournamentApi.list()
}

/** 참가 신청 — 비로그인→로그인, VIEWER→안내, STREAMER→신청(승인제). 실제 인가는 백엔드 강제. */
async function apply(t: Tournament) {
  applyMsg.value = null
  applyTarget.value = t.id
  if (!auth.isLoggedIn) {
    auth.login()
    return
  }
  if (!auth.isStreamer) {
    applyMsg.value = '스트리머 등급(팔로워 임계값 이상)부터 참가 신청할 수 있어요.'
    return
  }
  try {
    await tournamentApi.apply(t.id)
    applyMsg.value = '참가 신청이 접수되었습니다(승인 대기).'
    await reload()
  } catch (e: unknown) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    applyMsg.value = msg ?? '참가 신청에 실패했습니다.'
  }
}

onMounted(async () => {
  try {
    await reload()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="wrap section">
    <h2 class="section-label">대회</h2>

    <div v-if="loading" class="empty-state">불러오는 중…</div>
    <template v-else>
      <!-- 대표 대회 -->
      <div v-if="featured" class="featured">
        <div class="banner">
          <img v-if="featured.bannerImageUrl" :src="featured.bannerImageUrl" alt="대회 배너" />
          <div v-else class="placeholder wide">대회 배너</div>
        </div>
        <div class="featured-body">
          <span class="badge" :class="featured.status">{{ statusLabel[featured.status] }}</span>
          <h3>{{ featured.title }}</h3>
          <p class="desc">{{ featured.description }}</p>
          <p class="meta" v-if="featured.gameName">게임: {{ featured.gameName }}</p>
          <p class="meta" v-if="featured.eventDate">대회일: {{ featured.eventDate }}</p>
          <p class="meta" v-if="featured.applyStart">
            신청기간: {{ featured.applyStart?.slice(0, 10) }} ~ {{ featured.applyEnd?.slice(0, 10) }}
          </p>
          <p class="slots">참가 확정 {{ featured.filledSlots }}/{{ featured.capacity }}</p>
          <button class="btn orange" :disabled="featured.status !== 'OPEN'" @click="apply(featured)">
            참가 신청 &gt;
          </button>
          <p v-if="applyMsg && applyTarget === featured.id" class="apply-msg">{{ applyMsg }}</p>
        </div>
      </div>

      <!-- 진행/예정 대회 -->
      <h4 class="sub" v-if="upcoming.length">진행/예정 대회</h4>
      <div v-if="upcoming.length" class="tour-grid">
        <div v-for="t in upcoming" :key="t.id" class="tour-card">
          <div class="thumb">
            <img v-if="t.bannerImageUrl" :src="t.bannerImageUrl" alt="대회 배너" />
            <div v-else class="placeholder wide">대회 배너</div>
          </div>
          <div class="body">
            <span class="badge" :class="t.status">{{ statusLabel[t.status] }}</span>
            <h5>{{ t.title }}</h5>
            <p class="meta" v-if="t.gameName">{{ t.gameName }}</p>
            <p class="meta" v-if="t.eventDate">{{ t.eventDate }}</p>
            <p class="slots">참가 확정 {{ t.filledSlots }}/{{ t.capacity }}</p>
            <button class="btn sm" :disabled="t.status !== 'OPEN'" @click="apply(t)">참가 신청</button>
            <p v-if="applyMsg && applyTarget === t.id" class="apply-msg">{{ applyMsg }}</p>
          </div>
        </div>
      </div>
      <div v-else-if="!featured && !finished.length" class="empty-state">아직 등록된 대회가 없습니다.</div>

      <!-- 지난 대회 결과 -->
      <h4 class="sub" v-if="finished.length">지난 대회 결과</h4>
      <div v-for="t in finished" :key="t.id" class="result-card">
        <div>
          <h5>{{ t.title }}</h5>
          <p class="meta">{{ t.gameName }}<span v-if="t.eventDate"> · {{ t.eventDate }}</span></p>
        </div>
        <p class="result-text">{{ t.resultText || '결과 준비중' }}</p>
      </div>
    </template>
  </div>
</template>

<style scoped>
.featured { display: grid; grid-template-columns: 1.3fr 1fr; gap: 28px; align-items: center; }
.banner img { width: 100%; border-radius: var(--radius); display: block; }
.placeholder.wide { aspect-ratio: 3 / 1; }
.featured-body h3 { font-size: 26px; color: var(--text-strong); margin: 12px 0 8px; }
.featured-body .desc { color: var(--text-body); }
.meta { font-size: 14px; color: var(--text-muted); margin: 2px 0; }
.slots { font-weight: 700; color: var(--text-strong); margin: 10px 0; }
.apply-msg { margin-top: 10px; color: var(--accent-orange); font-weight: 700; }
.sub { margin: 36px 0 14px; color: var(--text-strong); }
.badge.DONE { background: var(--badge-closed); }

.tour-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 18px; }
.tour-card { border: 1px solid #eee; border-radius: var(--radius); overflow: hidden; background: #fff; }
.tour-card .thumb img { width: 100%; aspect-ratio: 3 / 1; object-fit: cover; display: block; }
.tour-card .body { padding: 14px 16px 18px; }
.tour-card h5 { font-size: 17px; color: var(--text-strong); margin: 8px 0 4px; }

.result-card {
  display: flex; justify-content: space-between; align-items: center; gap: 20px;
  border: 1px solid #eee; border-radius: var(--radius); padding: 16px 20px; margin-bottom: 12px;
}
.result-card h5 { color: var(--text-strong); margin-bottom: 4px; }
.result-text { color: var(--text-body); font-weight: 600; white-space: pre-line; text-align: right; }

@media (max-width: 860px) {
  .featured { grid-template-columns: 1fr; }
  .result-card { flex-direction: column; align-items: flex-start; }
  .result-text { text-align: left; }
}
</style>
