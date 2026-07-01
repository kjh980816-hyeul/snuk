<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { adminApi } from '@/api/admin'
import { campaignApi, collabApi } from '@/api'
import type { Campaign, CollabGame, ContentVideo, ClientLogo, Goods, OrderView } from '@/api/types'

type Tab = 'campaigns' | 'collab' | 'goods' | 'settings' | 'logs'
const tab = ref<Tab>('campaigns')

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
  gameEditing.value = { name: '', description: '', thumbnailUrl: '', gameLinkUrl: '', reviewLinkUrl: '', sortOrder: games.value.length }
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
  if (t === 'collab') loadCollab()
  if (t === 'goods') loadGoods()
  if (t === 'settings') loadSettings()
  if (t === 'logs') loadLogs()
}
</script>

<template>
  <div class="wrap admin section">
    <h2 class="section-label">관리자</h2>
    <nav class="tabs">
      <button :class="{ on: tab === 'campaigns' }" @click="onTab('campaigns')">캠페인</button>
      <button :class="{ on: tab === 'collab' }" @click="onTab('collab')">콜라보/노출</button>
      <button :class="{ on: tab === 'goods' }" @click="onTab('goods')">굿즈/주문</button>
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
            <td>{{ c.title }}</td><td>{{ c.status }}</td><td>{{ c.distributionType }}</td>
            <td>{{ c.keyMode }}</td><td>{{ c.filledSlots }}/{{ c.totalSlots }}</td>
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
        <label>홍보 이미지 URL<input v-model="editing.promoImageUrl" placeholder="https://" /></label>
        <div class="row3">
          <label>상태
            <select v-model="editing.status">
              <option>SCHEDULED</option><option>OPEN</option><option>CLOSED</option>
            </select>
          </label>
          <label>배포방식
            <select v-model="editing.distributionType"><option>FCFS</option><option>APPROVAL</option></select>
          </label>
          <label>키모드
            <select v-model="editing.keyMode"><option>QUANTITY</option><option>UNIQUE_KEY</option></select>
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
                <td>{{ k.maskedKey }}</td><td>{{ k.status }}</td><td>{{ k.assignedMemberId ?? '-' }}</td>
                <td class="acts">
                  <button v-if="k.status === 'AVAILABLE'" class="danger" @click="delKey(k.id)">삭제</button>
                  <button v-if="k.status === 'ASSIGNED'" @click="revokeKey(k.id)">무효화</button>
                </td>
              </tr>
              <tr v-if="!keys.length"><td colspan="4" class="empty">등록된 키가 없습니다.</td></tr>
            </tbody>
          </table>
        </div>

        <div class="apps">
          <h5>신청자</h5>
          <table class="grid">
            <thead><tr><th>회원</th><th>팔로워(스냅샷)</th><th>상태</th><th></th></tr></thead>
            <tbody>
              <tr v-for="a in applications" :key="a.applicationId">
                <td>#{{ a.memberId }}</td><td>{{ a.followerSnapshot }}</td><td>{{ a.status }}</td>
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
        <label>썸네일 URL<input v-model="gameEditing.thumbnailUrl" placeholder="https://" /></label>
        <label>게임 링크 URL<input v-model="gameEditing.gameLinkUrl" placeholder="https://" /></label>
        <label>후기 링크 URL<input v-model="gameEditing.reviewLinkUrl" placeholder="https://" /></label>
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
        <label>썸네일 URL<input v-model="videoEditing.thumbnailUrl" placeholder="https://" /></label>
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
        <label>로고 이미지 URL<input v-model="logoEditing.logoUrl" placeholder="https://" /></label>
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
        <label>이미지 URL<input v-model="goodsEditing.imageUrl" placeholder="https://" /></label>
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
