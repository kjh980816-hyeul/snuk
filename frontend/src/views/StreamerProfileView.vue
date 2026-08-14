<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { streamerApi, streamerPageApi } from '@/api'
import type {
  RouletteItem, SongRequestItem, StreamerCommandItem, StreamerNotice, StreamerPost,
  StreamerProfile, StreamerScheduleItem, WikiSection,
} from '@/api/types'
import { useAuthStore } from '@/stores/auth'

// 스트리머 프로필: 팔로우 + 개인 게시판 + (V23) 공지/방송일정/위키/명령어·룰렛/노래신청
const route = useRoute()
const auth = useAuthStore()

const profile = ref<StreamerProfile | null>(null)
const posts = ref<StreamerPost[]>([])
const loading = ref(true)
const notFound = ref(false)

// ----- 탭 -----
type Tab = 'board' | 'notice' | 'schedule' | 'wiki' | 'tools' | 'songs'
const tab = ref<Tab>('board')
const TABS: Array<{ k: Tab; n: string }> = [
  { k: 'board', n: '게시판' }, { k: 'notice', n: '공지' }, { k: 'schedule', n: '방송 일정' },
  { k: 'wiki', n: '위키' }, { k: 'tools', n: '명령어·룰렛' }, { k: 'songs', n: '노래 신청' },
]

const writeOpen = ref(false)
const title = ref('')
const content = ref('')
const submitting = ref(false)
const followBusy = ref(false)

const streamerId = computed(() => Number(route.params.id))
const isMe = computed(() => auth.me?.id === profile.value?.streamer.id)

const platLabel: Record<string, string> = { CHZZK: '치지직', SOOP: '숲', CIME: '씨미' }
const platColor: Record<string, string> = { CHZZK: '#00c73c', SOOP: '#34c7ff', CIME: '#7c5cff' }

async function load() {
  loading.value = true
  notFound.value = false
  try {
    profile.value = await streamerApi.profile(streamerId.value)
    posts.value = await streamerApi.posts(streamerId.value)
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function toggleFollow() {
  if (!profile.value || followBusy.value) return
  if (!auth.isLoggedIn) {
    ;(window as unknown as { openLogin?: () => void }).openLogin?.()
    return
  }
  followBusy.value = true
  try {
    const res = profile.value.following
      ? await streamerApi.unfollow(streamerId.value)
      : await streamerApi.follow(streamerId.value)
    profile.value = { ...profile.value, following: res.following, followCount: res.followCount }
  } catch (e) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    alert(msg ?? '요청에 실패했습니다.')
  } finally {
    followBusy.value = false
  }
}

async function submitPost() {
  if (!title.value.trim() || submitting.value) return
  submitting.value = true
  try {
    await streamerApi.writePost(streamerId.value, {
      title: title.value.trim(),
      content: content.value.trim(),
    })
    title.value = ''
    content.value = ''
    writeOpen.value = false
    posts.value = await streamerApi.posts(streamerId.value)
  } catch (e) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    alert(msg ?? '글 작성에 실패했습니다.')
  } finally {
    submitting.value = false
  }
}

async function removePost(p: StreamerPost) {
  if (!confirm(`'${p.title}' 글을 삭제할까요?`)) return
  try {
    await streamerApi.deletePost(p.id)
    posts.value = await streamerApi.posts(streamerId.value)
  } catch (e) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    alert(msg ?? '삭제 권한이 없습니다.')
  }
}

// 글 신고(항목 3) — 로그인 회원 1인 1신고, 어드민 신고함으로 접수
async function reportPost(p: StreamerPost) {
  if (!auth.isLoggedIn) {
    ;(window as unknown as { openLogin?: () => void }).openLogin?.()
    return
  }
  const reason = prompt(`'${p.title}' 글을 신고합니다.\n신고 사유를 입력해주세요. (선택)`)
  if (reason === null) return
  try {
    await streamerApi.reportPost(p.id, reason.trim())
    alert('신고가 접수됐습니다. 운영진 확인 후 처리됩니다.')
  } catch (e) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    alert(msg ?? '신고에 실패했습니다.')
  }
}

function dt(v: string) {
  return v?.slice(0, 16).replace('T', ' ')
}

function errMsg(e: unknown, fallback: string) {
  return (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
}

// 관리 권한: 본인 스트리머 또는 ADMIN (백엔드가 재검증)
const canManage = computed(() => isMe.value || auth.me?.role === 'ADMIN')

// ----- 공지 (V23) -----
const notices = ref<StreamerNotice[]>([])
const nTitle = ref(''); const nBody = ref(''); const nImportant = ref(false); const nOpen = ref(false)
async function loadNotices() { notices.value = await streamerPageApi.notices(streamerId.value).catch(() => []) }
async function addNotice() {
  if (!nTitle.value.trim()) return
  try {
    await streamerPageApi.createNotice(streamerId.value, { title: nTitle.value.trim(), body: nBody.value.trim(), important: nImportant.value })
    nTitle.value = ''; nBody.value = ''; nImportant.value = false; nOpen.value = false
    await loadNotices()
  } catch (e) { alert(errMsg(e, '공지 등록에 실패했습니다.')) }
}
async function delNotice(n: StreamerNotice) {
  if (!confirm(`'${n.title}' 공지를 삭제할까요?`)) return
  try { await streamerPageApi.deleteNotice(n.id); await loadNotices() } catch (e) { alert(errMsg(e, '삭제에 실패했습니다.')) }
}

// ----- 방송 일정 (V23) -----
const schedules = ref<StreamerScheduleItem[]>([])
const sDate = ref(''); const sTime = ref('20:00'); const sTitle = ref(''); const sGame = ref(''); const sMates = ref(''); const sOpen = ref(false)
const upcoming = computed(() => schedules.value.filter((s) => s.startAt >= new Date().toISOString().slice(0, 16)).sort((a, b) => a.startAt.localeCompare(b.startAt)))
const past = computed(() => schedules.value.filter((s) => s.startAt < new Date().toISOString().slice(0, 16)))
async function loadSchedules() { schedules.value = await streamerPageApi.schedules(streamerId.value).catch(() => []) }
async function addSchedule() {
  if (!sDate.value || !sTitle.value.trim()) return
  try {
    await streamerPageApi.createSchedule(streamerId.value, {
      startAt: `${sDate.value}T${sTime.value || '20:00'}:00`, title: sTitle.value.trim(),
      game: sGame.value.trim() || null, mates: sMates.value.trim() || null,
    })
    sDate.value = ''; sTitle.value = ''; sGame.value = ''; sMates.value = ''; sOpen.value = false
    await loadSchedules()
  } catch (e) { alert(errMsg(e, '일정 등록에 실패했습니다.')) }
}
async function delSchedule(s: StreamerScheduleItem) {
  if (!confirm('일정을 삭제할까요?')) return
  try { await streamerPageApi.deleteSchedule(s.id); await loadSchedules() } catch (e) { alert(errMsg(e, '삭제에 실패했습니다.')) }
}

// ----- 위키 (V23) -----
const wiki = ref<WikiSection[]>([])
const wikiEditing = ref(false)
const wikiDraft = ref<WikiSection[]>([])
async function loadWiki() { wiki.value = await streamerPageApi.wiki(streamerId.value).catch(() => []) }
function startWikiEdit() {
  wikiDraft.value = wiki.value.length ? wiki.value.map((s) => ({ ...s })) : [{ t: '소개', b: '' }]
  wikiEditing.value = true
}
async function saveWikiDoc() {
  try {
    wiki.value = await streamerPageApi.saveWiki(streamerId.value, wikiDraft.value.filter((s) => s.t.trim()))
    wikiEditing.value = false
  } catch (e) { alert(errMsg(e, '저장에 실패했습니다.')) }
}

// ----- 명령어·룰렛 (V23) -----
const commands = ref<StreamerCommandItem[]>([])
const cName = ref(''); const cResp = ref('')
const roulette = ref<RouletteItem[]>([])
const rLabel = ref(''); const rWeight = ref(1)
const rouletteResult = ref<string | null>(null)
const spinning = ref(false)
async function loadTools() {
  commands.value = await streamerPageApi.commands(streamerId.value).catch(() => [])
  roulette.value = await streamerPageApi.roulette(streamerId.value).catch(() => [])
}
async function addCommand() {
  if (!cName.value.trim() || !cResp.value.trim()) return
  try {
    await streamerPageApi.createCommand(streamerId.value, { name: cName.value.trim(), response: cResp.value.trim(), enabled: true })
    cName.value = ''; cResp.value = ''
    await loadTools()
  } catch (e) { alert(errMsg(e, '명령어 등록에 실패했습니다.')) }
}
async function toggleCommand(c: StreamerCommandItem) {
  try { await streamerPageApi.updateCommand(c.id, { enabled: !c.enabled }); await loadTools() } catch (e) { alert(errMsg(e, '변경에 실패했습니다.')) }
}
async function delCommand(c: StreamerCommandItem) {
  if (!confirm(`${c.name} 명령어를 삭제할까요?`)) return
  try { await streamerPageApi.deleteCommand(c.id); await loadTools() } catch (e) { alert(errMsg(e, '삭제에 실패했습니다.')) }
}
async function addRouletteItem() {
  if (!rLabel.value.trim()) return
  try {
    await streamerPageApi.createRouletteItem(streamerId.value, { label: rLabel.value.trim(), weight: Math.max(1, rWeight.value || 1) })
    rLabel.value = ''; rWeight.value = 1
    await loadTools()
  } catch (e) { alert(errMsg(e, '항목 추가에 실패했습니다.')) }
}
async function delRouletteItem(r: RouletteItem) {
  try { await streamerPageApi.deleteRouletteItem(r.id); await loadTools() } catch (e) { alert(errMsg(e, '삭제에 실패했습니다.')) }
}
function spinRoulette() {
  if (!roulette.value.length || spinning.value) return
  spinning.value = true
  rouletteResult.value = null
  // 가중치 랜덤 — 잠깐 돌아가는 연출 후 확정
  const total = roulette.value.reduce((a, r) => a + r.weight, 0)
  let pick = Math.random() * total
  let chosen = roulette.value[0]
  for (const r of roulette.value) { pick -= r.weight; if (pick <= 0) { chosen = r; break } }
  let ticks = 0
  const timer = setInterval(() => {
    rouletteResult.value = roulette.value[ticks % roulette.value.length].label
    ticks += 1
    if (ticks > 14) {
      clearInterval(timer)
      rouletteResult.value = chosen.label
      spinning.value = false
    }
  }, 90)
}

// ----- 노래 신청 (V23) -----
const songQueue = ref<SongRequestItem[]>([])
const songRecent = ref<SongRequestItem[]>([])
const songTitle = ref('')
async function loadSongs() {
  const d = await streamerPageApi.songs(streamerId.value).catch(() => ({ queued: [], recent: [] }))
  songQueue.value = d.queued
  songRecent.value = d.recent
}
async function requestSong() {
  if (!auth.isLoggedIn) { (window as unknown as { openLogin?: () => void }).openLogin?.(); return }
  if (!songTitle.value.trim()) return
  try {
    await streamerPageApi.requestSong(streamerId.value, songTitle.value.trim())
    songTitle.value = ''
    await loadSongs()
  } catch (e) { alert(errMsg(e, '신청에 실패했습니다.')) }
}
async function decideSong(s: SongRequestItem, status: 'PLAYED' | 'SKIPPED') {
  try { await streamerPageApi.decideSong(s.id, status); await loadSongs() } catch (e) { alert(errMsg(e, '처리에 실패했습니다.')) }
}
async function cancelSong(s: SongRequestItem) {
  try { await streamerPageApi.cancelSong(s.id); await loadSongs() } catch (e) { alert(errMsg(e, '취소에 실패했습니다.')) }
}

// 탭 전환 시 해당 탭 데이터 lazy 로드
watch(tab, (t) => {
  if (t === 'notice') void loadNotices()
  else if (t === 'schedule') void loadSchedules()
  else if (t === 'wiki') void loadWiki()
  else if (t === 'tools') void loadTools()
  else if (t === 'songs') void loadSongs()
})

onMounted(load)
watch(() => route.params.id, () => { tab.value = 'board'; void load() })
</script>

<template>
  <section class="sp-page">
    <div class="inner">
      <RouterLink to="/streamers" class="sp-back">&lt; 스트리머 목록</RouterLink>

      <div v-if="loading" class="sp-empty">불러오는 중…</div>
      <div v-else-if="notFound || !profile" class="sp-empty">존재하지 않는 스트리머입니다.</div>

      <template v-else>
        <!-- 프로필 헤더 -->
        <div class="sp-card">
          <div class="sp-avatar">
            <img v-if="profile.streamer.profileImageUrl" :src="profile.streamer.profileImageUrl" alt="" />
            <span v-else>{{ profile.streamer.nickname.slice(0, 1) }}</span>
          </div>
          <div class="sp-info">
            <div class="sp-name-row">
              <span class="sp-name">{{ profile.streamer.nickname }}</span>
              <span class="sp-plat" :style="{ color: platColor[profile.streamer.provider], borderColor: platColor[profile.streamer.provider] + '55', background: platColor[profile.streamer.provider] + '14' }">
                {{ platLabel[profile.streamer.provider] ?? profile.streamer.provider }}
              </span>
            </div>
            <div class="sp-stats-row">
              <span>SNUK 팔로워 <b>{{ profile.followCount.toLocaleString('ko-KR') }}</b></span>
              <span v-if="profile.streamer.followerCount != null">
                플랫폼 팔로워 <b>{{ profile.streamer.followerCount.toLocaleString('ko-KR') }}</b>
              </span>
            </div>
            <div class="sp-actions">
              <button v-if="!isMe" class="sp-btn" :class="{ on: profile.following }" :disabled="followBusy" @click="toggleFollow">
                {{ profile.following ? '✓ 팔로잉' : '+ 팔로우' }}
              </button>
              <a v-if="profile.streamer.channelUrl" :href="profile.streamer.channelUrl" target="_blank" rel="noopener">
                <button class="sp-btn ghost">방송국 가기 ↗</button>
              </a>
            </div>
          </div>
        </div>

        <!-- 탭 (V23: 게시판/공지/일정/위키/명령어·룰렛/노래신청) -->
        <div class="sp-tabs">
          <button v-for="t in TABS" :key="t.k" class="sp-tab" :class="{ on: tab === t.k }" @click="tab = t.k">{{ t.n }}</button>
        </div>

        <!-- 공지 -->
        <template v-if="tab === 'notice'">
          <div class="sp-board-head">
            <h3>공지사항 <span class="sp-count">{{ notices.length }}</span></h3>
            <button v-if="canManage" class="sp-btn" @click="nOpen = !nOpen">{{ nOpen ? '작성 취소' : '공지 쓰기' }}</button>
          </div>
          <div v-if="nOpen" class="sp-write">
            <input v-model="nTitle" placeholder="공지 제목" maxlength="200" />
            <textarea v-model="nBody" rows="4" placeholder="내용"></textarea>
            <label class="sp-chk"><input v-model="nImportant" type="checkbox" /> 중요 공지(맨 위 고정)</label>
            <button class="sp-btn" :disabled="!nTitle.trim()" @click="addNotice">등록</button>
          </div>
          <div v-if="!notices.length" class="sp-empty">아직 공지가 없습니다.</div>
          <article v-for="n in notices" :key="n.id" class="sp-post">
            <div class="sp-post-head">
              <div>
                <div class="sp-post-title"><span v-if="n.important" class="sp-imp">중요</span>{{ n.title }}</div>
                <div class="sp-post-meta">{{ dt(n.createdAt) }}</div>
              </div>
              <button v-if="canManage" class="sp-del" @click="delNotice(n)">삭제</button>
            </div>
            <p v-if="n.body" class="sp-post-content" style="margin-left:0;">{{ n.body }}</p>
          </article>
        </template>

        <!-- 방송 일정 -->
        <template v-else-if="tab === 'schedule'">
          <div class="sp-board-head">
            <h3>방송 일정 <span class="sp-count">{{ schedules.length }}</span></h3>
            <button v-if="canManage" class="sp-btn" @click="sOpen = !sOpen">{{ sOpen ? '작성 취소' : '일정 추가' }}</button>
          </div>
          <div v-if="sOpen" class="sp-write">
            <div class="sp-row">
              <input v-model="sDate" type="date" />
              <input v-model="sTime" type="time" />
            </div>
            <input v-model="sTitle" placeholder="방송 제목" maxlength="200" />
            <div class="sp-row">
              <input v-model="sGame" placeholder="게임 (선택)" />
              <input v-model="sMates" placeholder="함께하는 스트리머 (선택, 쉼표 구분)" />
            </div>
            <button class="sp-btn" :disabled="!sDate || !sTitle.trim()" @click="addSchedule">등록</button>
          </div>
          <h4 v-if="upcoming.length" class="sp-sub">다가오는 일정</h4>
          <article v-for="s in upcoming" :key="s.id" class="sp-post sp-sched">
            <div class="sp-post-head">
              <div>
                <div class="sp-post-title">{{ s.title }}</div>
                <div class="sp-post-meta">📅 {{ dt(s.startAt) }}<template v-if="s.game"> · 🎮 {{ s.game }}</template><template v-if="s.mates"> · 👥 {{ s.mates }}</template></div>
              </div>
              <button v-if="canManage" class="sp-del" @click="delSchedule(s)">삭제</button>
            </div>
          </article>
          <h4 v-if="past.length" class="sp-sub" style="margin-top:18px;">지난 일정</h4>
          <article v-for="s in past" :key="s.id" class="sp-post sp-sched" style="opacity:.55;">
            <div class="sp-post-head">
              <div>
                <div class="sp-post-title">{{ s.title }}</div>
                <div class="sp-post-meta">📅 {{ dt(s.startAt) }}<template v-if="s.game"> · 🎮 {{ s.game }}</template></div>
              </div>
              <button v-if="canManage" class="sp-del" @click="delSchedule(s)">삭제</button>
            </div>
          </article>
          <div v-if="!schedules.length" class="sp-empty">등록된 방송 일정이 없습니다.</div>
        </template>

        <!-- 위키 -->
        <template v-else-if="tab === 'wiki'">
          <div class="sp-board-head">
            <h3>위키</h3>
            <button v-if="canManage && !wikiEditing" class="sp-btn" @click="startWikiEdit">{{ wiki.length ? '문서 수정' : '문서 작성' }}</button>
          </div>
          <template v-if="wikiEditing">
            <div v-for="(s, i) in wikiDraft" :key="i" class="sp-write">
              <div class="sp-row">
                <input v-model="s.t" placeholder="섹션 제목 (예: 소개, 채팅 규칙)" style="flex:1;" />
                <button class="sp-del" @click="wikiDraft.splice(i, 1)">섹션 삭제</button>
              </div>
              <textarea v-model="s.b" rows="4" placeholder="내용"></textarea>
            </div>
            <div class="sp-row" style="margin-bottom:16px;">
              <button class="sp-btn ghost" @click="wikiDraft.push({ t: '', b: '' })">+ 섹션 추가</button>
              <button class="sp-btn" @click="saveWikiDoc">저장</button>
              <button class="sp-btn ghost" @click="wikiEditing = false">취소</button>
            </div>
          </template>
          <template v-else>
            <div v-if="!wiki.length" class="sp-empty">아직 작성된 위키 문서가 없습니다.</div>
            <article v-for="(s, i) in wiki" :key="i" class="sp-post">
              <div class="sp-post-title" style="margin-bottom:8px;">{{ s.t }}</div>
              <p class="sp-post-content" style="margin-left:0;">{{ s.b }}</p>
            </article>
            <p v-if="wiki.length" class="sp-wiki-foot">이 문서는 스트리머 본인이 작성했습니다.</p>
          </template>
        </template>

        <!-- 명령어·룰렛 -->
        <template v-else-if="tab === 'tools'">
          <div class="sp-board-head"><h3>채팅 명령어 <span class="sp-count">{{ commands.length }}</span></h3></div>
          <div v-if="canManage" class="sp-write">
            <div class="sp-row">
              <input v-model="cName" placeholder="!명령어" style="max-width:160px;" />
              <input v-model="cResp" placeholder="응답 내용" style="flex:1;" />
              <button class="sp-btn" :disabled="!cName.trim() || !cResp.trim()" @click="addCommand">추가</button>
            </div>
          </div>
          <div v-if="!commands.length" class="sp-empty">등록된 명령어가 없습니다.</div>
          <article v-for="c in commands" :key="c.id" class="sp-post" :style="c.enabled ? '' : 'opacity:.5;'">
            <div class="sp-post-head">
              <div style="min-width:0;">
                <div class="sp-post-title"><code class="sp-cmd">{{ c.name }}</code></div>
                <div class="sp-post-meta" style="margin-top:4px;">{{ c.response }}</div>
              </div>
              <div v-if="canManage" class="sp-post-btns">
                <button class="sp-report" @click="toggleCommand(c)">{{ c.enabled ? '끄기' : '켜기' }}</button>
                <button class="sp-del" @click="delCommand(c)">삭제</button>
              </div>
            </div>
          </article>

          <div class="sp-board-head" style="margin-top:26px;"><h3>후원 룰렛 <span class="sp-count">{{ roulette.length }}</span></h3></div>
          <div v-if="canManage" class="sp-write">
            <div class="sp-row">
              <input v-model="rLabel" placeholder="항목 이름 (예: 노래 1곡)" style="flex:1;" />
              <input v-model.number="rWeight" type="number" min="1" style="max-width:90px;" title="가중치" />
              <button class="sp-btn" :disabled="!rLabel.trim()" @click="addRouletteItem">추가</button>
            </div>
          </div>
          <div v-if="!roulette.length" class="sp-empty">등록된 룰렛 항목이 없습니다.</div>
          <template v-else>
            <div class="sp-roulette">
              <span v-for="r in roulette" :key="r.id" class="sp-rou-item" :class="{ hit: rouletteResult === r.label }">
                {{ r.label }} <em>×{{ r.weight }}</em>
                <button v-if="canManage" class="sp-rou-del" @click="delRouletteItem(r)">✕</button>
              </span>
            </div>
            <button class="sp-btn" style="margin-bottom:8px;" :disabled="spinning" @click="spinRoulette">
              {{ spinning ? '돌리는 중…' : '🎲 룰렛 돌리기' }}
            </button>
            <div v-if="rouletteResult && !spinning" class="sp-rou-result">🎉 결과: <b>{{ rouletteResult }}</b></div>
          </template>
        </template>

        <!-- 노래 신청 -->
        <template v-else-if="tab === 'songs'">
          <div class="sp-board-head"><h3>노래 신청 대기열 <span class="sp-count">{{ songQueue.length }}</span></h3></div>
          <div class="sp-write">
            <div class="sp-row">
              <input v-model="songTitle" placeholder="신청할 곡 제목 (가수 - 곡명)" style="flex:1;" maxlength="200" @keydown.enter="requestSong" />
              <button class="sp-btn" :disabled="!songTitle.trim()" @click="requestSong">신청</button>
            </div>
            <p class="sp-hint">한 사람당 대기열 3곡까지 신청할 수 있어요.</p>
          </div>
          <div v-if="!songQueue.length" class="sp-empty">대기 중인 신청곡이 없습니다.</div>
          <article v-for="(s, i) in songQueue" :key="s.id" class="sp-post">
            <div class="sp-post-head">
              <div style="min-width:0;">
                <div class="sp-post-title">{{ i + 1 }}. {{ s.title }}</div>
                <div class="sp-post-meta">{{ s.requesterName }} · {{ dt(s.createdAt) }}</div>
              </div>
              <div class="sp-post-btns">
                <template v-if="canManage">
                  <button class="sp-btn" style="padding:5px 11px;font-size:11px;" @click="decideSong(s, 'PLAYED')">✅ 재생</button>
                  <button class="sp-report" @click="decideSong(s, 'SKIPPED')">스킵</button>
                </template>
                <button v-else-if="s.mine" class="sp-del" @click="cancelSong(s)">취소</button>
              </div>
            </div>
          </article>
          <template v-if="songRecent.length">
            <h4 class="sp-sub" style="margin-top:18px;">최근 처리된 곡</h4>
            <article v-for="s in songRecent" :key="s.id" class="sp-post" style="opacity:.6;">
              <div class="sp-post-head">
                <div>
                  <div class="sp-post-title">{{ s.title }}</div>
                  <div class="sp-post-meta">{{ s.requesterName }} · {{ s.status === 'PLAYED' ? '✅ 재생됨' : '건너뜀' }}</div>
                </div>
              </div>
            </article>
          </template>
        </template>

        <!-- 개인 게시판 -->
        <div v-if="tab === 'board'" class="sp-board-head">
          <h3>{{ profile.streamer.nickname }}의 게시판 <span class="sp-count">{{ posts.length }}</span></h3>
          <button v-if="auth.isLoggedIn" class="sp-btn" @click="writeOpen = !writeOpen">
            {{ writeOpen ? '작성 취소' : '글쓰기' }}
          </button>
          <button v-else class="sp-btn ghost" @click="auth.login()">로그인 후 글쓰기</button>
        </div>

        <template v-if="tab === 'board'">
        <div v-if="writeOpen" class="sp-write">
          <input v-model="title" placeholder="제목" maxlength="200" />
          <textarea v-model="content" rows="5" placeholder="스트리머에게 남길 이야기를 적어보세요"></textarea>
          <button class="sp-btn" :disabled="submitting || !title.trim()" @click="submitPost">
            {{ submitting ? '등록 중…' : '등록' }}
          </button>
        </div>

        <div v-if="!posts.length" class="sp-empty">아직 글이 없습니다. 첫 글을 남겨보세요!</div>
        <article v-for="p in posts" :key="p.id" class="sp-post">
          <div class="sp-post-head">
            <div class="sp-post-author">
              <div class="sp-post-avatar">
                <img v-if="p.authorImageUrl" :src="p.authorImageUrl" alt="" />
                <span v-else>{{ p.authorName.slice(0, 1) }}</span>
              </div>
              <div>
                <div class="sp-post-title">{{ p.title }}</div>
                <div class="sp-post-meta">{{ p.authorName }} · {{ dt(p.createdAt) }}</div>
              </div>
            </div>
            <div class="sp-post-btns">
              <button v-if="auth.isLoggedIn && auth.me?.id !== p.authorId" class="sp-report" @click="reportPost(p)">신고</button>
              <button v-if="p.deletable" class="sp-del" @click="removePost(p)">삭제</button>
            </div>
          </div>
          <p v-if="p.content" class="sp-post-content">{{ p.content }}</p>
        </article>
        </template>
      </template>
    </div>
  </section>
</template>

<style scoped>
/* 시안 다크 테마 — home-snuk.css 변수 사용 */
.sp-back { font-size: 13px; color: var(--text3); display: inline-block; margin-bottom: 14px; text-decoration: none; }

.sp-card {
  display: flex; align-items: center; gap: 22px; flex-wrap: wrap;
  background: var(--bg2); border: 1px solid var(--border); border-radius: 16px;
  padding: 26px 28px; margin-bottom: 28px;
}
.sp-avatar {
  width: 96px; height: 96px; border-radius: 50%; overflow: hidden; flex: none;
  background: linear-gradient(135deg, var(--accent), var(--accent2));
  display: flex; align-items: center; justify-content: center;
  font-size: 34px; font-weight: 700; color: #fff; border: 2px solid var(--border2);
}
.sp-avatar img { width: 100%; height: 100%; object-fit: cover; }
.sp-name-row { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.sp-name { font-size: 22px; font-weight: 800; color: var(--text); }
.sp-plat { font-size: 11px; font-weight: 700; padding: 3px 10px; border-radius: 20px; border: 1px solid; }
.sp-stats-row { display: flex; gap: 16px; font-size: 13px; color: var(--text3); margin-top: 8px; flex-wrap: wrap; }
.sp-stats-row b { color: var(--text); }
.sp-actions { display: flex; gap: 8px; margin-top: 14px; flex-wrap: wrap; }
.sp-btn {
  padding: 9px 18px; font-size: 13px; font-weight: 700; border-radius: 10px; border: none; cursor: pointer;
  background: linear-gradient(135deg, var(--accent), var(--accent2)); color: #111;
}
.sp-btn.on { background: var(--bg3); color: var(--text2); border: 1px solid var(--border2); }
.sp-btn.ghost { background: transparent; color: var(--text2); border: 1px solid var(--border); }
.sp-btn:disabled { opacity: .5; cursor: not-allowed; }

.sp-board-head { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 14px; }
.sp-board-head h3 { font-size: 17px; color: var(--text); margin: 0; }
.sp-count { font-size: 12px; color: var(--text3); font-weight: 600; margin-left: 4px; }

.sp-write {
  background: var(--bg2); border: 1px solid var(--border); border-radius: 12px;
  padding: 16px; margin-bottom: 16px; display: flex; flex-direction: column; gap: 10px;
}
.sp-write input, .sp-write textarea {
  width: 100%; padding: 10px 12px; border: 1px solid var(--border); background: var(--bg3);
  color: var(--text); border-radius: 8px; font-size: 14px; outline: none;
}
.sp-write button { align-self: flex-end; }

.sp-post { background: var(--bg2); border: 1px solid var(--border); border-radius: 12px; padding: 16px 18px; margin-bottom: 10px; }
.sp-post-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 10px; }
.sp-post-author { display: flex; align-items: center; gap: 10px; min-width: 0; }
.sp-post-avatar {
  width: 36px; height: 36px; border-radius: 50%; overflow: hidden; flex: none;
  background: var(--bg3); display: flex; align-items: center; justify-content: center;
  font-size: 14px; font-weight: 700; color: var(--text2);
}
.sp-post-avatar img { width: 100%; height: 100%; object-fit: cover; }
.sp-post-title { font-size: 14px; font-weight: 700; color: var(--text); }
.sp-post-meta { font-size: 11px; color: var(--text3); margin-top: 2px; }
.sp-post-btns { display: flex; gap: 6px; flex: none; }
.sp-del {
  flex: none; font-size: 11px; font-weight: 700; padding: 5px 11px; border-radius: 8px; cursor: pointer;
  background: rgba(239, 68, 68, .1); color: var(--live); border: 1px solid rgba(239, 68, 68, .3);
}
.sp-report {
  flex: none; font-size: 11px; font-weight: 700; padding: 5px 11px; border-radius: 8px; cursor: pointer;
  background: var(--bg3); color: var(--text3); border: 1px solid var(--border);
}
.sp-report:hover { color: var(--gold); border-color: rgba(255, 179, 0, .4); }
.sp-post-content { margin: 10px 0 0 46px; font-size: 13px; color: var(--text2); white-space: pre-wrap; line-height: 1.7; }

.sp-empty {
  border: 1px dashed var(--border2); border-radius: 12px; padding: 40px 16px;
  text-align: center; color: var(--text3); font-size: 13px;
}

/* ----- V23 탭/공지/일정/위키/도구 ----- */
.sp-tabs { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 20px; }
.sp-tab {
  padding: 8px 16px; font-size: 13px; font-weight: 600; border-radius: 20px; cursor: pointer;
  background: var(--bg2); color: var(--text3); border: 1px solid var(--border);
}
.sp-tab.on { background: linear-gradient(135deg, var(--accent), var(--accent2)); color: #111; border-color: transparent; font-weight: 700; }
.sp-row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.sp-row input { flex: 1; min-width: 120px; }
.sp-chk { display: flex; align-items: center; gap: 6px; font-size: 12.5px; color: var(--text2); }
.sp-sub { font-size: 13px; color: var(--text3); margin: 0 0 10px; font-weight: 700; }
.sp-imp {
  display: inline-block; font-size: 10px; font-weight: 800; color: #fff; background: var(--live, #ef4444);
  border-radius: 5px; padding: 2px 7px; margin-right: 7px; vertical-align: 1px;
}
.sp-hint { font-size: 11.5px; color: var(--text3); margin: 2px 0 0; }
.sp-wiki-foot { font-size: 11.5px; color: var(--text3); border-top: 1px solid var(--border); padding-top: 10px; }
.sp-cmd {
  font-size: 13px; font-weight: 700; background: var(--bg3); border: 1px solid var(--border);
  border-radius: 7px; padding: 3px 9px; color: var(--accent);
}
.sp-roulette { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 14px; }
.sp-rou-item {
  display: inline-flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 600;
  background: var(--bg2); border: 1px solid var(--border); border-radius: 20px; padding: 7px 14px; color: var(--text);
}
.sp-rou-item em { font-style: normal; font-size: 11px; color: var(--text3); }
.sp-rou-item.hit { border-color: var(--accent); background: var(--bg3); box-shadow: 0 0 0 2px var(--accent) inset; }
.sp-rou-del { border: none; background: none; color: var(--text3); cursor: pointer; font-size: 11px; padding: 0 0 0 2px; }
.sp-rou-result { font-size: 15px; color: var(--text); background: var(--bg2); border: 1px solid var(--accent); border-radius: 12px; padding: 14px 18px; margin-top: 6px; }
.sp-sched .sp-post-title { font-size: 14px; }
</style>
