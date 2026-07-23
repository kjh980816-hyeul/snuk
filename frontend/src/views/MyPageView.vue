<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { authApi, mypageApi } from '@/api'
import type { MypageSummary } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const summary = ref<MypageSummary | null>(null)
const loading = ref(true)

type MpTab = 'apply' | 'tour' | 'codes' | 'reviews' | 'orders'
const tab = ref<MpTab>('apply')

// ----- 프사 변경 (파일 업로드) -----
const fileEl = ref<HTMLInputElement | null>(null)
const picSaving = ref(false)
async function onPicFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || picSaving.value) return
  if (file.size > 5 * 1024 * 1024) {
    alert('이미지는 5MB 이하만 업로드할 수 있어요.')
    input.value = ''
    return
  }
  picSaving.value = true
  try {
    await authApi.uploadProfileImage(file)
    await auth.fetchMe()
  } catch {
    alert('업로드에 실패했어요. 이미지 파일(jpg/png/gif/webp)인지 확인해주세요.')
  } finally {
    picSaving.value = false
    input.value = ''
  }
}
async function resetPic() {
  if (picSaving.value) return
  picSaving.value = true
  try {
    await authApi.updateProfileImage(null)
    await auth.fetchMe()
  } finally {
    picSaving.value = false
  }
}

const roleLabel: Record<string, string> = {
  VIEWER: '시청자', STREAMER: '스트리머', REPORTER: '기자', ADMIN: '관리자', GUEST: '게스트',
}
const appStatusLabel: Record<string, string> = {
  PENDING: '승인 대기', APPROVED: '확정', REJECTED: '거절',
}
const orderStatusLabel: Record<string, string> = {
  PENDING: '결제 대기', PAID: '결제 완료', CANCELLED: '취소', FAILED: '실패',
}

const keyItems = computed(() =>
  (summary.value?.applications ?? []).filter((a) => a.hasAssignedKey))

function dt(v: string | null | undefined) {
  return v ? v.slice(0, 16).replace('T', ' ') : '-'
}
function won(v: number) {
  return v.toLocaleString('ko-KR') + '원'
}

// ----- 후기 마감(키 수령 +30일) — 남은 날짜 표시 + 게임당 1회 7일 연장 -----
function daysLeft(deadline: string | null): number | null {
  if (!deadline) return null
  return Math.ceil((new Date(deadline).getTime() - Date.now()) / 86_400_000)
}
function deadlineText(a: { reviewDeadline: string | null; reviewWritten: boolean; warned: boolean }): string {
  if (!a.reviewDeadline) return ''
  if (a.reviewWritten) return '후기 작성 완료 ✅'
  const d = daysLeft(a.reviewDeadline)
  if (d === null) return ''
  if (d < 0) return `후기 마감 ${-d}일 지남 ⚠️`
  if (d === 0) return '오늘이 후기 마감일!'
  return `후기 마감까지 D-${d}`
}
const extending = ref(false)
async function extendDeadline(applicationId: number) {
  if (extending.value) return
  if (!confirm('후기 마감을 7일 연장할까요? (게임당 1번만 가능)')) return
  extending.value = true
  try {
    await mypageApi.extendDeadline(applicationId)
    summary.value = await mypageApi.summary()
  } catch (e) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    alert(msg ?? '연장에 실패했어요.')
  } finally {
    extending.value = false
  }
}

// ----- 스포트라이트 등록(내 방송 홍보하기) — 셸 전역 모달 재사용 -----
function promoteMyStream() {
  const open = (window as unknown as { openSpotlight?: () => void }).openSpotlight
  open?.()
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
  <section v-if="auth.me" class="mypage">
    <div class="inner">
      <div class="section-header">
        <h2 class="section-title">마이페이지</h2>
      </div>

      <!-- 프로필 카드 -->
      <div class="mp-profile-card">
        <div class="mp-avatar">
          <img v-if="auth.me.profileImageUrl" :src="auth.me.profileImageUrl" alt="프로필" />
          <span v-else>{{ auth.me.nickname.slice(0, 1) }}</span>
        </div>
        <div class="mp-info">
          <div class="mp-name-row">
            <span class="mp-nick">{{ auth.me.nickname }}</span>
            <span class="mp-role" :class="auth.me.role">{{ roleLabel[auth.me.role] ?? auth.me.role }}</span>
          </div>
          <div class="mp-sub" v-if="auth.me.followerCount !== null">
            팔로워 {{ auth.me.followerCount?.toLocaleString('ko-KR') }}
          </div>
          <div class="mp-sub">
            🪙 내 포인트 <b>{{ (auth.me.points ?? 0).toLocaleString('ko-KR') }}P</b>
            <span style="opacity:.7;"> — 매일 첫 로그인마다 적립, 스포트라이트 등록에 사용</span>
          </div>
          <div class="mp-pic-edit">
            <input ref="fileEl" type="file" accept="image/jpeg,image/png,image/gif,image/webp" class="hidden-file" @change="onPicFile" />
            <button class="mp-btn" :disabled="picSaving" @click="fileEl?.click()">
              {{ picSaving ? '업로드 중…' : '프사 변경' }}
            </button>
            <button v-if="auth.me.profileImageOverridden" class="mp-btn ghost" :disabled="picSaving" @click="resetPic">
              플랫폼 프사로
            </button>
            <button class="mp-btn promote" @click="promoteMyStream">📣 내 방송 홍보하기</button>
          </div>
          <div class="mp-promote-hint">스포트라이트에 내 방송을 홍보할 수 있어요 (승인된 스트리머, 최대 2시간 노출)</div>
        </div>
        <!-- 활동 요약 타일 -->
        <div class="mp-stats" v-if="summary">
          <div class="mp-stat"><strong>{{ summary.applications.length }}</strong><span>컨텐츠 신청</span></div>
          <div class="mp-stat"><strong>{{ summary.tournaments.length }}</strong><span>대회 참가</span></div>
          <div class="mp-stat"><strong>{{ summary.reviews.length }}</strong><span>작성 후기</span></div>
          <div class="mp-stat"><strong>{{ summary.orders.length }}</strong><span>굿즈 주문</span></div>
        </div>
      </div>

      <div v-if="loading" class="mp-empty">불러오는 중…</div>
      <template v-else-if="summary">
        <!-- 탭 -->
        <div class="tabs mp-tabs">
          <button class="tab" :class="{ active: tab === 'apply' }" @click="tab = 'apply'">신청 현황</button>
          <button class="tab" :class="{ active: tab === 'tour' }" @click="tab = 'tour'">대회 참가</button>
          <button class="tab" :class="{ active: tab === 'codes' }" @click="tab = 'codes'">게임 코드</button>
          <button class="tab" :class="{ active: tab === 'reviews' }" @click="tab = 'reviews'">내 후기</button>
          <button class="tab" :class="{ active: tab === 'orders' }" @click="tab = 'orders'">굿즈 주문</button>
        </div>

        <!-- 신청 현황 -->
        <div v-if="tab === 'apply'">
          <div v-if="!summary.applications.length" class="mp-empty">신청한 컨텐츠가 없습니다.<br>컨텐츠에 신청해보세요!</div>
          <div v-for="a in summary.applications" :key="a.applicationId" class="mp-item">
            <div class="mp-item-main">
              <div class="mp-item-title">{{ a.campaignTitle }}</div>
              <div class="mp-item-sub">신청일 {{ dt(a.appliedAt) }}</div>
              <div v-if="a.reviewDeadline" class="mp-deadline" :class="{ danger: !a.reviewWritten && (daysLeft(a.reviewDeadline) ?? 0) <= 3, done: a.reviewWritten }">
                {{ deadlineText(a) }}
              </div>
            </div>
            <span class="mp-pill" :class="a.status">{{ appStatusLabel[a.status] }}</span>
          </div>
        </div>

        <!-- 대회 참가 -->
        <div v-else-if="tab === 'tour'">
          <div v-if="!summary.tournaments.length" class="mp-empty">참가 신청한 대회가 없습니다.</div>
          <div v-for="t in summary.tournaments" :key="t.participantId" class="mp-item">
            <div class="mp-item-main">
              <div class="mp-item-title">{{ t.tournamentTitle }}</div>
              <div class="mp-item-sub">신청일 {{ dt(t.appliedAt) }}</div>
            </div>
            <span class="mp-pill" :class="t.status">{{ appStatusLabel[t.status] }}</span>
          </div>
        </div>

        <!-- 게임 코드 -->
        <div v-else-if="tab === 'codes'">
          <div v-if="!keyItems.length" class="mp-empty">배정받은 게임 코드가 없습니다.<br>운영자가 코드를 전달하면 여기서 확인할 수 있습니다.</div>
          <div v-for="a in keyItems" :key="'k' + a.applicationId" class="mp-item">
            <div class="mp-item-main">
              <div class="mp-item-title">{{ a.campaignTitle }}</div>
              <div class="mp-item-code">{{ a.maskedKey }}</div>
              <div class="mp-item-sub">코드 전체 보기는 해당 컨텐츠 페이지에서 본인 확인 후 가능합니다.</div>
              <div v-if="a.reviewDeadline" class="mp-deadline" :class="{ danger: !a.reviewWritten && (daysLeft(a.reviewDeadline) ?? 0) <= 3, done: a.reviewWritten }">
                {{ deadlineText(a) }}
                <span v-if="!a.reviewWritten" class="mp-deadline-date">({{ a.reviewDeadline.slice(0, 10) }}까지)</span>
                <span v-if="a.warned && !a.reviewWritten" class="mp-warn-chip">경고</span>
              </div>
            </div>
            <div class="mp-item-side">
              <span class="mp-pill APPROVED">코드 배정</span>
              <template v-if="a.reviewDeadline && !a.reviewWritten">
                <RouterLink :to="`/campaigns/${a.campaignId}/reviews`" class="mp-btn small">후기 쓰러 가기</RouterLink>
                <button v-if="!a.deadlineExtended" class="mp-btn small ghost" :disabled="extending"
                  @click="extendDeadline(a.applicationId)">7일 연장 (1회)</button>
                <span v-else class="mp-extended">연장 사용됨</span>
              </template>
            </div>
          </div>
        </div>

        <!-- 내 후기 -->
        <div v-else-if="tab === 'reviews'">
          <div v-if="!summary.reviews.length" class="mp-empty">작성한 후기가 없습니다.</div>
          <div v-for="r in summary.reviews" :key="r.postId" class="mp-item">
            <div class="mp-item-main">
              <div class="mp-item-title">{{ r.title }}</div>
              <div class="mp-item-sub">{{ dt(r.createdAt) }} · {{ r.hidden ? '숨김(운영자)' : '공개' }}</div>
            </div>
            <RouterLink v-if="r.campaignId" :to="`/campaigns/${r.campaignId}/reviews`" class="mp-link">게시판 ›</RouterLink>
          </div>
        </div>

        <!-- 굿즈 주문 -->
        <div v-else-if="tab === 'orders'">
          <div v-if="!summary.orders.length" class="mp-empty">주문 내역이 없습니다.</div>
          <div v-for="o in summary.orders" :key="o.orderId" class="mp-item">
            <div class="mp-item-main">
              <div class="mp-item-title">{{ o.goodsName }} × {{ o.quantity }}</div>
              <div class="mp-item-sub">{{ dt(o.createdAt) }} · {{ won(o.totalAmount) }}</div>
            </div>
            <span class="mp-pill" :class="o.status">{{ orderStatusLabel[o.status] }}</span>
          </div>
        </div>
      </template>
    </div>
  </section>
</template>

<style scoped>
/* 시안 다크 테마 — home-snuk.css 변수 사용 */
.mp-profile-card {
  display: flex; align-items: center; gap: 20px; flex-wrap: wrap;
  background: var(--bg2); border: 1px solid var(--border); border-radius: 16px;
  padding: 22px 24px; margin-bottom: 24px;
}
.mp-avatar {
  width: 84px; height: 84px; border-radius: 50%; overflow: hidden; flex: none;
  background: linear-gradient(135deg, var(--accent), var(--accent2));
  display: flex; align-items: center; justify-content: center;
  font-size: 30px; font-weight: 700; color: #fff; border: 2px solid var(--border2);
}
.mp-avatar img { width: 100%; height: 100%; object-fit: cover; }
.mp-info { min-width: 200px; }
.mp-name-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.mp-nick { font-size: 20px; font-weight: 800; color: var(--text); }
.mp-role {
  font-size: 10px; font-weight: 700; padding: 3px 9px; border-radius: 20px;
  background: var(--bg3); color: var(--text2); border: 1px solid var(--border);
}
.mp-role.STREAMER { background: rgba(0, 199, 60, .12); color: #00c73c; border-color: rgba(0, 199, 60, .35); }
.mp-role.ADMIN { background: rgba(255, 179, 0, .12); color: #ffb300; border-color: rgba(255, 179, 0, .35); }
.mp-sub { font-size: 13px; color: var(--text3); margin-top: 5px; }
.mp-pic-edit { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
.hidden-file { display: none; }
.mp-btn {
  padding: 8px 14px; font-size: 12px; font-weight: 700; border-radius: 8px; border: none; cursor: pointer;
  background: linear-gradient(135deg, var(--accent), var(--accent2)); color: #fff;
}
.mp-btn.ghost { background: transparent; color: var(--text2); border: 1px solid var(--border); }
.mp-btn.promote { background: linear-gradient(135deg, #00c73c, #00a832); }
.mp-promote-hint { font-size: 11px; color: var(--text3); margin-top: 8px; }
.mp-btn:disabled { opacity: .5; cursor: not-allowed; }

.mp-stats { display: flex; gap: 10px; margin-left: auto; flex-wrap: wrap; }
.mp-stat {
  min-width: 84px; background: var(--bg3); border: 1px solid var(--border); border-radius: 12px;
  padding: 12px 14px; text-align: center;
}
.mp-stat strong { display: block; font-size: 20px; font-weight: 800; color: var(--text); }
.mp-stat span { font-size: 11px; color: var(--text3); }

.mp-tabs { margin-bottom: 16px; }

.mp-item {
  display: flex; align-items: center; gap: 14px;
  background: var(--bg2); border: 1px solid var(--border); border-radius: 12px;
  padding: 14px 18px; margin-bottom: 10px;
}
.mp-item-main { flex: 1; min-width: 0; }
.mp-item-title { font-size: 14px; font-weight: 700; color: var(--text); }
.mp-item-sub { font-size: 12px; color: var(--text3); margin-top: 3px; }
.mp-item-code {
  font-family: monospace; font-size: 13px; color: var(--gold, #ffb300);
  background: var(--bg3); border-radius: 6px; padding: 4px 10px; display: inline-block; margin-top: 6px;
}
.mp-pill {
  flex: none; font-size: 11px; font-weight: 700; padding: 4px 12px; border-radius: 20px;
  background: var(--bg3); color: var(--text2); border: 1px solid var(--border);
}
.mp-pill.APPROVED, .mp-pill.PAID { background: rgba(52, 199, 120, .12); color: #34c878; border-color: rgba(52, 199, 120, .35); }
.mp-pill.PENDING { background: rgba(255, 179, 0, .12); color: #ffb300; border-color: rgba(255, 179, 0, .35); }
.mp-pill.REJECTED, .mp-pill.CANCELLED, .mp-pill.FAILED { background: rgba(239, 68, 68, .12); color: #ef4444; border-color: rgba(239, 68, 68, .35); }

/* 후기 마감/연장(항목 19) */
.mp-deadline { margin-top: 7px; font-size: 12px; font-weight: 700; color: #8fa8ff; }
.mp-deadline.danger { color: #ff7070; }
.mp-deadline.done { color: #34c878; }
.mp-deadline-date { font-weight: 500; color: var(--text3); margin-left: 4px; }
.mp-warn-chip { display: inline-block; margin-left: 6px; font-size: 10px; font-weight: 800; color: #fff;
  background: #ef4444; border-radius: 5px; padding: 1px 6px; vertical-align: 1px; }
.mp-item-side { display: flex; flex-direction: column; align-items: flex-end; gap: 6px; flex-shrink: 0; }
.mp-btn.small { font-size: 11px; padding: 5px 10px; text-decoration: none; text-align: center; }
.mp-extended { font-size: 11px; color: var(--text3); }
.mp-link { flex: none; font-size: 12px; font-weight: 700; color: var(--accent3, #5cf0fc); text-decoration: none; }

.mp-empty {
  border: 1px dashed var(--border2); border-radius: 12px; padding: 36px 16px;
  text-align: center; color: var(--text3); font-size: 13px; line-height: 1.8;
}
</style>
