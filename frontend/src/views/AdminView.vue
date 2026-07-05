<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi } from '@/api/admin'
import { campaignApi, collabApi, tournamentApi } from '@/api'
import type { Campaign, CollabGame, ContentVideo, ClientLogo, Goods, OrderView, Tournament } from '@/api/types'

type Tab = 'campaigns' | 'tournaments' | 'collab' | 'goods' | 'members' | 'settings' | 'logs'
const tab = ref<Tab>('campaigns')

// 화면 표기용 한글 라벨 (저장값은 영문 enum 그대로)
const ko: Record<string, string> = {
  SCHEDULED: '예정', OPEN: '모집중', CLOSED: '마감', DONE: '종료',
  FCFS: '선착순', APPROVAL: '승인제',
  QUANTITY: '수량만(키 없음)', UNIQUE_KEY: '고유 키 배포',
  AVAILABLE: '미배정', ASSIGNED: '배정됨', REVOKED: '무효',
  PENDING: '대기', APPROVED: '승인', REJECTED: '거절',
}
const lbl = (v: string | undefined | null) => (v ? (ko[v] ?? v) : '-')

// ----- campaigns -----
const campaigns = ref<Campaign[]>([])
const editing = ref<Partial<Campaign> | null>(null)
const selected = ref<Campaign | null>(null)
const keys = ref<Array<{ id: number; maskedKey: string; status: string; assignedMemberId: number | null }>>([])
const rawKeys = ref('')
const keyResult = ref<string | null>(null)
const applications = ref<Array<{ applicationId: number; memberId: number; status: string; followerSnapshot: number }>>([])

async function loadCampaigns() {
  campaigns.value = await campaignApi.list()
}
function newCampaign() {
  editing.value = {
    title: '', description: '', gameName: '', status: 'SCHEDULED',
    distributionType: 'FCFS', keyMode: 'QUANTITY', totalSlots: 0, featured: false,
  }
}
function editCampaign(c: Campaign) {
  editing.value = { ...c }
}
async function saveCampaign() {
  if (!editing.value) return
  const body = editing.value
  if (body.id) await adminApi.updateCampaign(body.id, body)
  else await adminApi.createCampaign(body)
  editing.value = null
  await loadCampaigns()
}
async function removeCampaign(c: Campaign) {
  if (!confirm(`'${c.title}' 캠페인을 삭제할까요?`)) return
  await adminApi.deleteCampaign(c.id)
  if (selected.value?.id === c.id) selected.value = null
  await loadCampaigns()
}
async function selectCampaign(c: Campaign) {
  selected.value = c
  keyResult.value = null
  rawKeys.value = ''
  keys.value = c.keyMode === 'UNIQUE_KEY' ? await adminApi.listKeys(c.id) : []
  applications.value = await adminApi.applications(c.id)
}
async function submitKeys() {
  if (!selected.value) return
  const res = await adminApi.registerKeys(selected.value.id, rawKeys.value)
  keyResult.value = `등록 ${res.registered} · 중복 ${res.duplicated} · 빈줄 ${res.blank} (가용 ${res.totalAvailable})`
  rawKeys.value = ''
  keys.value = await adminApi.listKeys(selected.value.id)
}
async function delKey(keyId: number) {
  if (!selected.value) return
  await adminApi.deleteKey(selected.value.id, keyId)
  keys.value = await adminApi.listKeys(selected.value.id)
}
async function revokeKey(keyId: number) {
  if (!selected.value) return
  await adminApi.revokeKey(selected.value.id, keyId)
  keys.value = await adminApi.listKeys(selected.value.id)
}
async function approveApp(id: number) {
  await adminApi.approve(id)
  if (selected.value) applications.value = await adminApi.applications(selected.value.id)
  await loadCampaigns()
}
async function rejectApp(id: number) {
  await adminApi.reject(id)
  if (selected.value) applications.value = await adminApi.applications(selected.value.id)
}

// ----- tournaments -----
const tournaments = ref<Tournament[]>([])
const tourEditing = ref<Partial<Tournament> | null>(null)
const tourSelected = ref<Tournament | null>(null)
const participants = ref<Array<{ participantId: number; memberId: number; status: string; followerSnapshot: number }>>([])

async function loadTournaments() {
  tournaments.value = await tournamentApi.list()
}
function newTournament() {
  tourEditing.value = {
    title: '', description: '', gameName: '', status: 'SCHEDULED',
    capacity: 0, featured: false, sortOrder: tournaments.value.length,
  }
}
function editTournament(t: Tournament) {
  tourEditing.value = { ...t }
}
async function saveTournament() {
  if (!tourEditing.value) return
  const b = tourEditing.value
  if (b.id) await adminApi.updateTournament(b.id, b)
  else await adminApi.createTournament(b)
  tourEditing.value = null
  await loadTournaments()
}
async function removeTournament(t: Tournament) {
  if (!confirm(`'${t.title}' 대회를 삭제할까요?`)) return
  await adminApi.deleteTournament(t.id)
  if (tourSelected.value?.id === t.id) tourSelected.value = null
  await loadTournaments()
}
async function selectTournament(t: Tournament) {
  tourSelected.value = t
  participants.value = await adminApi.participants(t.id)
}
async function approveParticipant(id: number) {
  await adminApi.approveParticipant(id)
  if (tourSelected.value) participants.value = await adminApi.participants(tourSelected.value.id)
  await loadTournaments()
}
async function rejectParticipant(id: number) {
  await adminApi.rejectParticipant(id)
  if (tourSelected.value) participants.value = await adminApi.participants(tourSelected.value.id)
}

// ----- collab (게임/영상/클라이언트: 추가+수정+삭제 모두 지원) -----
const games = ref<CollabGame[]>([])
const videos = ref<ContentVideo[]>([])
const clients = ref<ClientLogo[]>([])
const gameEditing = ref<Partial<CollabGame> | null>(null)
const videoEditing = ref<Partial<ContentVideo> | null>(null)
const logoEditing = ref<Partial<ClientLogo> | null>(null)
async function loadCollab() {
  ;[games.value, videos.value, clients.value] = await Promise.all([
    collabApi.games(), collabApi.videos(), collabApi.clients(),
  ])
}
// 게임
function newGame() {
  gameEditing.value = { name: '', description: '', thumbnailUrl: '', gameLinkUrl: '', reviewLinkUrl: '', campaignId: null, sortOrder: games.value.length }
}
function editGame(g: CollabGame) { gameEditing.value = { ...g } }
async function saveGame() {
  const b = gameEditing.value
  if (!b) return
  if (b.id) await adminApi.updateGame(b.id, b); else await adminApi.createGame(b)
  gameEditing.value = null
  await loadCollab()
}
async function delGame(id: number) {
  if (!confirm('이 콜라보 게임을 삭제할까요?')) return
  await adminApi.deleteGame(id)
  await loadCollab()
}
// 영상
function newVideo() {
  videoEditing.value = { title: '', videoUrl: '', thumbnailUrl: '', featured: false, sortOrder: videos.value.length }
}
function editVideo(v: ContentVideo) { videoEditing.value = { ...v } }
async function saveVideo() {
  const b = videoEditing.value
  if (!b) return
  if (b.id) await adminApi.updateVideo(b.id, b); else await adminApi.createVideo(b)
  videoEditing.value = null
  await loadCollab()
}
async function delVideo(id: number) {
  if (!confirm('이 영상을 삭제할까요?')) return
  await adminApi.deleteVideo(id)
  await loadCollab()
}
// 클라이언트 로고
function newLogo() {
  logoEditing.value = { name: '', logoUrl: '', linkUrl: '', sortOrder: clients.value.length }
}
function editLogo(l: ClientLogo) { logoEditing.value = { ...l } }
// 공용 이미지 파일 업로드 → 대상 객체의 필드에 /uploads/ 경로 주입 (모든 이미지 입력 공용)
const imgUploading = ref(false)
async function pickImage(e: Event, obj: object | null, field: string) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || !obj) return
  if (file.size > 5 * 1024 * 1024) { alert('이미지는 5MB 이하만 업로드할 수 있어요.'); input.value = ''; return }
  imgUploading.value = true
  try {
    const { url } = await adminApi.uploadImage(file)
    ;(obj as Record<string, unknown>)[field] = url
  } catch {
    alert('업로드 실패 — 이미지 파일(jpg/png/gif/webp)인지 확인해주세요.')
  } finally {
    imgUploading.value = false
    input.value = ''
  }
}
async function saveLogo() {
  const b = logoEditing.value
  if (!b) return
  if (b.id) await adminApi.updateLogo(b.id, b); else await adminApi.createLogo(b)
  logoEditing.value = null
  await loadCollab()
}
async function delLogo(id: number) {
  if (!confirm('이 클라이언트 로고를 삭제할까요?')) return
  await adminApi.deleteLogo(id)
  await loadCollab()
}

// ----- goods -----
const goodsList = ref<Goods[]>([])
const goodsEditing = ref<Partial<Goods> | null>(null)
const orders = ref<OrderView[]>([])
async function loadGoods() {
  ;[goodsList.value, orders.value] = await Promise.all([adminApi.listGoods(), adminApi.orders()])
}
function newGoods() {
  goodsEditing.value = { name: '', description: '', imageUrl: '', price: 0, stock: 0, status: 'ACTIVE', sortOrder: goodsList.value.length }
}
function editGoods(g: Goods) {
  goodsEditing.value = { ...g }
}
async function saveGoods() {
  if (!goodsEditing.value) return
  const b = goodsEditing.value
  if (b.id) await adminApi.updateGoods(b.id, b)
  else await adminApi.createGoods(b)
  goodsEditing.value = null
  await loadGoods()
}
async function removeGoods(g: Goods) {
  if (!confirm(`'${g.name}' 굿즈를 삭제할까요?`)) return
  await adminApi.deleteGoods(g.id)
  await loadGoods()
}
function won(v: number) {
  return v.toLocaleString('ko-KR') + '원'
}

// ----- members -----
interface MemberRow {
  id: number
  chzzkChannelId: string
  nickname: string
  profileImageUrl: string | null
  followerCount: number | null
  role: string
  roleOverridden: boolean
  createdAt: string
}
const members = ref<MemberRow[]>([])
async function loadMembers() {
  members.value = (await adminApi.members()).content
}
async function changeMemberRole(m: MemberRow, role: string) {
  if (role === m.role) return
  await adminApi.overrideRole(m.id, role)
  await loadMembers()
}
async function resetMemberRole(m: MemberRow) {
  await adminApi.clearOverride(m.id)
  await loadMembers()
}

// ----- settings -----
const settings = ref<Array<{ settingKey: string; settingValue: string; description: string | null }>>([])
async function loadSettings() {
  settings.value = await adminApi.settings()
}
async function saveSetting(key: string, value: string) {
  await adminApi.updateSetting(key, value)
  await loadSettings()
}
const overrideMemberId = ref<number | null>(null)
const overrideRole = ref('STREAMER')
async function doOverride() {
  if (!overrideMemberId.value) return
  await adminApi.overrideRole(overrideMemberId.value, overrideRole.value)
  alert('권한이 변경되었습니다.')
}

// ----- logs -----
const logs = ref<{ content: Array<Record<string, unknown>> } | null>(null)
async function loadLogs() {
  logs.value = await adminApi.logs()
}

onMounted(loadCampaigns)

function onTab(t: Tab) {
  tab.value = t
  if (t === 'tournaments') loadTournaments()
  if (t === 'collab') loadCollab()
  if (t === 'goods') loadGoods()
  if (t === 'members') loadMembers()
  if (t === 'settings') loadSettings()
  if (t === 'logs') loadLogs()
}
</script>

<template>
  <div class="wrap admin section">
    <h2 class="section-label">관리자</h2>
    <nav class="tabs">
      <button :class="{ on: tab === 'campaigns' }" @click="onTab('campaigns')">캠페인</button>
      <button :class="{ on: tab === 'tournaments' }" @click="onTab('tournaments')">대회</button>
      <button :class="{ on: tab === 'collab' }" @click="onTab('collab')">콜라보/노출</button>
      <button :class="{ on: tab === 'goods' }" @click="onTab('goods')">굿즈/주문</button>
      <button :class="{ on: tab === 'members' }" @click="onTab('members')">회원</button>
      <button :class="{ on: tab === 'settings' }" @click="onTab('settings')">설정/권한</button>
      <button :class="{ on: tab === 'logs' }" @click="onTab('logs')">감사로그</button>
    </nav>

    <!-- 캠페인 -->
    <section v-if="tab === 'campaigns'">
      <button class="btn orange sm" @click="newCampaign">+ 새 캠페인</button>
      <table class="grid">
        <thead><tr><th>제목</th><th>상태</th><th>배포</th><th>키모드</th><th>슬롯</th><th>대표</th><th></th></tr></thead>
        <tbody>
          <tr v-for="c in campaigns" :key="c.id" :class="{ sel: selected?.id === c.id }">
            <td>{{ c.title }}</td><td>{{ lbl(c.status) }}</td><td>{{ lbl(c.distributionType) }}</td>
            <td>{{ lbl(c.keyMode) }}</td><td>{{ c.filledSlots }}/{{ c.totalSlots }}</td>
            <td>{{ c.featured ? '★' : '' }}</td>
            <td class="acts">
              <button @click="selectCampaign(c)">관리</button>
              <button @click="editCampaign(c)">수정</button>
              <button class="danger" @click="removeCampaign(c)">삭제</button>
            </td>
          </tr>
          <tr v-if="!campaigns.length"><td colspan="7" class="empty">등록된 캠페인이 없습니다.</td></tr>
        </tbody>
      </table>

      <!-- 편집 폼 -->
      <div v-if="editing" class="form-card">
        <h4>{{ editing.id ? '캠페인 수정' : '새 캠페인' }}</h4>
        <label>제목<input v-model="editing.title" /></label>
        <label>설명<textarea v-model="editing.description"></textarea></label>
        <label>게임명<input v-model="editing.gameName" /></label>
        <label>홍보 이미지
          <div class="logo-upload">
            <img v-if="editing.promoImageUrl" :src="editing.promoImageUrl" class="logo-preview" alt="" />
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="pickImage($event, editing, 'promoImageUrl')" />
            <span v-if="imgUploading">업로드 중…</span>
          </div>
        </label>
        <div class="row3">
          <label>상태
            <select v-model="editing.status">
              <option value="SCHEDULED">예정</option><option value="OPEN">모집중</option><option value="CLOSED">마감</option>
            </select>
          </label>
          <label>배포방식
            <select v-model="editing.distributionType">
              <option value="FCFS">선착순 (신청 즉시 확정)</option>
              <option value="APPROVAL">승인제 (관리자 승인 후 확정)</option>
            </select>
          </label>
          <label>키모드
            <select v-model="editing.keyMode">
              <option value="QUANTITY">수량만 (키 배포 없음)</option>
              <option value="UNIQUE_KEY">고유 키 배포 (키 등록 필요)</option>
            </select>
          </label>
        </div>
        <div class="row3">
          <label>슬롯 수<input type="number" v-model.number="editing.totalSlots" /></label>
          <label class="chk"><input type="checkbox" v-model="editing.featured" /> 홈 대표</label>
        </div>
        <div class="form-acts">
          <button class="btn sm" @click="saveCampaign">저장</button>
          <button class="btn ghost sm" @click="editing = null">취소</button>
        </div>
      </div>

      <!-- 선택 캠페인 관리: 키 + 신청자 -->
      <div v-if="selected" class="manage">
        <h4>‘{{ selected.title }}’ 관리</h4>

        <div v-if="selected.keyMode === 'UNIQUE_KEY'" class="keys">
          <h5>게임 키 (붙여넣기 일괄 등록)</h5>
          <textarea v-model="rawKeys" placeholder="한 줄에 키 하나씩 붙여넣기"></textarea>
          <button class="btn sm" @click="submitKeys">등록</button>
          <p v-if="keyResult" class="result">{{ keyResult }}</p>
          <table class="grid">
            <thead><tr><th>키(마스킹)</th><th>상태</th><th>배정대상</th><th></th></tr></thead>
            <tbody>
              <tr v-for="k in keys" :key="k.id">
                <td>{{ k.maskedKey }}</td><td>{{ lbl(k.status) }}</td><td>{{ k.assignedMemberId ?? '-' }}</td>
                <td class="acts">
                  <button v-if="k.status === 'AVAILABLE'" class="danger" @click="delKey(k.id)">삭제</button>
                  <button v-if="k.status === 'ASSIGNED'" @click="revokeKey(k.id)">무효화</button>
                </td>
              </tr>
              <tr v-if="!keys.length"><td colspan="4" class="empty">등록된 키가 없습니다.</td></tr>
            </tbody>
          </table>
        </div>
        <p v-else class="hint">
          이 캠페인은 "수량만" 모드라 게임 키 등록이 없습니다. 키를 배포하려면 캠페인 수정에서
          키모드를 "고유 키 배포"로 바꾸면 여기에 키 등록 칸이 생깁니다.
        </p>

        <div class="apps">
          <h5>신청자</h5>
          <table class="grid">
            <thead><tr><th>회원</th><th>팔로워(스냅샷)</th><th>상태</th><th></th></tr></thead>
            <tbody>
              <tr v-for="a in applications" :key="a.applicationId">
                <td>#{{ a.memberId }}</td><td>{{ a.followerSnapshot }}</td><td>{{ lbl(a.status) }}</td>
                <td class="acts">
                  <template v-if="a.status === 'PENDING'">
                    <button @click="approveApp(a.applicationId)">승인</button>
                    <button class="danger" @click="rejectApp(a.applicationId)">거절</button>
                  </template>
                </td>
              </tr>
              <tr v-if="!applications.length"><td colspan="4" class="empty">신청자가 없습니다.</td></tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <!-- 대회 -->
    <section v-else-if="tab === 'tournaments'">
      <button class="btn orange sm" @click="newTournament">+ 새 대회</button>
      <table class="grid">
        <thead><tr><th>대회명</th><th>게임</th><th>대회일</th><th>상태</th><th>정원</th><th>대표</th><th></th></tr></thead>
        <tbody>
          <tr v-for="t in tournaments" :key="t.id" :class="{ sel: tourSelected?.id === t.id }">
            <td>{{ t.title }}</td><td>{{ t.gameName }}</td><td>{{ t.eventDate ?? '-' }}</td>
            <td>{{ lbl(t.status) }}</td><td>{{ t.filledSlots }}/{{ t.capacity }}</td>
            <td>{{ t.featured ? '★' : '' }}</td>
            <td class="acts">
              <button @click="selectTournament(t)">참가자</button>
              <button @click="editTournament(t)">수정</button>
              <button class="danger" @click="removeTournament(t)">삭제</button>
            </td>
          </tr>
          <tr v-if="!tournaments.length"><td colspan="7" class="empty">등록된 대회가 없습니다.</td></tr>
        </tbody>
      </table>

      <!-- 편집 폼 (결과입력 포함) -->
      <div v-if="tourEditing" class="form-card">
        <h4>{{ tourEditing.id ? '대회 수정' : '새 대회' }}</h4>
        <label>대회명<input v-model="tourEditing.title" /></label>
        <label>설명<textarea v-model="tourEditing.description"></textarea></label>
        <label>게임명<input v-model="tourEditing.gameName" /></label>
        <label>배너 이미지
          <div class="logo-upload">
            <img v-if="tourEditing.bannerImageUrl" :src="tourEditing.bannerImageUrl" class="logo-preview" alt="" />
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="pickImage($event, tourEditing, 'bannerImageUrl')" />
            <span v-if="imgUploading">업로드 중…</span>
          </div>
        </label>
        <div class="row3">
          <label>상태
            <select v-model="tourEditing.status">
              <option value="SCHEDULED">예정</option><option value="OPEN">모집중</option>
              <option value="CLOSED">마감</option><option value="DONE">종료(결과 노출)</option>
            </select>
          </label>
          <label>참가 정원<input type="number" v-model.number="tourEditing.capacity" /></label>
          <label class="chk"><input type="checkbox" v-model="tourEditing.featured" /> 대표 대회</label>
        </div>
        <label>대회 결과 (DONE 상태에서 페이지에 노출)
          <textarea v-model="tourEditing.resultText" placeholder="예) 우승: 팀 알파 / MVP: 스트리머A"></textarea>
        </label>
        <label>정렬순서<input type="number" v-model.number="tourEditing.sortOrder" /></label>
        <div class="form-acts">
          <button class="btn sm" @click="saveTournament">저장</button>
          <button class="btn ghost sm" @click="tourEditing = null">취소</button>
        </div>
      </div>

      <!-- 선택 대회: 참가 신청자 승인/거절 -->
      <div v-if="tourSelected" class="manage">
        <h4>‘{{ tourSelected.title }}’ 참가 신청자</h4>
        <table class="grid">
          <thead><tr><th>회원</th><th>팔로워(스냅샷)</th><th>상태</th><th></th></tr></thead>
          <tbody>
            <tr v-for="p in participants" :key="p.participantId">
              <td>#{{ p.memberId }}</td><td>{{ p.followerSnapshot }}</td><td>{{ lbl(p.status) }}</td>
              <td class="acts">
                <template v-if="p.status === 'PENDING'">
                  <button @click="approveParticipant(p.participantId)">승인</button>
                  <button class="danger" @click="rejectParticipant(p.participantId)">거절</button>
                </template>
              </td>
            </tr>
            <tr v-if="!participants.length"><td colspan="4" class="empty">참가 신청자가 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 콜라보 -->
    <section v-else-if="tab === 'collab'">
      <!-- 콜라보 게임 -->
      <h4>콜라보 게임 <button class="btn orange xs" @click="newGame">+ 추가</button></h4>
      <table class="grid">
        <thead><tr><th>게임명</th><th>게임링크</th><th>후기링크</th><th>정렬</th><th></th></tr></thead>
        <tbody>
          <tr v-for="g in games" :key="g.id">
            <td>{{ g.name }}</td><td class="url">{{ g.gameLinkUrl }}</td><td class="url">{{ g.reviewLinkUrl }}</td>
            <td>{{ g.sortOrder }}</td>
            <td class="acts"><button @click="editGame(g)">수정</button><button class="danger" @click="delGame(g.id)">삭제</button></td>
          </tr>
          <tr v-if="!games.length"><td colspan="5" class="empty">없음</td></tr>
        </tbody>
      </table>
      <div v-if="gameEditing" class="form-card">
        <h4>{{ gameEditing.id ? '게임 수정' : '새 게임' }}</h4>
        <label>게임명<input v-model="gameEditing.name" /></label>
        <label>설명<textarea v-model="gameEditing.description"></textarea></label>
        <label>썸네일 이미지
          <div class="logo-upload">
            <img v-if="gameEditing.thumbnailUrl" :src="gameEditing.thumbnailUrl" class="logo-preview" alt="" />
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="pickImage($event, gameEditing, 'thumbnailUrl')" />
            <span v-if="imgUploading">업로드 중…</span>
          </div>
        </label>
        <label>게임 링크 URL<input v-model="gameEditing.gameLinkUrl" placeholder="https://" /></label>
        <label>후기 링크 URL (외부 블로그 등 — 선택)<input v-model="gameEditing.reviewLinkUrl" placeholder="https://" /></label>
        <label>후기 게시판 연결 (사이트 내 캠페인 — 선택)
          <select v-model="gameEditing.campaignId">
            <option :value="null">연결 안 함</option>
            <option v-for="c in campaigns" :key="c.id" :value="c.id">{{ c.title }}</option>
          </select>
        </label>
        <label>정렬순서<input type="number" v-model.number="gameEditing.sortOrder" /></label>
        <div class="form-acts">
          <button class="btn sm" @click="saveGame">저장</button>
          <button class="btn ghost sm" @click="gameEditing = null">취소</button>
        </div>
      </div>

      <!-- 콘텐츠 영상 -->
      <h4 style="margin-top:28px">콘텐츠 영상 <button class="btn orange xs" @click="newVideo">+ 추가</button></h4>
      <table class="grid">
        <thead><tr><th>제목</th><th>영상 URL</th><th>대표</th><th>정렬</th><th></th></tr></thead>
        <tbody>
          <tr v-for="v in videos" :key="v.id">
            <td>{{ v.title }}</td><td class="url">{{ v.videoUrl }}</td><td>{{ v.featured ? '★' : '' }}</td>
            <td>{{ v.sortOrder }}</td>
            <td class="acts"><button @click="editVideo(v)">수정</button><button class="danger" @click="delVideo(v.id)">삭제</button></td>
          </tr>
          <tr v-if="!videos.length"><td colspan="5" class="empty">없음</td></tr>
        </tbody>
      </table>
      <div v-if="videoEditing" class="form-card">
        <h4>{{ videoEditing.id ? '영상 수정' : '새 영상' }}</h4>
        <label>제목<input v-model="videoEditing.title" /></label>
        <label>영상 URL<input v-model="videoEditing.videoUrl" placeholder="https://youtube.com/watch?v=..." /></label>
        <label>썸네일 이미지 (선택)
          <div class="logo-upload">
            <img v-if="videoEditing.thumbnailUrl" :src="videoEditing.thumbnailUrl" class="logo-preview" alt="" />
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="pickImage($event, videoEditing, 'thumbnailUrl')" />
            <span v-if="imgUploading">업로드 중…</span>
          </div>
        </label>
        <div class="row3">
          <label class="chk"><input type="checkbox" v-model="videoEditing.featured" /> 히어로 대표</label>
          <label>정렬순서<input type="number" v-model.number="videoEditing.sortOrder" /></label>
        </div>
        <div class="form-acts">
          <button class="btn sm" @click="saveVideo">저장</button>
          <button class="btn ghost sm" @click="videoEditing = null">취소</button>
        </div>
      </div>

      <!-- 클라이언트 로고 -->
      <h4 style="margin-top:28px">클라이언트 로고 <button class="btn orange xs" @click="newLogo">+ 추가</button></h4>
      <table class="grid">
        <thead><tr><th>이름</th><th>로고 URL</th><th>링크</th><th>정렬</th><th></th></tr></thead>
        <tbody>
          <tr v-for="cl in clients" :key="cl.id">
            <td>{{ cl.name ?? '-' }}</td><td class="url">{{ cl.logoUrl }}</td><td class="url">{{ cl.linkUrl }}</td>
            <td>{{ cl.sortOrder }}</td>
            <td class="acts"><button @click="editLogo(cl)">수정</button><button class="danger" @click="delLogo(cl.id)">삭제</button></td>
          </tr>
          <tr v-if="!clients.length"><td colspan="5" class="empty">없음</td></tr>
        </tbody>
      </table>
      <div v-if="logoEditing" class="form-card">
        <h4>{{ logoEditing.id ? '로고 수정' : '새 로고' }}</h4>
        <label>이름<input v-model="logoEditing.name" /></label>
        <label>로고 이미지
          <div class="logo-upload">
            <img v-if="logoEditing.logoUrl" :src="logoEditing.logoUrl" class="logo-preview" alt="" />
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="pickImage($event, logoEditing, 'logoUrl')" />
            <span v-if="imgUploading">업로드 중…</span>
          </div>
        </label>
        <label>연결 링크 URL<input v-model="logoEditing.linkUrl" placeholder="https://" /></label>
        <label>정렬순서<input type="number" v-model.number="logoEditing.sortOrder" /></label>
        <div class="form-acts">
          <button class="btn sm" @click="saveLogo">저장</button>
          <button class="btn ghost sm" @click="logoEditing = null">취소</button>
        </div>
      </div>
    </section>

    <!-- 굿즈/주문 -->
    <section v-else-if="tab === 'goods'">
      <button class="btn orange sm" @click="newGoods">+ 새 굿즈</button>
      <table class="grid">
        <thead><tr><th>상품명</th><th>가격</th><th>재고</th><th>상태</th><th>정렬</th><th></th></tr></thead>
        <tbody>
          <tr v-for="g in goodsList" :key="g.id">
            <td>{{ g.name }}</td><td>{{ won(g.price) }}</td><td>{{ g.stock }}</td>
            <td>{{ g.status }}</td><td>{{ g.sortOrder }}</td>
            <td class="acts">
              <button @click="editGoods(g)">수정</button>
              <button class="danger" @click="removeGoods(g)">삭제</button>
            </td>
          </tr>
          <tr v-if="!goodsList.length"><td colspan="6" class="empty">등록된 굿즈가 없습니다.</td></tr>
        </tbody>
      </table>

      <div v-if="goodsEditing" class="form-card">
        <h4>{{ goodsEditing.id ? '굿즈 수정' : '새 굿즈' }}</h4>
        <label>상품명<input v-model="goodsEditing.name" /></label>
        <label>설명<textarea v-model="goodsEditing.description"></textarea></label>
        <label>상품 이미지
          <div class="logo-upload">
            <img v-if="goodsEditing.imageUrl" :src="goodsEditing.imageUrl" class="logo-preview" alt="" />
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="pickImage($event, goodsEditing, 'imageUrl')" />
            <span v-if="imgUploading">업로드 중…</span>
          </div>
        </label>
        <div class="row3">
          <label>가격(원)<input type="number" v-model.number="goodsEditing.price" /></label>
          <label>재고<input type="number" v-model.number="goodsEditing.stock" /></label>
          <label>상태
            <select v-model="goodsEditing.status"><option>ACTIVE</option><option>HIDDEN</option></select>
          </label>
        </div>
        <label>정렬순서<input type="number" v-model.number="goodsEditing.sortOrder" /></label>
        <div class="form-acts">
          <button class="btn sm" @click="saveGoods">저장</button>
          <button class="btn ghost sm" @click="goodsEditing = null">취소</button>
        </div>
      </div>

      <div class="manage">
        <h4>주문 내역</h4>
        <table class="grid">
          <thead><tr><th>주문#</th><th>상품</th><th>수량</th><th>금액</th><th>상태</th><th>받는분</th><th>연락처</th><th>주소</th><th>결제일</th></tr></thead>
          <tbody>
            <tr v-for="o in orders" :key="o.id">
              <td>#{{ o.id }}</td><td>{{ o.goodsName }}</td><td>{{ o.quantity }}</td>
              <td>{{ won(o.totalAmount) }}</td><td>{{ o.status }}</td>
              <td>{{ o.receiverName }}</td><td>{{ o.receiverPhone }}</td>
              <td>{{ o.address }} {{ o.addressDetail }}</td>
              <td>{{ o.paidAt?.slice(0, 16).replace('T', ' ') ?? '-' }}</td>
            </tr>
            <tr v-if="!orders.length"><td colspan="9" class="empty">주문이 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 설정/권한 -->
    <!-- 회원 -->
    <section v-else-if="tab === 'members'">
      <table class="grid">
        <thead><tr><th>ID</th><th>닉네임</th><th>채널ID</th><th>팔로워</th><th>등급</th><th>가입일</th><th></th></tr></thead>
        <tbody>
          <tr v-for="m in members" :key="m.id">
            <td>{{ m.id }}</td>
            <td>
              <img v-if="m.profileImageUrl" :src="m.profileImageUrl" class="avatar" alt="" />
              {{ m.nickname }}
            </td>
            <td class="mono">{{ m.chzzkChannelId.slice(0, 12) }}…</td>
            <td>{{ m.followerCount ?? '-' }}</td>
            <td>
              <select :value="m.role" @change="changeMemberRole(m, ($event.target as HTMLSelectElement).value)">
                <option>VIEWER</option><option>STREAMER</option><option>ADMIN</option>
              </select>
              <span v-if="m.roleOverridden" class="badge">수동</span>
            </td>
            <td>{{ m.createdAt?.slice(0, 10) }}</td>
            <td class="acts">
              <button v-if="m.roleOverridden" @click="resetMemberRole(m)">자동 복귀</button>
            </td>
          </tr>
          <tr v-if="!members.length"><td colspan="7" class="empty">회원이 없습니다.</td></tr>
        </tbody>
      </table>
      <p class="hint">등급을 바꾸면 수동 고정(자동 재산정 제외)됩니다. "자동 복귀"를 누르면 다음 로그인부터 팔로워 기준으로 다시 계산돼요.</p>
    </section>

    <section v-else-if="tab === 'settings'">
      <h4>설정값</h4>
      <table class="grid">
        <thead><tr><th>키</th><th>값</th><th>설명</th><th></th></tr></thead>
        <tbody>
          <tr v-for="s in settings" :key="s.settingKey">
            <td>{{ s.settingKey }}</td>
            <td><input v-model="s.settingValue" /></td>
            <td>{{ s.description }}</td>
            <td><button @click="saveSetting(s.settingKey, s.settingValue)">저장</button></td>
          </tr>
          <tr v-if="!settings.length"><td colspan="4" class="empty">설정값이 없습니다.</td></tr>
        </tbody>
      </table>

      <h4 style="margin-top:28px">권한 수동 오버라이드</h4>
      <div class="override">
        <input type="number" v-model.number="overrideMemberId" placeholder="회원 ID" />
        <select v-model="overrideRole"><option>VIEWER</option><option>STREAMER</option><option>ADMIN</option></select>
        <button class="btn sm" @click="doOverride">적용</button>
      </div>
    </section>

    <!-- 로그 -->
    <section v-else-if="tab === 'logs'">
      <table class="grid">
        <thead><tr><th>시각</th><th>행위</th><th>대상</th><th>상세</th></tr></thead>
        <tbody>
          <tr v-for="(l, i) in logs?.content ?? []" :key="i">
            <td>{{ (l.createdAt as string)?.slice(0, 19).replace('T', ' ') }}</td>
            <td>{{ l.action }}</td>
            <td>{{ l.targetType }}#{{ l.targetId }}</td>
            <td>{{ l.detail }}</td>
          </tr>
          <tr v-if="!(logs?.content?.length)"><td colspan="4" class="empty">로그가 없습니다.</td></tr>
        </tbody>
      </table>
    </section>
  </div>
</template>

<style scoped>
.tabs { display: flex; gap: 8px; margin-bottom: 20px; border-bottom: 1px solid #eee; }
.tabs button { background: none; border: 0; padding: 10px 16px; font-weight: 700; color: var(--text-muted); border-bottom: 2px solid transparent; }
.tabs button.on { color: var(--text-strong); border-bottom-color: var(--accent-orange); }
.grid { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 14px; }
.grid th, .grid td { border-bottom: 1px solid #eee; padding: 8px 10px; text-align: left; }
.grid tr.sel { background: #fff7ec; }
.grid .empty, .empty { color: var(--text-muted); text-align: center; }
.acts button { margin-right: 6px; border: 1px solid #ddd; background: #fff; border-radius: 6px; padding: 4px 8px; }
.acts button.danger, .danger { color: var(--label-red); border-color: #f3c2c2; }
.xs { font-size: 12px; padding: 2px 8px; margin-left: 6px; }
.avatar { width: 24px; height: 24px; border-radius: 50%; vertical-align: middle; margin-right: 6px; object-fit: cover; }
.mono { font-family: monospace; font-size: 12px; color: var(--text-muted); }
.badge { margin-left: 6px; font-size: 11px; font-weight: 800; color: var(--accent-orange); border: 1px solid var(--accent-orange); border-radius: 999px; padding: 1px 7px; }
.hint { margin-top: 12px; font-size: 13px; color: var(--text-muted); }
.logo-upload { display: flex; align-items: center; gap: 10px; margin-top: 4px; }
.logo-preview { width: 48px; height: 48px; object-fit: contain; border: 1px solid #eee; border-radius: 6px; background: #fafafa; }
.form-card, .manage { margin-top: 24px; border: 1px solid #eee; border-radius: var(--radius); padding: 18px; }
.form-card label { display: block; margin-bottom: 10px; font-size: 13px; font-weight: 600; }
.form-card input, .form-card textarea, .form-card select { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; margin-top: 4px; }
.form-card .row3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.form-card .chk { display: flex; align-items: center; gap: 6px; }
.form-acts { display: flex; gap: 8px; margin-top: 12px; }
.keys textarea, .manage textarea { width: 100%; min-height: 90px; padding: 8px; border: 1px solid #ddd; border-radius: 6px; margin-bottom: 8px; }
.result { color: var(--accent-orange); font-weight: 700; }
.collab-admin { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.collab-admin ul { list-style: none; padding: 0; }
.collab-admin li { padding: 6px 0; border-bottom: 1px solid #f0f0f0; display: flex; justify-content: space-between; align-items: center; }
.override { display: flex; gap: 10px; align-items: center; }
.override input, .override select { padding: 8px; border: 1px solid #ddd; border-radius: 6px; }
</style>
