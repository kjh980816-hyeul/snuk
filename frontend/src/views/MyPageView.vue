<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { mypageApi } from '@/api'
import type { MypageSummary } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const summary = ref<MypageSummary | null>(null)
const loading = ref(true)

const roleLabel: Record<string, string> = {
  VIEWER: '시청자', STREAMER: '스트리머', ADMIN: '관리자', GUEST: '게스트',
}
const appStatusLabel: Record<string, string> = {
  PENDING: '승인 대기', APPROVED: '확정', REJECTED: '거절',
}
const orderStatusLabel: Record<string, string> = {
  PENDING: '결제 대기', PAID: '결제 완료', CANCELLED: '취소', FAILED: '실패',
}

function dt(v: string | null | undefined) {
  return v ? v.slice(0, 16).replace('T', ' ') : '-'
}
function won(v: number) {
  return v.toLocaleString('ko-KR') + '원'
}

onMounted(async () => {
  try {
    summary.value = await mypageApi.summary()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="wrap mypage section" v-if="auth.me">
    <h2 class="section-label">마이페이지</h2>
    <div class="profile">
      <div class="avatar">
        <img v-if="auth.me.profileImageUrl" :src="auth.me.profileImageUrl" alt="프로필" />
        <div v-else class="placeholder">프로필</div>
      </div>
      <div class="info">
        <p class="nick">{{ auth.me.nickname }}</p>
        <p class="row">등급 · <strong>{{ roleLabel[auth.me.role] ?? auth.me.role }}</strong></p>
        <p class="row" v-if="auth.me.followerCount !== null">팔로워 · {{ auth.me.followerCount?.toLocaleString() }}</p>
      </div>
    </div>

    <div v-if="loading" class="empty-state">불러오는 중…</div>
    <template v-else-if="summary">
      <!-- 내 캠페인 신청 -->
      <h4 class="sub">내 컨텐츠 신청</h4>
      <table class="grid" v-if="summary.applications.length">
        <thead><tr><th>캠페인</th><th>상태</th><th>배정 키</th><th>신청일</th></tr></thead>
        <tbody>
          <tr v-for="a in summary.applications" :key="a.applicationId">
            <td>{{ a.campaignTitle }}</td>
            <td><span class="pill" :class="a.status">{{ appStatusLabel[a.status] }}</span></td>
            <td class="key">{{ a.hasAssignedKey ? a.maskedKey : '-' }}</td>
            <td>{{ dt(a.appliedAt) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty-line">신청한 컨텐츠가 없습니다.</p>
      <p class="hint" v-if="summary.applications.some((a) => a.hasAssignedKey)">
        키 전체 보기는 해당 컨텐츠 페이지에서 본인 확인 후 가능합니다.
      </p>

      <!-- 내 대회 참가 -->
      <h4 class="sub">내 대회 참가</h4>
      <table class="grid" v-if="summary.tournaments.length">
        <thead><tr><th>대회</th><th>상태</th><th>신청일</th></tr></thead>
        <tbody>
          <tr v-for="t in summary.tournaments" :key="t.participantId">
            <td>{{ t.tournamentTitle }}</td>
            <td><span class="pill" :class="t.status">{{ appStatusLabel[t.status] }}</span></td>
            <td>{{ dt(t.appliedAt) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty-line">참가 신청한 대회가 없습니다.</p>

      <!-- 내 후기 -->
      <h4 class="sub">내 후기</h4>
      <table class="grid" v-if="summary.reviews.length">
        <thead><tr><th>제목</th><th>상태</th><th>작성일</th></tr></thead>
        <tbody>
          <tr v-for="r in summary.reviews" :key="r.postId">
            <td>{{ r.title }}</td>
            <td>{{ r.hidden ? '숨김(운영자)' : '공개' }}</td>
            <td>{{ dt(r.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty-line">작성한 후기가 없습니다.</p>

      <!-- 내 굿즈 주문 -->
      <h4 class="sub">내 굿즈 주문</h4>
      <table class="grid" v-if="summary.orders.length">
        <thead><tr><th>상품</th><th>수량</th><th>금액</th><th>상태</th><th>주문일</th></tr></thead>
        <tbody>
          <tr v-for="o in summary.orders" :key="o.orderId">
            <td>{{ o.goodsName }}</td><td>{{ o.quantity }}</td><td>{{ won(o.totalAmount) }}</td>
            <td><span class="pill" :class="o.status">{{ orderStatusLabel[o.status] }}</span></td>
            <td>{{ dt(o.createdAt) }}</td>
          </tr>
        </tbody>
      </table>
      <p v-else class="empty-line">주문 내역이 없습니다.</p>
    </template>
  </div>
</template>

<style scoped>
.profile { display: flex; gap: 20px; align-items: center; margin-bottom: 12px; }
.avatar { width: 88px; height: 88px; border-radius: 50%; overflow: hidden; flex: none; }
.avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar .placeholder { width: 100%; height: 100%; border-radius: 50%; }
.info .nick { font-size: 22px; font-weight: 800; color: var(--text-strong); margin: 0 0 6px; }
.info .row { margin: 2px 0; color: var(--text-body); }

.sub { margin: 34px 0 10px; color: var(--text-strong); }
.grid { width: 100%; border-collapse: collapse; font-size: 14px; }
.grid th, .grid td { border-bottom: 1px solid #eee; padding: 9px 10px; text-align: left; }
.grid th { color: var(--text-muted); font-weight: 600; }
.key { font-family: monospace; }
.empty-line { color: var(--text-muted); font-size: 14px; }
.hint { margin-top: 8px; color: var(--text-muted); font-size: 13px; }

.pill { display: inline-block; font-size: 12px; font-weight: 700; padding: 3px 10px; border-radius: 999px; background: #f0f0f0; color: var(--text-body); }
.pill.APPROVED, .pill.PAID { background: #e8f5e9; color: #2e7d32; }
.pill.PENDING { background: #fff3e0; color: #e65100; }
.pill.REJECTED, .pill.CANCELLED, .pill.FAILED { background: #fdecea; color: #c62828; }
</style>
