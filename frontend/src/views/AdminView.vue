<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adminApi } from '@/api/admin'
import { campaignApi, collabApi, noticeApi, resourceApi, tournamentApi } from '@/api'
import type { Campaign, CollabGame, ContentVideo, ClientLogo, Goods, Notice, OrderView, Spotlight, Tournament } from '@/api/types'

// 탭 구성은 메인 사이트 사이드바 순서/명칭 기준 (컨텐츠·대회 통합 탭 + 게임체험단·영상·굿즈샵·협력사)
type Tab = 'campaigns' | 'games' | 'videos' | 'clients' | 'goods' | 'notices' | 'warnings' | 'reports' | 'resources' | 'members' | 'settings' | 'logs'
const tab = ref<Tab>('campaigns')

// 테마 — 메인 사이트와 동일 키(snuk-theme) 공유
const theme = ref<'light' | 'dark'>(
  (localStorage.getItem('snuk-theme') as 'light' | 'dark') || 'dark',
)
function toggleTheme() {
  theme.value = theme.value === 'light' ? 'dark' : 'light'
  localStorage.setItem('snuk-theme', theme.value)
}

// 화면 표기용 한글 라벨 (저장값은 영문 enum 그대로)
const ko: Record<string, string> = {
  SCHEDULED: '예정', OPEN: '모집중', CLOSED: '마감', DONE: '종료',
  FCFS: '선착순', APPROVAL: '승인제',
  QUANTITY: '수량만(키 없음)', UNIQUE_KEY: '고유 키 배포',
  AVAILABLE: '미배정', ASSIGNED: '배정됨', REVOKED: '무효',
  PENDING: '대기', APPROVED: '승인', REJECTED: '거절',
  ACTIVE: '판매중', HIDDEN: '숨김', PAID: '결제완료', CANCELLED: '취소', FAILED: '실패',
}
const lbl = (v: string | undefined | null) => (v ? (ko[v] ?? v) : '-')

// ----- campaigns -----
const campaigns = ref<Campaign[]>([])
const editing = ref<Partial<Campaign> | null>(null)
const selected = ref<Campaign | null>(null)
const keys = ref<Array<{ id: number; maskedKey: string; status: string; assignedMemberId: number | null }>>([])
const rawKeys = ref('')
const keyResult = ref<string | null>(null)
const applications = ref<Array<{ applicationId: number; memberId: number; nickname: string; profileImageUrl: string | null; status: string; followerSnapshot: number }>>([])

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
  if (!confirm(`'${c.title}' 컨텐츠를 삭제할까요?`)) return
  await adminApi.deleteCampaign(c.id)
  if (selected.value?.id === c.id) selected.value = null
  await loadCampaigns()
}
async function selectCampaign(c: Campaign) {
  selected.value = c
  applications.value = await adminApi.applications(c.id)
}
// 키/신청자 유틸 — 컨텐츠 패널·게임체험단 패널 공용(campaignId 명시)
async function submitKeys(campaignId: number) {
  const res = await adminApi.registerKeys(campaignId, rawKeys.value)
  keyResult.value = `등록 ${res.registered} · 중복 ${res.duplicated} · 빈줄 ${res.blank} (가용 ${res.totalAvailable})`
  rawKeys.value = ''
  keys.value = await adminApi.listKeys(campaignId)
}
async function delKey(campaignId: number, keyId: number) {
  await adminApi.deleteKey(campaignId, keyId)
  keys.value = await adminApi.listKeys(campaignId)
}
async function revokeKey(campaignId: number, keyId: number) {
  await adminApi.revokeKey(campaignId, keyId)
  keys.value = await adminApi.listKeys(campaignId)
}
async function approveApp(id: number, campaignId: number) {
  await adminApi.approve(id)
  applications.value = await adminApi.applications(campaignId)
  await loadCampaigns()
  if (gmCampaign.value?.id === campaignId) keys.value = await adminApi.listKeys(campaignId)
}
async function rejectApp(id: number, campaignId: number) {
  await adminApi.reject(id)
  applications.value = await adminApi.applications(campaignId)
}

// ----- tournaments -----
const tournaments = ref<Tournament[]>([])
const tourEditing = ref<Partial<Tournament> | null>(null)
const tourSelected = ref<Tournament | null>(null)
const participants = ref<Array<{ participantId: number; memberId: number; nickname: string; profileImageUrl: string | null; status: string; followerSnapshot: number; answers: Array<{ text: string | null; imageUrl: string | null }> }>>([])

// 대회 참가 질문(항목 17) — 줄바꿈 구분, 줄 앞에 [선택] 붙이면 선택 항목(기본 필수)
const tourQuestionsText = computed({
  get: () => (tourEditing.value?.applyQuestions ?? [])
    .map((q) => (q.required ? q.q : `[선택] ${q.q}`)).join('\n'),
  set: (v: string) => {
    if (tourEditing.value) {
      tourEditing.value.applyQuestions = v.split('\n')
        .map((s) => s.trim()).filter(Boolean)
        .map((s) => s.startsWith('[선택]')
          ? { q: s.slice(4).trim(), required: false }
          : { q: s, required: true })
        .filter((q) => q.q)
    }
  },
})

// 참가 신청 CSV 다운로드(항목 18)
function exportParticipantsCsv(tournamentId: number) {
  tournamentApi.exportParticipants(tournamentId).catch(() => alert('다운로드에 실패했습니다.'))
}

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

// ----- 컨텐츠·대회 통합 목록 (게임체험단 연계 캠페인은 제외 — 게임체험단 탭에서 관리) -----
interface UnifiedRow {
  type: 'campaign' | 'tournament'
  id: number
  img: string | null
  title: string
  status: string
  slots: string
  featured: boolean
  campaign?: Campaign
  tournament?: Tournament
}
const gameLinkedIds = computed(() => new Set(games.value.map((g) => g.campaignId).filter(Boolean)))
const unifiedRows = computed<UnifiedRow[]>(() => [
  ...campaigns.value
    .filter((c) => !gameLinkedIds.value.has(c.id))
    .map((c): UnifiedRow => ({
      type: 'campaign', id: c.id, img: c.promoImageUrl, title: c.title, status: c.status,
      slots: `${c.filledSlots}/${c.totalSlots}`, featured: c.featured, campaign: c,
    })),
  ...tournaments.value.map((t): UnifiedRow => ({
    type: 'tournament', id: t.id, img: t.bannerImageUrl, title: t.title, status: t.status,
    slots: `${t.filledSlots}/${t.capacity}`, featured: t.featured, tournament: t,
  })),
])
function manageRow(r: UnifiedRow) {
  if (r.type === 'campaign' && r.campaign) { tourSelected.value = null; selectCampaign(r.campaign) }
  else if (r.tournament) { selected.value = null; selectTournament(r.tournament) }
}
function editRow(r: UnifiedRow) {
  if (r.type === 'campaign' && r.campaign) { tourEditing.value = null; editCampaign(r.campaign) }
  else if (r.tournament) { editing.value = null; editTournament(r.tournament) }
}
function removeRow(r: UnifiedRow) {
  if (r.type === 'campaign' && r.campaign) removeCampaign(r.campaign)
  else if (r.tournament) removeTournament(r.tournament)
}

// ----- 게임체험단 모집·키 관리 (키 시스템은 여기 — 스눅↔게임사 협약 키 배포) -----
const gameManage = ref<CollabGame | null>(null)
const gmCampaign = ref<Campaign | null>(null)
async function openGameManage(g: CollabGame) {
  gameManage.value = g
  keyResult.value = null
  rawKeys.value = ''
  if (!g.campaignId) { gmCampaign.value = null; keys.value = []; applications.value = []; return }
  gmCampaign.value = campaigns.value.find((c) => c.id === g.campaignId)
    ?? await campaignApi.detail(g.campaignId)
  keys.value = await adminApi.listKeys(g.campaignId)
  applications.value = await adminApi.applications(g.campaignId)
}
/** 연결 캠페인이 없는 게임 — 모집·키 시스템(비노출용 캠페인) 자동 생성 후 연결 */
async function createTrialSystem(g: CollabGame) {
  const created = await adminApi.createCampaign({
    title: `[체험단] ${g.name}`, description: g.description ?? '', gameName: g.name,
    promoImageUrl: g.thumbnailUrl, status: 'OPEN', distributionType: 'APPROVAL',
    keyMode: 'UNIQUE_KEY', totalSlots: 0, featured: false,
  })
  await adminApi.updateGame(g.id, { ...g, campaignId: created.id })
  await Promise.all([loadCollab(), loadCampaigns()])
  const fresh = games.value.find((x) => x.id === g.id)
  if (fresh) await openGameManage(fresh)
}
async function saveTrialRecruit() {
  if (!gmCampaign.value) return
  await adminApi.updateCampaign(gmCampaign.value.id, {
    status: gmCampaign.value.status,
    totalSlots: gmCampaign.value.totalSlots,
    distributionType: gmCampaign.value.distributionType,
  })
  await loadCampaigns()
  alert('모집 설정이 저장되었습니다.')
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
  provider: string
  channelId: string
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

// 사이트 노출 설정(V10 시드) — 전용 UI 로 관리, 일반 설정 테이블에서는 제외
const SITE_IMAGES = [
  { key: 'HERO_IMAGE_URL', label: '홈 히어로 배경' },
  { key: 'BANNER_CONTENTS_URL', label: '컨텐츠 배너' },
  { key: 'BANNER_CHAMPIONSHIP_URL', label: '대회 배너' },
  { key: 'BANNER_GAMES_URL', label: '게임체험단 배너' },
  { key: 'BANNER_VIDEOS_URL', label: '영상 배너' },
  { key: 'BANNER_STREAMERS_URL', label: '스트리머 배너' },
  { key: 'BANNER_LIVE_URL', label: '생방송 배너' },
  { key: 'BANNER_GOODS_URL', label: '굿즈샵 배너' },
  { key: 'BANNER_PARTNERS_URL', label: '협력사 배너' },
]
// 배너 문구(제목/부제) — V12 시드, '-'=기본 문구 사용
const BANNER_TEXTS = [
  { page: 'CONTENTS', label: '컨텐츠' },
  { page: 'CHAMPIONSHIP', label: '대회' },
  { page: 'GAMES', label: '게임체험단' },
  { page: 'VIDEOS', label: '영상' },
  { page: 'STREAMERS', label: '스트리머' },
  { page: 'LIVE', label: '생방송' },
  { page: 'GOODS', label: '굿즈샵' },
  { page: 'PARTNERS', label: '협력사' },
]
const bannerTextInputs = ref<Record<string, string>>({})
function loadBannerTextInputs() {
  const next: Record<string, string> = {}
  for (const b of BANNER_TEXTS) {
    next[`BANNER_${b.page}_TITLE`] = settingValue(`BANNER_${b.page}_TITLE`)
    next[`BANNER_${b.page}_SUB`] = settingValue(`BANNER_${b.page}_SUB`)
  }
  bannerTextInputs.value = next
}
async function saveBannerText(page: string) {
  // 빈값 저장 = 문구 숨김(항목 4). 기본 문구로 돌리려면 '-' 입력.
  // 값을 먼저 스냅샷 — saveSetting 안의 loadSettings 가 입력값을 서버 값으로 되돌리기 때문.
  const title = bannerTextInputs.value[`BANNER_${page}_TITLE`]?.trim() ?? ''
  const sub = bannerTextInputs.value[`BANNER_${page}_SUB`]?.trim() ?? ''
  await adminApi.updateSetting(`BANNER_${page}_TITLE`, title)
  await adminApi.updateSetting(`BANNER_${page}_SUB`, sub)
  await loadSettings()
  alert('저장되었습니다.')
}
// ----- 메인 라이브 배너(히어로 아래, 항목 13/18) -----
const liveBannerEnabled = ref(false)
const liveBannerUrl = ref('')
const liveBannerTitle = ref('')
async function saveLiveBanner() {
  // 값을 먼저 스냅샷 — saveSetting 안의 loadSettings 가 입력값을 서버 값으로 되돌리기 때문.
  const enabled = liveBannerEnabled.value ? '1' : '0'
  const url = liveBannerUrl.value.trim() || '-'
  const title = liveBannerTitle.value.trim() || '-'
  await adminApi.updateSetting('LIVE_BANNER_ENABLED', enabled)
  await adminApi.updateSetting('LIVE_BANNER_URL', url)
  await adminApi.updateSetting('LIVE_BANNER_TITLE', title)
  await loadSettings()
  alert('저장되었습니다. 메인 히어로 아래에 반영돼요.')
}
const SITE_KEYS = [
  'LIVE_CHANNEL_ID', 'LIVE_BANNER_ENABLED', 'LIVE_BANNER_URL', 'LIVE_BANNER_TITLE',
  ...SITE_IMAGES.map((s) => s.key),
  ...BANNER_TEXTS.flatMap((b) => [`BANNER_${b.page}_TITLE`, `BANNER_${b.page}_SUB`]),
]
const generalSettings = computed(() => settings.value.filter(
  (s) => !SITE_KEYS.includes(s.settingKey) && !s.settingKey.startsWith('MENU_')))
const liveChannelInput = ref('')

// 사이드바 메뉴 표시/숨김(항목 8) — MENU_{KEY}='0' 이면 숨김
const MENU_ITEMS = [
  { key: 'HOME', label: 'HOME' },
  { key: 'CAMPAIGNS', label: '컨텐츠·대회' },
  { key: 'GAMES', label: '게임체험단' },
  { key: 'STREAMERS', label: '스트리머' },
  { key: 'NEWS', label: '스눅 뉴스' },
  { key: 'VIDEOS', label: '영상' },
  { key: 'GOODS', label: '굿즈샵' },
  { key: 'RESOURCES', label: '무료소스' },
  { key: 'CLIENTS', label: '협력사' },
]
const menuInputs = ref<Record<string, boolean>>({})
function loadMenuInputs() {
  const next: Record<string, boolean> = {}
  for (const m of MENU_ITEMS) {
    next[m.key] = settingValue(`MENU_${m.key}`) !== '0'
  }
  menuInputs.value = next
}
async function saveMenus() {
  for (const m of MENU_ITEMS) {
    await adminApi.updateSetting(`MENU_${m.key}`, menuInputs.value[m.key] ? '1' : '0')
  }
  await loadSettings()
  alert('저장되었습니다. 사이트 새로고침 시 반영돼요.')
}

// 히어로/배너 이미지 제거·기본 복원(항목 16) — 'none'=이미지 없음, '-'=기본 이미지
async function setSettingImage(key: string, value: 'none' | '-') {
  await adminApi.updateSetting(key, value)
  await loadSettings()
}

function settingValue(key: string): string {
  const v = settings.value.find((s) => s.settingKey === key)?.settingValue ?? ''
  return v === '-' ? '' : v
}
async function loadSettings() {
  settings.value = await adminApi.settings()
  liveChannelInput.value = settingValue('LIVE_CHANNEL_ID')
  liveBannerEnabled.value = settingValue('LIVE_BANNER_ENABLED') === '1'
  liveBannerUrl.value = settingValue('LIVE_BANNER_URL')
  liveBannerTitle.value = settingValue('LIVE_BANNER_TITLE')
  loadBannerTextInputs()
  loadMenuInputs()
}
async function saveLiveChannel() {
  // setting_value 는 빈값 불가 — 미설정은 '-' 로 저장(프론트가 기본값 처리)
  await saveSetting('LIVE_CHANNEL_ID', liveChannelInput.value.trim() || '-')
  alert('저장되었습니다.')
}
async function pickSettingImage(e: Event, key: string) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { alert('이미지는 5MB 이하만 업로드할 수 있어요.'); input.value = ''; return }
  imgUploading.value = true
  try {
    const { url } = await adminApi.uploadImage(file)
    await saveSetting(key, url)
  } catch {
    alert('업로드 실패 — 이미지 파일(jpg/png/gif/webp)인지 확인해주세요.')
  } finally {
    imgUploading.value = false
    input.value = ''
  }
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

// ----- notices / spotlights -----
const notices = ref<Notice[]>([])
const noticeEditing = ref<{ id?: number; title: string; content: string; pinned: boolean } | null>(null)
const spotlights = ref<Spotlight[]>([])

async function loadNotices() {
  notices.value = await noticeApi.list(20)
  spotlights.value = await adminApi.spotlights()
}
function newNotice() {
  noticeEditing.value = { title: '', content: '', pinned: false }
}
function editNotice(n: Notice) {
  noticeEditing.value = { id: n.id, title: n.title, content: n.content ?? '', pinned: n.pinned }
}
async function saveNotice() {
  if (!noticeEditing.value || !noticeEditing.value.title.trim()) return
  const b = noticeEditing.value
  if (b.id) await adminApi.updateNotice(b.id, { title: b.title, content: b.content, pinned: b.pinned })
  else await adminApi.createNotice({ title: b.title, content: b.content, pinned: b.pinned })
  noticeEditing.value = null
  await loadNotices()
}
async function removeNotice(n: Notice) {
  if (!confirm(`'${n.title}' 공지를 삭제할까요?`)) return
  await adminApi.deleteNotice(n.id)
  await loadNotices()
}
async function removeSpotlight(s: Spotlight) {
  if (!confirm(`'${s.title}' 스포트라이트를 내릴까요?`)) return
  await adminApi.deleteSpotlight(s.id)
  await loadNotices()
}
async function approveSpotlight(s: Spotlight) {
  await adminApi.approveSpotlight(s.id)
  await loadNotices()
}
function spotState(s: Spotlight) {
  if (!s.approved) return '승인대기'
  if (s.scheduledAt && new Date(s.scheduledAt).getTime() > Date.now()) return '노출예약'
  return new Date(s.expiresAt).getTime() > Date.now() ? '노출중' : '만료'
}

// ----- 후기 미작성 경고 로그 -----
interface ReviewWarning {
  applicationId: number; memberId: number; nickname: string
  campaignId: number; campaignTitle: string
  reviewDeadline: string; warnedAt: string; deadlineExtended: boolean; reviewWritten: boolean
}
const reviewWarnings = ref<ReviewWarning[]>([])
async function loadWarnings() {
  reviewWarnings.value = await adminApi.reviewWarnings()
}

// ----- logs -----
const logs = ref<{ content: Array<Record<string, unknown>> } | null>(null)
async function loadLogs() {
  logs.value = await adminApi.logs()
}

onMounted(() => { loadCampaigns(); loadTournaments(); loadCollab() })

// ----- 게시판 신고함(항목 3) -----
const reports = ref<Array<{ reportId: number; postId: number; reason: string | null; reporterName: string; createdAt: string; postTitle: string; postContent: string; postAuthorName: string; streamerName: string }>>([])
async function loadReports() {
  reports.value = await adminApi.postReports()
}
async function dismissReport(reportId: number) {
  await adminApi.dismissPostReport(reportId)
  await loadReports()
}
async function deleteReportedPost(postId: number) {
  if (!confirm('신고된 글을 삭제할까요? (해당 글의 모든 신고도 함께 정리됩니다)')) return
  await adminApi.deleteReportedPost(postId)
  await loadReports()
}

// ----- 무료소스(항목 19) -----
const resources = ref<Array<{ id: number; title: string; description: string | null; fileUrl: string | null; imageUrl: string | null; createdAt: string }>>([])
const resTitle = ref('')
const resDesc = ref('')
const resLink = ref('')
const resFile = ref<File | null>(null)
const resImage = ref<File | null>(null)
const resUploading = ref(false)
async function loadResources() {
  resources.value = await resourceApi.list()
}
function pickResFile(e: Event) {
  resFile.value = (e.target as HTMLInputElement).files?.[0] ?? null
}
function pickResImage(e: Event) {
  resImage.value = (e.target as HTMLInputElement).files?.[0] ?? null
}
async function submitResource() {
  if (!resTitle.value.trim() || resUploading.value) return
  if (!resLink.value.trim() && !resFile.value) {
    alert('링크(주소) 또는 파일 중 하나는 입력해주세요.')
    return
  }
  resUploading.value = true
  try {
    await adminApi.createResource(resTitle.value.trim(), resDesc.value.trim(), {
      link: resLink.value.trim() || undefined,
      file: resFile.value,
      image: resImage.value,
    })
    resTitle.value = ''
    resDesc.value = ''
    resLink.value = ''
    resFile.value = null
    resImage.value = null
    await loadResources()
    alert('무료소스가 등록됐습니다.')
  } catch (e) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    alert(msg ?? '등록에 실패했습니다.')
  } finally {
    resUploading.value = false
  }
}
async function removeResource(id: number) {
  if (!confirm('이 소스를 삭제할까요? (파일도 함께 삭제됩니다)')) return
  await adminApi.deleteResource(id)
  await loadResources()
}

function onTab(t: Tab) {
  tab.value = t
  if (t === 'campaigns') { loadCampaigns(); loadTournaments(); loadCollab() }
  if (t === 'games' || t === 'videos' || t === 'clients') loadCollab()
  if (t === 'goods') loadGoods()
  if (t === 'notices') loadNotices()
  if (t === 'warnings') loadWarnings()
  if (t === 'reports') loadReports()
  if (t === 'resources') loadResources()
  if (t === 'members') loadMembers()
  if (t === 'settings') loadSettings()
  if (t === 'logs') loadLogs()
}
</script>

<template>
  <div class="admin-page" :data-theme="theme">
  <div class="admin-wrap">
    <header class="admin-head">
      <h2>SNUK <span>관리자</span></h2>
      <div class="head-acts">
        <button class="home-link" @click="toggleTheme">{{ theme === 'light' ? '다크 모드' : '라이트 모드' }}</button>
        <a href="/" class="home-link">← 사이트로</a>
      </div>
    </header>
    <nav class="tabs">
      <button :class="{ on: tab === 'campaigns' }" @click="onTab('campaigns')">컨텐츠·대회</button>
      <button :class="{ on: tab === 'games' }" @click="onTab('games')">게임체험단</button>
      <button :class="{ on: tab === 'videos' }" @click="onTab('videos')">영상</button>
      <button :class="{ on: tab === 'goods' }" @click="onTab('goods')">굿즈샵</button>
      <button :class="{ on: tab === 'clients' }" @click="onTab('clients')">협력사</button>
      <button :class="{ on: tab === 'notices' }" @click="onTab('notices')">공지/스포트라이트</button>
      <button :class="{ on: tab === 'warnings' }" @click="onTab('warnings')">후기 경고</button>
      <button :class="{ on: tab === 'reports' }" @click="onTab('reports')">신고함</button>
      <button :class="{ on: tab === 'resources' }" @click="onTab('resources')">무료소스</button>
      <button :class="{ on: tab === 'members' }" @click="onTab('members')">회원</button>
      <button :class="{ on: tab === 'settings' }" @click="onTab('settings')">설정</button>
      <button :class="{ on: tab === 'logs' }" @click="onTab('logs')">감사로그</button>
    </nav>

    <!-- 컨텐츠·대회 통합 — 한 목록·같은 양식. 스눅 공식 컨텐츠는 여기 "+ 새 컨텐츠"로 등록 -->
    <section v-if="tab === 'campaigns'">
      <div style="display:flex;gap:8px;align-items:center;">
        <button class="btn orange sm" @click="tourEditing = null; newCampaign()">+ 새 컨텐츠 (스눅 공식)</button>
        <button class="btn orange sm" @click="editing = null; newTournament()">+ 새 대회</button>
        <span class="hint" style="margin:0">여기서 등록한 컨텐츠가 메인 큰 카드 후보예요. 게임체험단(키 배포)은 게임체험단 탭에서.</span>
      </div>
      <table class="grid">
        <thead><tr><th>이미지</th><th>구분</th><th>제목</th><th>상태</th><th>모집</th><th>대표</th><th></th></tr></thead>
        <tbody>
          <tr v-for="r in unifiedRows" :key="r.type + r.id"
              :class="{ sel: (r.type === 'campaign' && selected?.id === r.id) || (r.type === 'tournament' && tourSelected?.id === r.id) }">
            <td><img v-if="r.img" :src="r.img" class="thumb" alt="" /><span v-else class="no-img">－</span></td>
            <td>{{ r.type === 'tournament' ? '대회' : '컨텐츠' }}</td>
            <td>{{ r.title }}</td><td>{{ lbl(r.status) }}</td><td>{{ r.slots }}</td>
            <td>{{ r.featured ? '★' : '' }}</td>
            <td class="acts">
              <button @click="manageRow(r)">참가자</button>
              <button @click="editRow(r)">수정</button>
              <button class="danger" @click="removeRow(r)">삭제</button>
            </td>
          </tr>
          <tr v-if="!unifiedRows.length"><td colspan="7" class="empty">등록된 컨텐츠·대회가 없습니다.</td></tr>
        </tbody>
      </table>

      <!-- 편집 폼 -->
      <div v-if="editing" class="form-card">
        <h4>{{ editing.id ? '컨텐츠 수정' : '새 컨텐츠' }}</h4>
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
          <label>진행일<input type="date" v-model="editing.eventDate" /></label>
        </div>
        <div class="row3">
          <label>모집 인원<input type="number" v-model.number="editing.totalSlots" /></label>
          <label class="chk"><input type="checkbox" v-model="editing.featured" /> 메인 큰 카드 고정 (미체크 시 자동 슬라이드)</label>
        </div>
        <div class="form-acts">
          <button class="btn sm" @click="saveCampaign">저장</button>
          <button class="btn ghost sm" @click="editing = null">취소</button>
        </div>
      </div>

      <!-- 컨텐츠 참가자(신청자) — 대회 참가자와 동일 양식 -->
      <div v-if="selected" class="manage">
        <h4>‘{{ selected.title }}’ 참가자</h4>
        <table class="grid">
          <thead><tr><th>회원</th><th>팔로워(스냅샷)</th><th>상태</th><th></th></tr></thead>
          <tbody>
            <tr v-for="a in applications" :key="a.applicationId">
              <td>{{ a.nickname }} <span class="hint" style="margin:0">#{{ a.memberId }}</span></td>
              <td>{{ a.followerSnapshot }}</td><td>{{ lbl(a.status) }}</td>
              <td class="acts">
                <template v-if="a.status === 'PENDING'">
                  <button @click="approveApp(a.applicationId, selected!.id)">승인</button>
                  <button class="danger" @click="rejectApp(a.applicationId, selected!.id)">거절</button>
                </template>
              </td>
            </tr>
            <tr v-if="!applications.length"><td colspan="4" class="empty">신청자가 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 대회 편집/참가자 패널 (통합 목록에서 진입 — 목록은 위 통합 테이블 하나) -->
    <section v-if="tab === 'campaigns'">
      <!-- 편집 폼 (결과입력 포함) -->
      <div v-if="tourEditing" class="form-card">
        <h4>{{ tourEditing.id ? '대회 수정' : '새 대회' }}</h4>
        <label>대회명<input v-model="tourEditing.title" /></label>
        <label>설명<textarea v-model="tourEditing.description"></textarea></label>
        <label>게임명<input v-model="tourEditing.gameName" /></label>
        <label>배너 이미지 (포스터 — 목록/대표 노출)
          <div class="logo-upload">
            <img v-if="tourEditing.bannerImageUrl" :src="tourEditing.bannerImageUrl" class="logo-preview" alt="" />
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="pickImage($event, tourEditing, 'bannerImageUrl')" />
            <span v-if="imgUploading">업로드 중…</span>
          </div>
        </label>
        <label>홍보 상세 이미지 (상세 페이지 본문 — 세로 긴 이미지 가능, 선택)
          <div class="logo-upload">
            <img v-if="tourEditing.detailImageUrl" :src="tourEditing.detailImageUrl" class="logo-preview" alt="" />
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="pickImage($event, tourEditing, 'detailImageUrl')" />
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
          <label>대회일<input type="date" v-model="tourEditing.eventDate" /></label>
        </div>
        <label class="chk"><input type="checkbox" v-model="tourEditing.featured" /> 대표 대회</label>
        <label>대회 결과 (DONE 상태에서 페이지에 노출)
          <textarea v-model="tourEditing.resultText" placeholder="예) 우승: 팀 알파 / MVP: 스트리머A"></textarea>
        </label>
        <label>참가 신청 질문 (한 줄에 하나 · 기본 필수 — 줄 앞에 [선택] 을 붙이면 선택 항목, 답변엔 사진 첨부도 가능)
          <textarea v-model="tourQuestionsText" placeholder="예) 티어를 알려주세요&#10;[선택] 하고 싶은 말"></textarea>
        </label>
        <label>정렬순서<input type="number" v-model.number="tourEditing.sortOrder" /></label>
        <div class="form-acts">
          <button class="btn sm" @click="saveTournament">저장</button>
          <button class="btn ghost sm" @click="tourEditing = null">취소</button>
        </div>
      </div>

      <!-- 선택 대회: 참가 신청자 승인/거절 + 답변 확인 + CSV(항목 14/17/18) -->
      <div v-if="tourSelected" class="manage">
        <h4>‘{{ tourSelected.title }}’ 참가 신청자
          <button class="btn sm" style="margin-left:8px;" @click="exportParticipantsCsv(tourSelected!.id)">📄 엑셀(CSV) 다운로드</button>
        </h4>
        <table class="grid">
          <thead><tr><th>회원</th><th>팔로워(스냅샷)</th><th>상태</th><th>답변</th><th></th></tr></thead>
          <tbody>
            <tr v-for="p in participants" :key="p.participantId">
              <td>{{ p.nickname }} <span class="hint" style="margin:0">#{{ p.memberId }}</span></td>
              <td>{{ p.followerSnapshot }}</td><td>{{ lbl(p.status) }}</td>
              <td style="max-width:340px;">
                <div v-for="(ans, i) in p.answers" :key="i" style="font-size:11.5px;line-height:1.5;">
                  <b>Q{{ i + 1 }}.</b> {{ ans.text || '' }}
                  <a v-if="ans.imageUrl" :href="ans.imageUrl" target="_blank" rel="noopener">
                    <img :src="ans.imageUrl" alt="" style="height:34px;border-radius:5px;vertical-align:middle;margin-left:4px;" />
                  </a>
                </div>
                <span v-if="!p.answers?.length" class="hint" style="margin:0">-</span>
              </td>
              <td class="acts">
                <template v-if="p.status === 'PENDING'">
                  <button @click="approveParticipant(p.participantId)">승인</button>
                  <button class="danger" @click="rejectParticipant(p.participantId)">거절</button>
                </template>
              </td>
            </tr>
            <tr v-if="!participants.length"><td colspan="5" class="empty">참가 신청자가 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 게임체험단 (콜라보 게임) -->
    <section v-else-if="tab === 'games'">
      <h4>게임체험단 게임 <button class="btn orange xs" @click="newGame">+ 추가</button></h4>
      <p class="hint">게임사 협약 키 배포는 여기서 — 게임별 "모집·키" 버튼에서 키 등록·신청자 승인을 관리해요.</p>
      <table class="grid">
        <thead><tr><th>썸네일</th><th>게임명</th><th>모집·키</th><th>게임링크</th><th>정렬</th><th></th></tr></thead>
        <tbody>
          <tr v-for="g in games" :key="g.id" :class="{ sel: gameManage?.id === g.id }">
            <td><img v-if="g.thumbnailUrl" :src="g.thumbnailUrl" class="thumb" alt="" /><span v-else class="no-img">－</span></td>
            <td>{{ g.name }}</td>
            <td>{{ g.campaignId ? '연결됨' : '미설정' }}</td>
            <td class="url">{{ g.gameLinkUrl }}</td>
            <td>{{ g.sortOrder }}</td>
            <td class="acts">
              <button @click="openGameManage(g)">모집·키</button>
              <button @click="editGame(g)">수정</button>
              <button class="danger" @click="delGame(g.id)">삭제</button>
            </td>
          </tr>
          <tr v-if="!games.length"><td colspan="6" class="empty">등록된 게임이 없습니다.</td></tr>
        </tbody>
      </table>

      <!-- 게임 모집·키 관리 (키 시스템은 게임체험단 소속) -->
      <div v-if="gameManage" class="manage">
        <h4>‘{{ gameManage.name }}’ 모집·키 관리</h4>

        <div v-if="!gmCampaign">
          <p class="hint">아직 이 게임의 모집·키 시스템이 없어요. 버튼 한 번이면 만들어집니다 (승인제 + 고유 키 배포).</p>
          <button class="btn orange sm" @click="createTrialSystem(gameManage)">모집·키 시스템 만들기</button>
        </div>

        <template v-else>
          <div class="row3">
            <label>모집 상태
              <select v-model="gmCampaign.status">
                <option value="SCHEDULED">예정</option><option value="OPEN">모집중</option><option value="CLOSED">마감</option>
              </select>
            </label>
            <label>배포방식
              <select v-model="gmCampaign.distributionType">
                <option value="FCFS">선착순 (신청 즉시 키 발급)</option>
                <option value="APPROVAL">승인제 (승인 시 키 발급)</option>
              </select>
            </label>
            <label>모집 인원<input type="number" v-model.number="gmCampaign.totalSlots" /></label>
          </div>
          <button class="btn sm" @click="saveTrialRecruit">모집 설정 저장</button>

          <div class="keys" style="margin-top:18px">
            <h5>게임 키 (붙여넣기 일괄 등록 — 키 수령 후 30일 내 후기 마감 자동 적용)</h5>
            <textarea v-model="rawKeys" placeholder="한 줄에 키 하나씩 붙여넣기"></textarea>
            <button class="btn sm" @click="submitKeys(gmCampaign.id)">등록</button>
            <p v-if="keyResult" class="result">{{ keyResult }}</p>
            <table class="grid">
              <thead><tr><th>키(마스킹)</th><th>상태</th><th>배정대상</th><th></th></tr></thead>
              <tbody>
                <tr v-for="k in keys" :key="k.id">
                  <td>{{ k.maskedKey }}</td><td>{{ lbl(k.status) }}</td><td>{{ k.assignedMemberId ?? '-' }}</td>
                  <td class="acts">
                    <button v-if="k.status === 'AVAILABLE'" class="danger" @click="delKey(gmCampaign!.id, k.id)">삭제</button>
                    <button v-if="k.status === 'ASSIGNED'" @click="revokeKey(gmCampaign!.id, k.id)">무효화</button>
                  </td>
                </tr>
                <tr v-if="!keys.length"><td colspan="4" class="empty">등록된 키가 없습니다.</td></tr>
              </tbody>
            </table>
          </div>

          <div class="apps" style="margin-top:18px">
            <h5>신청자 (승인하면 키 자동 배정)</h5>
            <table class="grid">
              <thead><tr><th>회원</th><th>팔로워(스냅샷)</th><th>상태</th><th></th></tr></thead>
              <tbody>
                <tr v-for="a in applications" :key="a.applicationId">
                  <td>{{ a.nickname }} <span class="hint" style="margin:0">#{{ a.memberId }}</span></td>
                  <td>{{ a.followerSnapshot }}</td><td>{{ lbl(a.status) }}</td>
                  <td class="acts">
                    <template v-if="a.status === 'PENDING'">
                      <button @click="approveApp(a.applicationId, gmCampaign!.id)">승인</button>
                      <button class="danger" @click="rejectApp(a.applicationId, gmCampaign!.id)">거절</button>
                    </template>
                  </td>
                </tr>
                <tr v-if="!applications.length"><td colspan="4" class="empty">신청자가 없습니다.</td></tr>
              </tbody>
            </table>
          </div>
        </template>
      </div>
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
        <label>후기 게시판 연결 (사이트 내 컨텐츠 — 선택. 연결해야 신청·키 배포·후기 마감이 동작)
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

    </section>

    <!-- 영상 -->
    <section v-else-if="tab === 'videos'">
      <h4>영상 <button class="btn orange xs" @click="newVideo">+ 추가</button></h4>
      <table class="grid">
        <thead><tr><th>썸네일</th><th>제목</th><th>영상 URL</th><th>대표</th><th>정렬</th><th></th></tr></thead>
        <tbody>
          <tr v-for="v in videos" :key="v.id">
            <td><img v-if="v.thumbnailUrl" :src="v.thumbnailUrl" class="thumb" alt="" /><span v-else class="no-img">－</span></td>
            <td>{{ v.title }}</td><td class="url">{{ v.videoUrl }}</td><td>{{ v.featured ? '★' : '' }}</td>
            <td>{{ v.sortOrder }}</td>
            <td class="acts"><button @click="editVideo(v)">수정</button><button class="danger" @click="delVideo(v.id)">삭제</button></td>
          </tr>
          <tr v-if="!videos.length"><td colspan="6" class="empty">등록된 영상이 없습니다.</td></tr>
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

    </section>

    <!-- 협력사 -->
    <section v-else-if="tab === 'clients'">
      <h4>협력사 <button class="btn orange xs" @click="newLogo">+ 추가</button></h4>
      <table class="grid">
        <thead><tr><th>로고</th><th>이름</th><th>링크</th><th>정렬</th><th></th></tr></thead>
        <tbody>
          <tr v-for="cl in clients" :key="cl.id">
            <td><img v-if="cl.logoUrl" :src="cl.logoUrl" class="thumb contain" alt="" /><span v-else class="no-img">－</span></td>
            <td>{{ cl.name ?? '-' }}</td><td class="url">{{ cl.linkUrl }}</td>
            <td>{{ cl.sortOrder }}</td>
            <td class="acts"><button @click="editLogo(cl)">수정</button><button class="danger" @click="delLogo(cl.id)">삭제</button></td>
          </tr>
          <tr v-if="!clients.length"><td colspan="5" class="empty">등록된 협력사가 없습니다.</td></tr>
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
        <thead><tr><th>이미지</th><th>상품명</th><th>가격</th><th>재고</th><th>상태</th><th>정렬</th><th></th></tr></thead>
        <tbody>
          <tr v-for="g in goodsList" :key="g.id">
            <td><img v-if="g.imageUrl" :src="g.imageUrl" class="thumb" alt="" /><span v-else class="no-img">－</span></td>
            <td>{{ g.name }}</td><td>{{ won(g.price) }}</td><td>{{ g.stock }}</td>
            <td>{{ lbl(g.status) }}</td><td>{{ g.sortOrder }}</td>
            <td class="acts">
              <button @click="editGoods(g)">수정</button>
              <button class="danger" @click="removeGoods(g)">삭제</button>
            </td>
          </tr>
          <tr v-if="!goodsList.length"><td colspan="7" class="empty">등록된 굿즈가 없습니다.</td></tr>
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
            <select v-model="goodsEditing.status"><option value="ACTIVE">판매중</option><option value="HIDDEN">숨김</option></select>
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
              <td>{{ won(o.totalAmount) }}</td><td>{{ lbl(o.status) }}</td>
              <td>{{ o.receiverName }}</td><td>{{ o.receiverPhone }}</td>
              <td>{{ o.address }} {{ o.addressDetail }}</td>
              <td>{{ o.paidAt?.slice(0, 16).replace('T', ' ') ?? '-' }}</td>
            </tr>
            <tr v-if="!orders.length"><td colspan="9" class="empty">주문이 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 공지/스포트라이트 -->
    <section v-else-if="tab === 'notices'">
      <h3>공지사항</h3>
      <button class="btn orange sm" @click="newNotice">+ 새 공지</button>
      <table class="grid">
        <thead><tr><th>제목</th><th>고정</th><th>등록일</th><th></th></tr></thead>
        <tbody>
          <tr v-for="n in notices" :key="n.id">
            <td>{{ n.title }}</td>
            <td>{{ n.pinned ? '📌' : '' }}</td>
            <td>{{ n.createdAt?.slice(0, 10) }}</td>
            <td class="acts">
              <button @click="editNotice(n)">수정</button>
              <button class="danger" @click="removeNotice(n)">삭제</button>
            </td>
          </tr>
          <tr v-if="!notices.length"><td colspan="4" class="empty">등록된 공지가 없습니다.</td></tr>
        </tbody>
      </table>

      <div v-if="noticeEditing" class="form-card">
        <h4>{{ noticeEditing.id ? '공지 수정' : '새 공지' }}</h4>
        <label>제목<input v-model="noticeEditing.title" maxlength="200" /></label>
        <label>내용<textarea v-model="noticeEditing.content" rows="5"></textarea></label>
        <label class="chk"><input type="checkbox" v-model="noticeEditing.pinned" /> 상단 고정</label>
        <div class="acts">
          <button class="btn sm" :disabled="!noticeEditing.title.trim()" @click="saveNotice">저장</button>
          <button class="btn ghost sm" @click="noticeEditing = null">취소</button>
        </div>
      </div>

      <h3 style="margin-top: 32px">스포트라이트 (최근 50건 — 승인제)</h3>
      <p class="hint">스트리머가 신청한 방송 홍보 — <b>승인해야 노출</b>되고, 승인 시각부터 2시간 사이드바 최대 2개 표시.</p>
      <table class="grid">
        <thead><tr><th>제목</th><th>스트리머</th><th>플랫폼</th><th>링크</th><th>방송 예정</th><th>상태</th><th>등록</th><th></th></tr></thead>
        <tbody>
          <tr v-for="s in spotlights" :key="s.id">
            <td>{{ s.title }}</td>
            <td>{{ s.streamerName }}</td>
            <td>{{ s.platform }}</td>
            <td><a :href="s.streamUrl" target="_blank" rel="noopener">링크 ↗</a></td>
            <td>{{ s.scheduledAt ? s.scheduledAt.slice(5, 16).replace('T', ' ') : '-' }}</td>
            <td>{{ spotState(s) }}</td>
            <td>{{ s.createdAt?.slice(5, 16).replace('T', ' ') }}</td>
            <td class="acts">
              <button v-if="!s.approved" @click="approveSpotlight(s)">승인</button>
              <button class="danger" @click="removeSpotlight(s)">내리기</button>
            </td>
          </tr>
          <tr v-if="!spotlights.length"><td colspan="8" class="empty">등록된 스포트라이트가 없습니다.</td></tr>
        </tbody>
      </table>
    </section>

    <!-- 후기 미작성 경고 로그 -->
    <section v-else-if="tab === 'warnings'">
      <h3>후기 미작성 경고 로그</h3>
      <p class="hint">게임 키 수령 후 30일(연장 시 +7일) 안에 후기를 안 쓴 회원 목록이에요. 스윕은 1시간마다 자동 실행됩니다.</p>
      <table class="grid">
        <thead><tr><th>회원</th><th>컨텐츠</th><th>후기 마감</th><th>경고 시각</th><th>연장</th><th>후기</th></tr></thead>
        <tbody>
          <tr v-for="w in reviewWarnings" :key="w.applicationId">
            <td>#{{ w.memberId }} {{ w.nickname }}</td>
            <td>{{ w.campaignTitle }}</td>
            <td>{{ w.reviewDeadline?.slice(0, 10) }}</td>
            <td>{{ w.warnedAt?.slice(0, 16).replace('T', ' ') }}</td>
            <td>{{ w.deadlineExtended ? '사용' : '-' }}</td>
            <td>{{ w.reviewWritten ? '✅ 뒤늦게 작성' : '❌ 미작성' }}</td>
          </tr>
          <tr v-if="!reviewWarnings.length"><td colspan="6" class="empty">경고 이력이 없습니다.</td></tr>
        </tbody>
      </table>
    </section>

    <!-- 게시판 신고함 (항목 3) -->
    <section v-else-if="tab === 'reports'">
      <h3>스트리머 게시판 신고함</h3>
      <p class="hint">회원이 신고한 게시글이에요. "글 삭제"는 글과 걸린 신고를 함께 정리, "기각"은 신고만 지웁니다.</p>
      <table class="grid">
        <thead><tr><th>글</th><th>내용</th><th>작성자</th><th>게시판</th><th>신고자</th><th>사유</th><th>신고일</th><th></th></tr></thead>
        <tbody>
          <tr v-for="r in reports" :key="r.reportId">
            <td>{{ r.postTitle }}</td>
            <td style="max-width:260px;"><span style="font-size:11.5px;">{{ (r.postContent || '').slice(0, 80) }}</span></td>
            <td>{{ r.postAuthorName }}</td>
            <td>{{ r.streamerName }}</td>
            <td>{{ r.reporterName }}</td>
            <td>{{ r.reason || '-' }}</td>
            <td>{{ r.createdAt?.slice(5, 16).replace('T', ' ') }}</td>
            <td class="acts">
              <button class="danger" @click="deleteReportedPost(r.postId)">글 삭제</button>
              <button @click="dismissReport(r.reportId)">기각</button>
            </td>
          </tr>
          <tr v-if="!reports.length"><td colspan="8" class="empty">접수된 신고가 없습니다.</td></tr>
        </tbody>
      </table>
    </section>

    <!-- 무료소스 (항목 19) -->
    <section v-else-if="tab === 'resources'">
      <h3>무료소스 자료실</h3>
      <p class="hint">사이트 "무료소스" 페이지에 노출돼요. <b>링크(주소)로 등록</b>하는 게 기본이고, 파일 직접 업로드도 가능합니다(5MB 이하).</p>
      <div class="form-card">
        <h4>새 소스 등록</h4>
        <label>제목<input v-model="resTitle" maxlength="200" /></label>
        <label>설명<textarea v-model="resDesc" rows="3"></textarea></label>
        <label>링크(주소) — 소스가 있는 곳 주소
          <input v-model="resLink" placeholder="https://drive.google.com/... 또는 소스 페이지 주소" />
        </label>
        <label>또는 파일 직접 업로드 (선택)<input type="file" @change="pickResFile" /></label>
        <label>썸네일 이미지 (선택)<input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="pickResImage" /></label>
        <div class="form-acts">
          <button class="btn sm" :disabled="resUploading || !resTitle.trim() || (!resLink.trim() && !resFile)" @click="submitResource">
            {{ resUploading ? '업로드 중…' : '등록' }}
          </button>
        </div>
      </div>
      <table class="grid">
        <thead><tr><th>썸네일</th><th>제목</th><th>설명</th><th>파일</th><th>등록일</th><th></th></tr></thead>
        <tbody>
          <tr v-for="r in resources" :key="r.id">
            <td><img v-if="r.imageUrl" :src="r.imageUrl" class="thumb" alt="" /><span v-else class="no-img">－</span></td>
            <td>{{ r.title }}</td>
            <td style="max-width:280px;"><span style="font-size:11.5px;">{{ (r.description || '').slice(0, 60) }}</span></td>
            <td>
              <a v-if="r.fileUrl && r.fileUrl.startsWith('http')" :href="r.fileUrl" target="_blank" rel="noopener">링크 열기 ↗</a>
              <a v-else-if="r.fileUrl" :href="r.fileUrl" download>다운로드 ⬇</a>
            </td>
            <td>{{ r.createdAt?.slice(0, 10) }}</td>
            <td class="acts"><button class="danger" @click="removeResource(r.id)">삭제</button></td>
          </tr>
          <tr v-if="!resources.length"><td colspan="6" class="empty">등록된 소스가 없습니다.</td></tr>
        </tbody>
      </table>
    </section>

    <!-- 설정/권한 -->
    <!-- 회원 -->
    <section v-else-if="tab === 'members'">
      <table class="grid">
        <thead><tr><th>ID</th><th>닉네임</th><th>플랫폼</th><th>채널ID</th><th>팔로워</th><th>등급</th><th>가입일</th><th></th></tr></thead>
        <tbody>
          <tr v-for="m in members" :key="m.id">
            <td>{{ m.id }}</td>
            <td>
              <img v-if="m.profileImageUrl" :src="m.profileImageUrl" class="avatar" alt="" />
              {{ m.nickname }}
            </td>
            <td>{{ { CHZZK: '치지직', CIME: '씨미', SOOP: '숲' }[m.provider] ?? m.provider }}</td>
            <td class="mono">{{ m.channelId.slice(0, 12) }}…</td>
            <td>{{ m.followerCount ?? '-' }}</td>
            <td>
              <select :value="m.role" @change="changeMemberRole(m, ($event.target as HTMLSelectElement).value)">
                <option>VIEWER</option><option>STREAMER</option><option>REPORTER</option><option>ADMIN</option>
              </select>
              <span v-if="m.roleOverridden" class="badge">수동</span>
            </td>
            <td>{{ m.createdAt?.slice(0, 10) }}</td>
            <td class="acts">
              <button v-if="m.roleOverridden" @click="resetMemberRole(m)">자동 복귀</button>
            </td>
          </tr>
          <tr v-if="!members.length"><td colspan="8" class="empty">회원이 없습니다.</td></tr>
        </tbody>
      </table>
      <p class="hint">등급을 바꾸면 수동 고정(자동 재산정 제외)됩니다. "자동 복귀"를 누르면 다음 로그인부터 팔로워 기준으로 다시 계산돼요.</p>
    </section>

    <section v-else-if="tab === 'settings'">
      <h4>사이트 이미지 · 생방송</h4>
      <div class="form-card site-card">
        <label>생방송 공식 치지직 채널 ID (비우면 "준비중" 표시)
          <div class="live-row">
            <input v-model="liveChannelInput" placeholder="예) 5e3c8cabda51d938ca9d1ceda9680203" />
            <button class="btn sm" @click="saveLiveChannel">저장</button>
          </div>
        </label>
        <div class="live-banner-box">
          <h5 style="margin:14px 0 8px;">메인 라이브 배너 (히어로 바로 아래)</h5>
          <label class="chk" style="margin-bottom:8px;">
            <input type="checkbox" v-model="liveBannerEnabled" /> 배너 켜기 (끄면 메인에서 완전히 사라져요)
          </label>
          <label>방송 주소 (누구 방송을 띄울지 — 치지직/숲/유튜브 URL)
            <input v-model="liveBannerUrl" placeholder="https://chzzk.naver.com/live/..." />
          </label>
          <label>배너 제목 (비우면 "지금 방송 중")
            <input v-model="liveBannerTitle" placeholder="예) 씨미 님 스눅컵 연습 방송 🔴" />
          </label>
          <button class="btn sm" style="margin-top:8px;" @click="saveLiveBanner">라이브 배너 저장</button>
        </div>
        <div class="site-images">
          <div v-for="si in SITE_IMAGES" :key="si.key" class="site-img">
            <span class="site-img-label">{{ si.label }}</span>
            <div v-if="settingValue(si.key) === 'none'" class="site-img-empty">이미지 숨김 (표시 안 함)</div>
            <img v-else-if="settingValue(si.key)" :src="settingValue(si.key)" alt="" />
            <div v-else class="site-img-empty">기본 이미지 사용 중</div>
            <input type="file" accept="image/jpeg,image/png,image/gif,image/webp" @change="pickSettingImage($event, si.key)" />
            <div style="display:flex;gap:6px;margin-top:6px;">
              <button class="btn ghost xs" @click="setSettingImage(si.key, 'none')">이미지 없애기</button>
              <button class="btn ghost xs" @click="setSettingImage(si.key, '-')">기본으로</button>
            </div>
          </div>
        </div>
        <p class="hint" v-if="imgUploading">업로드 중…</p>
        <p class="hint" v-else>이미지를 선택하면 바로 업로드·적용됩니다. "이미지 없애기"는 해당 배너 이미지를 아예 표시하지 않아요.</p>
      </div>

      <h4 style="margin-top:28px">배너 문구</h4>
      <div class="form-card site-card">
        <div v-for="b in BANNER_TEXTS" :key="b.page" class="banner-text-row">
          <span class="banner-text-label">{{ b.label }}</span>
          <input v-model="bannerTextInputs[`BANNER_${b.page}_TITLE`]" placeholder="제목 (비우면 표시 안 함)" />
          <input v-model="bannerTextInputs[`BANNER_${b.page}_SUB`]" placeholder="부제 문구 (비우면 표시 안 함)" class="wide" />
          <button class="btn sm" @click="saveBannerText(b.page)">저장</button>
        </div>
        <p class="hint">비워두고 저장하면 문구가 <b>표시되지 않아요</b>. 기본 문구로 돌리려면 <code>-</code> 하나만 입력 후 저장.</p>
      </div>

      <h4 style="margin-top:28px">사이드바 메뉴 표시</h4>
      <div class="form-card site-card">
        <div style="display:flex;flex-wrap:wrap;gap:14px;">
          <label v-for="m in MENU_ITEMS" :key="m.key" class="chk" style="margin:0;">
            <input type="checkbox" v-model="menuInputs[m.key]" /> {{ m.label }}
          </label>
        </div>
        <button class="btn sm" style="margin-top:10px;align-self:flex-start;" @click="saveMenus">메뉴 표시 저장</button>
        <p class="hint">체크 해제한 메뉴는 사이드바·모바일 메뉴에서 숨겨져요. (페이지 주소로 직접 들어가는 건 막지 않아요)</p>
      </div>

      <h4 style="margin-top:28px">설정값</h4>
      <table class="grid">
        <thead><tr><th>키</th><th>값</th><th>설명</th><th></th></tr></thead>
        <tbody>
          <tr v-for="s in generalSettings" :key="s.settingKey">
            <td>{{ s.settingKey }}</td>
            <td><input v-model="s.settingValue" /></td>
            <td>{{ s.description }}</td>
            <td><button @click="saveSetting(s.settingKey, s.settingValue)">저장</button></td>
          </tr>
          <tr v-if="!generalSettings.length"><td colspan="4" class="empty">설정값이 없습니다.</td></tr>
        </tbody>
      </table>

      <h4 style="margin-top:28px">권한 수동 오버라이드</h4>
      <div class="override">
        <input type="number" v-model.number="overrideMemberId" placeholder="회원 ID" />
        <select v-model="overrideRole"><option>VIEWER</option><option>STREAMER</option><option>REPORTER</option><option>ADMIN</option></select>
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
  </div>
</template>

<style scoped>
/* SNUK 시안 다크 모노톤 — 관리자 콘솔(셸 밖 bare 렌더라 자체 완결 스타일) */
.admin-page {
  --a-bg: #111113; --a-bg2: #1c1c1f; --a-bg3: #262629; --a-bg4: #303035;
  --a-text: #f0f0f2; --a-text2: #8a8a92; --a-text3: #505058;
  --a-border: rgba(255, 255, 255, 0.1); --a-border2: rgba(255, 255, 255, 0.2);
  --a-red: #ff6b6b; --a-radius: 10px; --a-radius2: 16px;
  min-height: 100vh; background: var(--a-bg); color: var(--a-text);
  font-family: 'Pretendard', 'Noto Sans KR', -apple-system, 'Segoe UI', sans-serif;
}
.admin-wrap { max-width: 1200px; margin: 0 auto; padding: 32px 24px 80px; }
.admin-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; }
.admin-head h2 { margin: 0; font-size: 26px; font-weight: 800; letter-spacing: 2px; }
.admin-head h2 span { color: var(--a-text2); font-weight: 600; letter-spacing: 0; font-size: 18px; margin-left: 8px; }
.home-link { color: var(--a-text2); text-decoration: none; font-size: 13px; border: 1px solid var(--a-border); border-radius: 999px; padding: 7px 14px; transition: 0.15s; }
.home-link:hover { color: var(--a-text); border-color: var(--a-border2); background: var(--a-bg3); }

h4, h5 { color: var(--a-text); }
.tabs { display: flex; gap: 4px; margin-bottom: 20px; border-bottom: 1px solid var(--a-border); flex-wrap: wrap; }
.tabs button { background: none; border: 0; padding: 10px 16px; font-weight: 700; font-size: 14px; color: var(--a-text2); border-bottom: 2px solid transparent; cursor: pointer; transition: 0.15s; }
.tabs button:hover { color: var(--a-text); }
.tabs button.on { color: var(--a-text); border-bottom-color: var(--a-text); }

/* 버튼 — 시안 CTA(라이트 온 다크) 톤 */
.btn { display: inline-flex; align-items: center; gap: 6px; border: 1px solid var(--a-border2); background: var(--a-bg3); color: var(--a-text); border-radius: 8px; padding: 8px 14px; font-weight: 700; font-size: 14px; cursor: pointer; transition: 0.15s; }
.btn:hover { background: var(--a-bg4); }
.btn.orange { background: #e8e8e8; color: #18181c; border-color: transparent; }
.btn.orange:hover { background: #fff; }
.btn.ghost { background: transparent; }
.btn.sm { padding: 6px 12px; font-size: 13px; }
.btn.xs, .xs { font-size: 12px; padding: 3px 10px; margin-left: 6px; }

.grid { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 14px; }
.grid th { color: var(--a-text2); font-weight: 600; font-size: 12px; }
.grid th, .grid td { border-bottom: 1px solid var(--a-border); padding: 9px 10px; text-align: left; }
.grid tr.sel { background: rgba(255, 255, 255, 0.06); }
.grid .empty, .empty { color: var(--a-text3); text-align: center; }
.grid button:not(.btn), .acts button { margin-right: 6px; border: 1px solid var(--a-border2); background: var(--a-bg3); color: var(--a-text); border-radius: 6px; padding: 4px 10px; font-size: 13px; cursor: pointer; transition: 0.15s; }
.grid button:not(.btn):hover, .acts button:hover { background: var(--a-bg4); }
.acts button.danger, .danger { color: var(--a-red); border-color: rgba(255, 107, 107, 0.35); }

.avatar { width: 24px; height: 24px; border-radius: 50%; vertical-align: middle; margin-right: 6px; object-fit: cover; }
.mono { font-family: monospace; font-size: 12px; color: var(--a-text2); }
.badge { margin-left: 6px; font-size: 11px; font-weight: 800; color: var(--a-text); border: 1px solid var(--a-border2); border-radius: 999px; padding: 1px 7px; }
.hint { margin-top: 12px; font-size: 13px; color: var(--a-text2); }
.logo-upload { display: flex; align-items: center; gap: 10px; margin-top: 4px; }
.logo-preview { width: 48px; height: 48px; object-fit: contain; border: 1px solid var(--a-border); border-radius: 6px; background: var(--a-bg3); }

.form-card, .manage { margin-top: 24px; border: 1px solid var(--a-border); border-radius: var(--a-radius2); padding: 20px; background: var(--a-bg2); }
.form-card label { display: block; margin-bottom: 10px; font-size: 13px; font-weight: 600; color: var(--a-text2); }
input, textarea, select { background: var(--a-bg3); color: var(--a-text); border: 1px solid var(--a-border); border-radius: 6px; font: inherit; }
input::placeholder, textarea::placeholder { color: var(--a-text3); }
input:focus, textarea:focus, select:focus { outline: none; border-color: var(--a-border2); }
.form-card input, .form-card textarea, .form-card select { width: 100%; padding: 8px; margin-top: 4px; }
.grid input, .grid select { padding: 6px 8px; }
.form-card .row3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }
.form-card .chk { display: flex; align-items: center; gap: 6px; }
.form-card .chk input { width: auto; }
.form-acts { display: flex; gap: 8px; margin-top: 12px; }
.keys textarea, .manage textarea { width: 100%; min-height: 90px; padding: 8px; margin-bottom: 8px; }
.result { color: var(--a-text); font-weight: 700; }
.collab-admin { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.collab-admin ul { list-style: none; padding: 0; }
.collab-admin li { padding: 6px 0; border-bottom: 1px solid var(--a-border); display: flex; justify-content: space-between; align-items: center; }
.override { display: flex; gap: 10px; align-items: center; }
.override input, .override select { padding: 8px; }
.url { color: var(--a-text2); max-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* 라이트 테마 — 메인 사이트(snuk-theme)와 동일 키 공유 */
.admin-page[data-theme="light"] {
  --a-bg: #f8f8f9; --a-bg2: #ffffff; --a-bg3: #f0f0f2; --a-bg4: #e4e4e8;
  --a-text: #18181c; --a-text2: #505058; --a-text3: #909098;
  --a-border: rgba(0, 0, 0, 0.1); --a-border2: rgba(0, 0, 0, 0.2);
}
.admin-page[data-theme="light"] .btn.orange { background: #18181c; color: #fff; }
.admin-page[data-theme="light"] .btn.orange:hover { background: #303035; }
.admin-page[data-theme="light"] .tabs button.on { border-bottom-color: #18181c; }

.head-acts { display: flex; gap: 8px; align-items: center; }
.head-acts button.home-link { background: transparent; cursor: pointer; font: inherit; font-size: 13px; }

/* 목록 썸네일 */
.thumb { width: 52px; height: 32px; object-fit: cover; border-radius: 6px; background: var(--a-bg3); display: block; }
.thumb.contain { object-fit: contain; }
.no-img { color: var(--a-text3); }

/* 설정 탭 — 사이트 이미지/생방송 카드 */
.site-card { margin-top: 12px; }
.live-row { display: flex; gap: 8px; margin-top: 4px; }
.live-row input { flex: 1; padding: 8px; }
.site-images { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; margin-top: 14px; }
.site-img { display: flex; flex-direction: column; gap: 8px; }
.site-img-label { font-size: 13px; font-weight: 600; color: var(--a-text2); }
.banner-text-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; flex-wrap: wrap; }
.banner-text-label { width: 76px; font-size: 13px; font-weight: 600; color: var(--a-text2); flex: none; }
.banner-text-row input { flex: 1; min-width: 140px; }
.banner-text-row input.wide { flex: 2; }
.site-img img { width: 100%; height: 90px; object-fit: cover; border-radius: 8px; border: 1px solid var(--a-border); background: var(--a-bg3); }
.site-img-empty { height: 90px; border: 1px dashed var(--a-border2); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 12px; color: var(--a-text3); }
</style>
