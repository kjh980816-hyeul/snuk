<script setup lang="ts">
// 스눅 커뮤니티 — 게시판 트리(좌) / 목록·검색·정렬(중앙) / 인기글(우) + 상세·글쓰기·내가 쓴 글
// 게시판 구성은 어드민(/admin → 커뮤니티)에서 관리. 더미 없음 — 데이터 없으면 빈 상태.
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { communityApi, siteSettingsApi } from '@/api'
import type {
  CommunityBoard, CommunityComment, CommunityPostDetail, CommunityPostPage, CommunityPostSummary,
} from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const boards = ref<CommunityBoard[]>([])
const loading = ref(true)

// 좌측 바로가기·상단 배너 문구는 어드민(/admin → 커뮤니티)에서 설정 — 코드 수정 없이 변경
const settings = ref<Record<string, string>>({})
function setting(key: string, fallback = '') {
  const v = settings.value[key]
  return !v || v === '-' || v === 'none' ? fallback : v
}
const sideLinksTitle = computed(() => setting('COMMUNITY_LINKS_TITLE', '스눅 소식'))
const sideLinks = computed<Array<{ label: string; url: string }>>(() => {
  try {
    const parsed = JSON.parse(setting('COMMUNITY_LINKS', '[]'))
    return Array.isArray(parsed) ? parsed.filter((x) => x && x.label && x.url) : []
  } catch {
    return []
  }
})
const bannerTitle = computed(() => setting('BANNER_COMMUNITY_TITLE', '스트리머와 시청자가 함께 쓰는 스눅 라운지'))
const bannerSub = computed(() => setting('BANNER_COMMUNITY_SUB', 'SNUK COMMUNITY'))
const bannerImage = computed(() => setting('BANNER_COMMUNITY_URL', ''))

function goLink(url: string) {
  if (!url) return
  if (/^https?:\/\//i.test(url)) window.open(url, '_blank', 'noopener')
  else router.push(url)
}

// ── 목록 상태
const boardId = ref<number | null>(null)
const sort = ref<'new' | 'hot' | 'buff'>('new')
const q = ref('')
const page = ref(0)
const size = ref(20)
const pageData = ref<CommunityPostPage | null>(null)
const popular = ref<CommunityPostSummary[]>([])

// ── 화면 모드
type Mode = 'list' | 'mine' | 'write'
const mode = ref<Mode>('list')
const minePosts = ref<CommunityPostSummary[]>([])
const mineChecked = ref<Set<number>>(new Set())

// ── 상세
const detail = ref<CommunityPostDetail | null>(null)
const comments = ref<CommunityComment[]>([])
const nearby = ref<CommunityPostSummary[]>([])
const commentText = ref('')
const commentBusy = ref(false)

// ── 작성 폼
const formBoardId = ref<number | null>(null)
const formTitle = ref('')
const formContent = ref('')
const editingId = ref<number | null>(null)
const submitting = ref(false)

const detailId = computed(() => (route.params.id ? Number(route.params.id) : null))
const noticeId = computed(() => (route.params.noticeId ? Number(route.params.noticeId) : null))

/** 목록 행 클릭 — 뉴스는 기사 페이지로, 공지는 커뮤니티 안에서, 나머지는 커뮤니티 글 상세 */
function openPost(p: { id: number; source: string | null }) {
  if (p.source === 'NEWS') router.push(`/news/${p.id}`)
  else if (p.source === 'NOTICE') router.push(`/community/notice/${p.id}`)
  else router.push(`/community/${p.id}`)
}

// 본문 이미지: 이미지 주소만 있는 줄은 <img> 로 렌더(텍스트 보간이라 XSS 없음)
const IMG_LINE = /^(https?:\/\/\S+\.(png|jpe?g|gif|webp)(\?\S*)?|\/uploads\/\S+)$/i
const contentBlocks = computed(() => {
  const raw = detail.value?.content ?? ''
  return raw.split(/\n/).reduce<Array<{ type: 'img' | 'p'; text: string }>>((acc, line) => {
    const t = line.trim()
    if (IMG_LINE.test(t)) acc.push({ type: 'img', text: t })
    else if (t) {
      const last = acc[acc.length - 1]
      if (last && last.type === 'p') last.text += '\n' + line
      else acc.push({ type: 'p', text: line })
    } else {
      const last = acc[acc.length - 1]
      if (last && last.type === 'p') last.text += '\n'
    }
    return acc
  }, [])
})

// 글쓰기 폼 이미지 첨부 — 로그인 회원 누구나
const contentEl = ref<HTMLTextAreaElement | null>(null)
const imageUploading = ref(false)
async function onPickImage(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  imageUploading.value = true
  try {
    const { url } = await communityApi.uploadImage(file)
    const el = contentEl.value
    const pos = el ? el.selectionStart : formContent.value.length
    const before = formContent.value.slice(0, pos)
    const after = formContent.value.slice(pos)
    formContent.value = `${before}${before.endsWith('\n') || !before ? '' : '\n'}${url}\n${after}`
  } catch (err) {
    alert(errMsg(err, '이미지 업로드에 실패했어요. (5MB 이하 jpg·png·gif·webp)'))
  } finally {
    imageUploading.value = false
    input.value = ''
  }
}

// ── 게시판 트리
const groups = computed(() => boards.value.filter((b) => b.parentId === null))
function childrenOf(id: number) {
  return boards.value.filter((b) => b.parentId === id)
}
/** 글을 쓸 수 있는 게시판 = 하위가 없는 그룹 + 모든 하위 게시판 (트리 순서로 나열) */
const writableBoards = computed(() => {
  const out: Array<{ id: number; label: string }> = []
  for (const g of groups.value) {
    const kids = childrenOf(g.id)
    if (!kids.length) {
      if (g.canWrite) out.push({ id: g.id, label: g.name })
    } else {
      kids.filter((c) => c.canWrite).forEach((c) => out.push({ id: c.id, label: `${g.name} › ${c.name}` }))
    }
  }
  return out
})
const canWriteAnywhere = computed(() => writableBoards.value.length > 0)
const currentBoardName = computed(() => {
  if (boardId.value === null) return '전체 게시글'
  const b = boards.value.find((x) => x.id === boardId.value)
  if (!b) return '전체 게시글'
  const parent = b.parentId !== null ? boards.value.find((x) => x.id === b.parentId) : null
  return parent ? `${parent.name} · ${b.name}` : b.name
})

function md(v: string) {
  return v ? v.slice(5, 10).replace('-', '.') : ''
}
function dt(v: string) {
  return v ? v.slice(0, 16).replace('T', ' ') : ''
}

// ── 로딩
async function loadBoards() {
  boards.value = await communityApi.boards().catch(() => [])
}

async function loadList() {
  pageData.value = await communityApi
    .posts({ boardId: boardId.value, sort: sort.value, q: q.value.trim() || undefined, page: page.value, size: size.value })
    .catch(() => null)
}

async function loadPopular() {
  popular.value = await communityApi.popular(boardId.value, 10).catch(() => [])
}

async function loadMine() {
  if (!auth.isLoggedIn) { minePosts.value = []; return }
  minePosts.value = await communityApi.myPosts().catch(() => [])
  mineChecked.value = new Set()
}

async function loadDetail() {
  if (noticeId.value != null) {
    try {
      detail.value = await communityApi.noticeDetail(noticeId.value)
      comments.value = []
      nearby.value = []
    } catch {
      detail.value = null
      router.replace('/community')
    }
    return
  }
  if (detailId.value == null) { detail.value = null; return }
  try {
    detail.value = await communityApi.detail(detailId.value)
    const [c, n] = await Promise.all([
      communityApi.comments(detailId.value).catch(() => []),
      communityApi.nearby(detailId.value).catch(() => []),
    ])
    comments.value = c
    nearby.value = n
  } catch {
    detail.value = null
    router.replace('/community')
  }
}

// ── 액션
function selectBoard(id: number | null) {
  boardId.value = id
  page.value = 0
  mode.value = 'list'
  if (detailId.value != null) router.push('/community')
  loadList()
  loadPopular()
}

function applySort(s: 'new' | 'hot' | 'buff') {
  sort.value = s
  page.value = 0
  loadList()
}

function goPage(p: number) {
  page.value = p
  loadList()
  window.scrollTo(0, 0)
}

function openMine() {
  if (!auth.isLoggedIn) { alert('로그인 후 이용할 수 있어요.'); return }
  mode.value = 'mine'
  if (detailId.value != null) router.push('/community')
  loadMine()
}

function openWrite() {
  if (!auth.isLoggedIn) { alert('글쓰기는 로그인 후 이용할 수 있어요.'); return }
  editingId.value = null
  formTitle.value = ''
  formContent.value = ''
  formBoardId.value = writableBoards.value.some((b) => b.id === boardId.value)
    ? boardId.value
    : (writableBoards.value[0]?.id ?? null)
  mode.value = 'write'
  if (detailId.value != null) router.push('/community')
  window.scrollTo(0, 0)
}

function openEdit(p: CommunityPostSummary | CommunityPostDetail) {
  editingId.value = p.id
  formTitle.value = p.title
  formContent.value = 'content' in p ? (p.content ?? '') : ''
  formBoardId.value = p.boardId
  mode.value = 'write'
  if (detailId.value != null) router.push('/community')
  window.scrollTo(0, 0)
  // 목록에서 수정 진입 시 본문을 별도로 채워온다
  if (!('content' in p)) {
    communityApi.detail(p.id).then((d) => { formContent.value = d.content ?? '' }).catch(() => {})
  }
}

async function submitPost() {
  if (!formBoardId.value) { alert('게시판을 선택해주세요.'); return }
  if (!formTitle.value.trim() || submitting.value) return
  submitting.value = true
  try {
    const body = { boardId: formBoardId.value, title: formTitle.value.trim(), content: formContent.value }
    const saved = editingId.value != null
      ? await communityApi.edit(editingId.value, body)
      : await communityApi.write(body)
    mode.value = 'list'
    await Promise.all([loadList(), loadPopular()])
    router.push(`/community/${saved.id}`)
  } catch (e) {
    alert(errMsg(e, '글 저장에 실패했어요.'))
  } finally {
    submitting.value = false
  }
}

async function removePost(p: { id: number; title: string; source?: string | null }) {
  if (!confirm(`"${p.title}" 글을 삭제할까요?`)) return
  try {
    if (p.source === 'NOTICE') await communityApi.deleteNotice(p.id)
    else await communityApi.remove(p.id)
    if (detailId.value === p.id) router.push('/community')
    await Promise.all([loadList(), loadPopular(), mode.value === 'mine' ? loadMine() : Promise.resolve()])
  } catch (e) {
    alert(errMsg(e, '삭제에 실패했어요.'))
  }
}

async function removeChecked() {
  const ids = [...mineChecked.value]
  if (!ids.length) return
  if (!confirm(`선택한 ${ids.length}개 글을 삭제할까요?`)) return
  for (const id of ids) {
    await communityApi.remove(id).catch(() => {})
  }
  await Promise.all([loadMine(), loadList(), loadPopular()])
}

function toggleCheck(id: number) {
  const next = new Set(mineChecked.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  mineChecked.value = next
}

async function toggleBuff() {
  if (!auth.isLoggedIn) { alert('버프는 로그인 후 누를 수 있어요.'); return }
  if (detailId.value == null) return
  try {
    const r = await communityApi.buff(detailId.value)
    if (detail.value) {
      detail.value.buffed = r.buffed
      detail.value.buffCount = r.buffCount
    }
  } catch (e) {
    alert(errMsg(e, '버프에 실패했어요.'))
  }
}

async function submitComment() {
  if (!auth.isLoggedIn) { alert('댓글은 로그인 후 작성할 수 있어요.'); return }
  const text = commentText.value.trim()
  if (!text || commentBusy.value || detailId.value == null) return
  commentBusy.value = true
  try {
    await communityApi.addComment(detailId.value, text)
    commentText.value = ''
    comments.value = await communityApi.comments(detailId.value)
    if (detail.value) detail.value.commentCount = comments.value.length
  } catch (e) {
    alert(errMsg(e, '댓글 등록에 실패했어요.'))
  } finally {
    commentBusy.value = false
  }
}

async function removeComment(c: CommunityComment) {
  if (detailId.value == null || !confirm('댓글을 삭제할까요?')) return
  try {
    await communityApi.deleteComment(detailId.value, c.id)
    comments.value = await communityApi.comments(detailId.value)
    if (detail.value) detail.value.commentCount = comments.value.length
  } catch {
    alert('삭제에 실패했어요.')
  }
}

async function reportPost() {
  if (!auth.isLoggedIn) { alert('신고는 로그인 후 이용할 수 있어요.'); return }
  if (detailId.value == null) return
  const reason = prompt('신고 사유를 적어주세요. (운영자가 확인합니다)')
  if (reason === null) return
  try {
    await communityApi.report(detailId.value, reason.trim())
    alert('신고를 접수했습니다. 운영자가 확인 후 처리합니다.')
  } catch (e) {
    alert(errMsg(e, '신고에 실패했어요.'))
  }
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    alert('주소를 복사했습니다.')
  } catch {
    alert(window.location.href)
  }
}

function canManage(p: { authorId: number }) {
  return auth.isAdmin || auth.me?.id === p.authorId
}

function errMsg(e: unknown, fallback: string) {
  return (e as { response?: { data?: { message?: string } } })?.response?.data?.message ?? fallback
}

// ── 페이저
const pages = computed(() => {
  const total = pageData.value?.totalPages ?? 1
  const cur = pageData.value?.page ?? 0
  const start = Math.max(0, Math.min(cur - 2, total - 5))
  return Array.from({ length: Math.min(5, total) }, (_, i) => start + i).filter((p) => p >= 0 && p < total)
})

let searchTimer: ReturnType<typeof setTimeout> | null = null
watch(q, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => { page.value = 0; loadList() }, 350)
})
watch(size, () => { page.value = 0; loadList() })
watch([detailId, noticeId], loadDetail)

onMounted(async () => {
  try {
    settings.value = await siteSettingsApi.get().catch(() => ({}))
    await loadBoards()
    await Promise.all([loadList(), loadPopular(), loadDetail()])
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="com">
  <div class="inner com-inner">
    <!-- 커뮤니티 배너 (문구·링크는 어드민 설정) -->
    <div class="combanner">
      <img v-if="bannerImage" :src="bannerImage" class="cbbg" alt="" />
      <div class="cbtext">
        <span class="cb1">{{ bannerSub }}</span>
        <span class="cb2">{{ bannerTitle }}</span>
      </div>
      <span class="cbimg">💬</span>
    </div>

    <!-- 모바일 게시판 바 -->
    <div class="mobbar">
      <button :class="{ on: mode === 'list' && boardId === null }" @click="selectBoard(null)">전체</button>
      <template v-for="g in groups" :key="g.id">
        <button v-if="!childrenOf(g.id).length" :class="{ on: mode === 'list' && boardId === g.id }"
          @click="selectBoard(g.id)">{{ g.name }}</button>
        <button v-for="c in childrenOf(g.id)" :key="c.id" :class="{ on: mode === 'list' && boardId === c.id }"
          @click="selectBoard(c.id)">{{ g.name }}·{{ c.name }}</button>
      </template>
      <button :class="{ on: mode === 'mine' }" @click="openMine()">내 글</button>
    </div>

    <div class="comwrap">
      <!-- ── 좌: 게시판 트리 -->
      <aside class="comside">
        <div class="stickyin">
          <div class="card pad">
            <button v-if="canWriteAnywhere || !auth.isLoggedIn" class="btn pri wide" @click="openWrite()">글쓰기</button>
            <button class="btn wide" :class="{ on: mode === 'mine' }" @click="openMine()">✎ 내가 쓴 글</button>
            <button class="comnav" :class="{ on: mode === 'list' && boardId === null }" @click="selectBoard(null)">
              ☰ 전체 게시글
            </button>

            <template v-if="sideLinks.length">
              <p class="comgl">{{ sideLinksTitle }}</p>
              <button v-for="(l, i) in sideLinks" :key="i" class="comnav sub" @click="goLink(l.url)">
                <span class="dash">└</span>{{ l.label }}
              </button>
            </template>

            <p class="comgl">커뮤니티</p>
            <template v-for="g in groups" :key="g.id">
              <button class="comnav" :class="{ on: mode === 'list' && boardId === g.id }" @click="selectBoard(g.id)">
                {{ g.name }}
              </button>
              <button v-for="c in childrenOf(g.id)" :key="c.id" class="comnav sub"
                :class="{ on: mode === 'list' && boardId === c.id }" @click="selectBoard(c.id)">
                <span class="dash">└</span>{{ c.name }}
              </button>
            </template>
            <p v-if="!groups.length && !loading" class="side-empty">등록된 게시판이 없습니다.</p>
          </div>
        </div>
      </aside>

      <!-- ── 중앙 -->
      <div class="commid">
        <!-- 상세 -->
        <template v-if="detail">
          <button class="btn sm back" @click="router.push('/community')">← 목록으로</button>
          <article class="card pad postwrap">
            <div class="taglist">
              <span v-if="detail.groupName" class="tag">{{ detail.groupName }}</span>
              <span class="tag">{{ detail.boardName }}</span>
              <span v-if="detail.hidden" class="tag warn">숨김 처리됨</span>
            </div>
            <h2 class="postt">{{ detail.title }}</h2>
            <div class="postmeta">
              <img v-if="detail.authorImageUrl" :src="detail.authorImageUrl" class="ava" alt="" />
              <span v-else class="ava ava-empty">{{ detail.authorName.slice(0, 1) }}</span>
              <div class="postby">
                <p class="byname">{{ detail.authorName }}</p>
                <p class="bysub">{{ dt(detail.createdAt) }} · 조회 {{ detail.viewCount.toLocaleString() }} · 댓글 {{ detail.commentCount }}</p>
              </div>
              <button v-if="!detail.source" class="btn sm" :class="{ buffed: detail.buffed }" @click="toggleBuff()">
                ♥ 버프 {{ detail.buffCount }}
              </button>
              <button class="btn sm" @click="copyLink()">🔗</button>
            </div>
            <div class="postbody">
              <template v-for="(b, i) in contentBlocks" :key="i">
                <img v-if="b.type === 'img'" :src="b.text" class="body-img" alt="" loading="lazy" />
                <p v-else class="body-p">{{ b.text }}</p>
              </template>
            </div>
            <div class="postact">
              <template v-if="canManage(detail)">
                <button v-if="!detail.source" class="btn sm" @click="openEdit(detail)">수정</button>
                <button class="btn sm danger" @click="removePost(detail)">삭제</button>
              </template>
              <button v-if="!detail.source" class="btn sm ghost" @click="reportPost()">신고</button>
            </div>
          </article>

          <!-- 댓글 (공지·뉴스 등 시스템 글은 제외) -->
          <div v-if="!detail.source" class="cmtwrap">
            <p class="sec-head">댓글 {{ comments.length }}개</p>
            <div class="card pad">
              <div class="cmt-write">
                <textarea v-model="commentText" rows="2" maxlength="1000"
                  :placeholder="auth.isLoggedIn ? '댓글을 남겨보세요' : '로그인 후 댓글을 작성할 수 있어요'"
                  :disabled="!auth.isLoggedIn" @keydown.enter.exact.prevent="submitComment"></textarea>
                <button class="btn pri sm" :disabled="commentBusy || !commentText.trim() || !auth.isLoggedIn"
                  @click="submitComment()">등록</button>
              </div>
              <div class="cmt-list">
                <div v-for="c in comments" :key="c.id" class="cmt-item">
                  <img v-if="c.profileImageUrl" :src="c.profileImageUrl" class="ava sm" alt="" />
                  <span v-else class="ava sm ava-empty">{{ c.nickname.slice(0, 1) }}</span>
                  <div class="cmt-body">
                    <div class="cmt-meta">
                      <span class="cmt-nick">{{ c.nickname }}</span>
                      <span class="cmt-date">{{ dt(c.createdAt) }}</span>
                      <button v-if="auth.isAdmin || auth.me?.id === c.memberId" class="cmt-del"
                        @click="removeComment(c)">삭제</button>
                    </div>
                    <p class="cmt-text">{{ c.content }}</p>
                  </div>
                </div>
                <p v-if="!comments.length" class="empty sm">첫 댓글을 남겨보세요!</p>
              </div>
            </div>
          </div>

          <!-- 같은 게시판 다른 글 -->
          <div v-if="nearby.length" class="cmtwrap">
            <p class="sec-head">{{ detail.boardName }} 게시판의 다른 글</p>
            <div class="card ov">
              <button v-for="n in nearby" :key="n.id" class="bestrow" @click="router.push(`/community/${n.id}`)">
                <span class="bt">{{ n.title }}</span>
                <span class="cmtc">{{ n.commentCount }}</span>
              </button>
            </div>
          </div>
        </template>

        <!-- 글쓰기 -->
        <template v-else-if="mode === 'write'">
          <button class="btn sm back" @click="mode = 'list'">← 목록으로</button>
          <div class="card pad">
            <div class="wrhead">
              <span class="wrtitle">{{ editingId != null ? '글 수정' : '글쓰기' }}</span>
              <select v-model.number="formBoardId" class="inp board-sel">
                <option :value="null" disabled>게시판을 선택하세요</option>
                <option v-for="b in writableBoards" :key="b.id" :value="b.id">{{ b.label }}</option>
              </select>
            </div>
            <input v-model="formTitle" class="inp wr-title" maxlength="200" placeholder="제목을 입력해주세요" />
            <textarea ref="contentEl" v-model="formContent" class="inp wr-body" maxlength="20000"
              placeholder="함께 나누고 싶은 얘기를 남겨주세요. 사진은 아래 '사진 넣기'로 넣으면 그 자리에 들어갑니다."></textarea>
            <label class="pic-btn">
              {{ imageUploading ? '올리는 중…' : '🖼 사진 넣기' }}
              <input type="file" accept="image/*" style="display:none" @change="onPickImage" />
            </label>
            <div class="wr-foot">
              <span class="hint">서로를 존중하는 글만 남겨주세요. 신고가 접수되면 운영자가 확인합니다.</span>
              <button class="btn pri" :disabled="submitting || !formTitle.trim() || !formBoardId" @click="submitPost()">
                {{ submitting ? '저장 중…' : editingId != null ? '수정 완료' : '등록' }}
              </button>
            </div>
          </div>
        </template>

        <!-- 내가 쓴 글 -->
        <template v-else-if="mode === 'mine'">
          <div class="card pad headcard">
            <p class="headtitle">내가 쓴 글 {{ minePosts.length }}개</p>
          </div>
          <div class="card ov">
            <div class="pthead ck">
              <span class="ckcol"></span>
              <span class="pcat">게시판</span><span class="ptt">제목</span>
              <span class="pd">작성일</span><span class="pv">조회</span><span class="pb">버프</span>
              <span class="pact">관리</span>
            </div>
            <div v-for="p in minePosts" :key="p.id" class="ptrow ck" :class="{ sel: mineChecked.has(p.id) }">
              <input type="checkbox" :checked="mineChecked.has(p.id)" @change="toggleCheck(p.id)" />
              <span class="pcat">{{ p.boardName }}</span>
              <button class="ptt link" @click="router.push(`/community/${p.id}`)">
                <span class="tx">{{ p.title }}</span>
                <span v-if="p.commentCount" class="cmtc">({{ p.commentCount }})</span>
                <span v-if="p.hidden" class="tag warn mini">숨김</span>
              </button>
              <span class="pd">{{ md(p.createdAt) }}</span>
              <span class="pv">{{ p.viewCount.toLocaleString() }}</span>
              <span class="pb">{{ p.buffCount }}</span>
              <span class="pact">
                <button class="btn xs" @click="openEdit(p)">수정</button>
                <button class="btn xs danger" @click="removePost(p)">삭제</button>
              </span>
            </div>
            <p v-if="!minePosts.length" class="empty">아직 쓴 글이 없습니다.</p>
          </div>
          <div class="bulkbar">
            <span class="hint">{{ mineChecked.size ? `${mineChecked.size}개 선택됨` : '선택된 글이 없습니다' }}</span>
            <button class="btn sm danger" :disabled="!mineChecked.size" @click="removeChecked()">선택 삭제</button>
          </div>
        </template>

        <!-- 목록 -->
        <template v-else>
          <div class="midhead">
          <div class="card pad headcard">
            <p class="headtitle">{{ currentBoardName }}</p>
          </div>
          <div class="pinrow">
            <div class="sortrow">
              <button :class="{ on: sort === 'hot' }" @click="applySort('hot')">인기순</button>
              <span class="dot">·</span>
              <button :class="{ on: sort === 'new' }" @click="applySort('new')">최신순</button>
              <span class="dot">·</span>
              <button :class="{ on: sort === 'buff' }" @click="applySort('buff')">버프순</button>
            </div>
            <div class="pinright">
              <select v-model.number="size" class="inp per">
                <option :value="10">10개씩</option>
                <option :value="20">20개씩</option>
                <option :value="50">50개씩</option>
                <option :value="100">100개씩</option>
              </select>
              <div class="searchbox">
                <span class="si">🔍</span>
                <input v-model="q" class="inp" placeholder="게시글 검색" />
              </div>
              <button v-if="canWriteAnywhere || !auth.isLoggedIn" class="btn pri sm wbtn" @click="openWrite()">✎ 글쓰기</button>
            </div>
          </div>
          </div>

          <div class="card ov">
            <div class="pthead">
              <span class="pcat">게시판</span><span class="ptt">제목</span><span class="pby">작성자</span>
              <span class="pd">작성일</span><span class="pv">조회</span><span class="pb">버프</span>
            </div>
            <button v-for="p in pageData?.items ?? []" :key="p.id" class="ptrow" @click="openPost(p)">
              <span class="pcat">{{ p.boardName }}</span>
              <span class="ptt">
                <span class="tx">{{ p.title }}</span>
                <span v-if="p.commentCount" class="cmtc">({{ p.commentCount }})</span>
              </span>
              <span class="pby">{{ p.authorName }}</span>
              <span class="pd">{{ md(p.createdAt) }}</span>
              <span class="pv">{{ p.viewCount.toLocaleString() }}</span>
              <span class="pb">{{ p.buffCount }}</span>
            </button>
            <p v-if="loading" class="empty">불러오는 중…</p>
            <p v-else-if="!(pageData?.items?.length)" class="empty">
              {{ q.trim() ? '검색 결과가 없습니다.' : '아직 글이 없습니다. 첫 글을 남겨보세요!' }}
            </p>
          </div>

          <div v-if="(pageData?.totalPages ?? 1) > 1" class="pager">
            <button class="pg" :disabled="page === 0" @click="goPage(page - 1)">‹</button>
            <button v-for="p in pages" :key="p" class="pg" :class="{ on: p === page }" @click="goPage(p)">
              {{ p + 1 }}
            </button>
            <button class="pg" :disabled="page >= (pageData?.totalPages ?? 1) - 1" @click="goPage(page + 1)">›</button>
          </div>
        </template>
        <p class="comfoot">© 2026 SNUK. All rights reserved. · 문의: contact@snuk.kr</p>
      </div>

      <!-- ── 우: 인기글 -->
      <aside class="comright">
        <div class="stickyin"><!-- (데스크톱은 컬럼 자체가 고정, ≤960 은 기존 흐름) -->
          <div class="card pad">
            <p class="side-title">이 라운지 인기글</p>
            <button v-for="p in popular" :key="p.id" class="hotpost" @click="openPost(p)">
              <span class="hp">[{{ p.boardName }}]</span>
              <span class="hpt">{{ p.title }}</span>
              <span v-if="p.commentCount" class="cmtc">({{ p.commentCount }})</span>
            </button>
            <p v-if="!popular.length" class="empty sm">아직 인기글이 없습니다.</p>
          </div>
        </div>
      </aside>
    </div>
  </div>
  </section>
</template>

<style scoped>
.com { padding: 30px 0 60px; }
/* 다른 페이지와 같은 .inner(가운데 정렬) — 3단이라 넓은 화면에서만 조금 더 넓게 */
@media (min-width: 1560px) { .com-inner { max-width: 1480px; } }

/* ── 배너 */
.combanner { display: flex; align-items: center; gap: 16px; width: 100%; text-align: left;
  background: linear-gradient(100deg, #2E1065, #5B21B6 55%, #7C3AED); color: #fff;
  border-radius: 18px; padding: 18px 24px; margin-bottom: 16px; }
.combanner { position: relative; overflow: hidden; }
.cbbg { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; opacity: .45; }
.cbtext { position: relative; z-index: 1; }
.pic-btn { display: inline-block; margin-top: 10px; padding: 8px 14px; border-radius: 999px;
  border: 1px dashed var(--border2, #444); font-size: 12.5px; color: var(--text2, #ADA89E); cursor: pointer; }
.body-p { white-space: pre-wrap; margin: 0 0 16px; }
.body-img { display: block; max-width: 100%; margin: 18px auto; border-radius: 12px; }
.cb1 { display: block; font-size: 13px; font-weight: 700; opacity: .85; }
.cb2 { display: block; font-size: 20px; font-weight: 800; letter-spacing: -.03em; margin-top: 2px; }
.cb2 b { color: #C4B5FD; }
.cbimg { margin-left: auto; font-size: 30px; opacity: .85; flex-shrink: 0; }

/* ── 3단 레이아웃 */
.comwrap { display: grid; grid-template-columns: 230px minmax(0, 1fr) 300px; gap: 22px; }
.comside, .comright, .commid { min-width: 0; }
.stickyin { position: sticky; top: 16px; align-self: start; max-height: calc(100vh - 32px);
  overflow-y: auto; scrollbar-width: none; }
.stickyin::-webkit-scrollbar { display: none; }

.card { background: var(--bg2, #1B1A18); border: 1px solid var(--border, #2E2C29); border-radius: 14px; }
.card.pad { padding: 16px; }
.card.ov { overflow: hidden; }

/* ── 좌 사이드 */
.comside .btn.wide { width: 100%; margin-bottom: 10px; justify-content: center; }
.comnav { display: flex; align-items: center; gap: 7px; width: 100%; text-align: left; border: none;
  background: none; color: var(--text2, #ADA89E); font-size: 13px; font-weight: 600; padding: 8px 10px;
  border-radius: 8px; cursor: pointer; font-family: 'Pretendard', 'Noto Sans KR', sans-serif; }
.comnav:hover { background: var(--bg3, #232220); color: var(--text, #EDEAE3); }
.comnav.on { background: var(--accent-tint, #241C4A); color: var(--accent3, #C9B8FF); font-weight: 700; }
.comnav.sub { padding-left: 8px; font-size: 12.5px; }
.dash { color: var(--text3, #8A857C); font-size: 11px; }
.comgl { font-size: 12px; font-weight: 700; color: var(--text3, #8A857C); margin: 14px 0 6px; padding-left: 4px; }
.side-empty, .side-title { font-size: 13px; color: var(--text3, #8A857C); }
.side-title { font-weight: 700; font-size: 14px; color: var(--text, #EDEAE3); margin-bottom: 10px; }

/* ── 버튼 */
.btn { display: inline-flex; align-items: center; justify-content: center; gap: 6px; border-radius: 999px;
  border: 1px solid var(--border2, #403D38); background: var(--bg3, #232220); color: var(--text2, #ADA89E);
  font-size: 13px; font-weight: 700; padding: 9px 16px; cursor: pointer;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif; white-space: nowrap; }
.btn:hover { color: var(--text, #EDEAE3); border-color: var(--accent, #8163FF); }
.btn.pri { background: var(--accent, #8163FF); border-color: transparent; color: #fff; }
.btn.pri:hover { filter: brightness(1.08); color: #fff; }
.btn.on { border-color: var(--accent, #8163FF); color: var(--accent3, #C9B8FF); }
.btn.sm { padding: 7px 13px; font-size: 12.5px; }
.btn.xs { padding: 5px 10px; font-size: 11.5px; }
.btn.danger { color: #E9635F; border-color: rgba(233, 99, 95, .4); }
.btn.ghost { background: none; }
.btn.buffed { background: var(--accent-tint, #241C4A); border-color: var(--accent, #8163FF); color: var(--accent3, #C9B8FF); }
.btn:disabled { opacity: .45; cursor: default; }
.back { margin-bottom: 14px; }

/* ── 중앙: 헤더·정렬·검색 — 스크롤해도 상단에 고정, 게시글만 밑으로 흐른다 */
.midhead { position: sticky; top: 0; z-index: 30; background: var(--bg, #131211);
  padding-top: 14px; margin-top: -14px; }
.headcard { margin-bottom: 14px; }
.headtitle { font-size: 17px; font-weight: 700; letter-spacing: -.02em; color: var(--text, #EDEAE3); }
.pinrow { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.sortrow { display: flex; align-items: center; gap: 6px; }
.sortrow button { border: none; background: none; color: var(--text3, #8A857C); font-size: 13px; font-weight: 600;
  cursor: pointer; font-family: 'Pretendard', 'Noto Sans KR', sans-serif; padding: 2px; }
.sortrow button.on { color: var(--accent, #8163FF); font-weight: 800; }
.dot { color: var(--border2, #403D38); }
.pinright { display: flex; align-items: center; gap: 8px; }
.inp { background: var(--bg3, #232220); border: 1px solid var(--border, #2E2C29); border-radius: 9px;
  padding: 9px 12px; color: var(--text, #EDEAE3); font-size: 13px; outline: none;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif; }
.inp:focus { border-color: var(--accent, #8163FF); }
.per { width: 96px; padding: 8px; font-size: 12.5px; }
.searchbox { display: flex; align-items: center; gap: 6px; background: var(--bg3, #232220);
  border: 1px solid var(--border, #2E2C29); border-radius: 999px; padding: 0 12px; }
.searchbox .inp { border: none; background: none; padding: 8px 0; width: 150px; }
.si { font-size: 12px; color: var(--text3, #8A857C); }

/* ── 목록 테이블 */
.pthead, .ptrow { display: grid; grid-template-columns: 96px minmax(0, 1fr) 90px 62px 60px 54px;
  align-items: center; gap: 10px; padding: 11px 16px; text-align: left; width: 100%; }
.pthead { background: var(--bg3, #232220); border-bottom: 1px solid var(--border, #2E2C29);
  font-size: 12px; font-weight: 700; color: var(--text3, #8A857C); }
.ptrow { border: none; border-bottom: 1px solid var(--border, #2E2C29); background: none;
  color: var(--text, #EDEAE3); cursor: pointer; font-family: 'Pretendard', 'Noto Sans KR', sans-serif; }
.ptrow:last-child { border-bottom: none; }
.ptrow:hover { background: var(--bg3, #232220); }
.pcat { font-size: 12px; color: var(--text3, #8A857C); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ptt { display: flex; align-items: center; gap: 6px; min-width: 0; font-size: 13.5px; font-weight: 600; }
.ptt .tx { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ptt.link { border: none; background: none; color: var(--text, #EDEAE3); cursor: pointer; text-align: left;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif; }
.cmtc { color: #E9635F; font-weight: 700; font-size: 12px; flex-shrink: 0; }
.pby, .pd, .pv, .pb { font-size: 12px; color: var(--text3, #8A857C); overflow: hidden;
  text-overflow: ellipsis; white-space: nowrap; }
.pv, .pb { text-align: right; font-variant-numeric: tabular-nums; }
.pthead.ck, .ptrow.ck { grid-template-columns: 22px 96px minmax(0, 1fr) 90px 62px 54px 108px; }
.ptrow.ck { cursor: default; }
.ptrow.ck.sel { background: var(--accent-tint, #241C4A); }
.pact { display: flex; gap: 5px; justify-content: flex-end; }
.bulkbar { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 12px; }
.empty { padding: 40px 16px; text-align: center; color: var(--text3, #8A857C); font-size: 13px; }
.empty.sm { padding: 16px 0; font-size: 12.5px; }
.hint { font-size: 12px; color: var(--text3, #8A857C); }

/* ── 페이저 */
.pager { display: flex; justify-content: center; gap: 6px; margin-top: 18px; }
.pg { min-width: 34px; height: 34px; border-radius: 8px; border: 1px solid var(--border, #2E2C29);
  background: var(--bg2, #1B1A18); color: var(--text2, #ADA89E); font-size: 13px; cursor: pointer;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif; }
.pg.on { background: var(--accent, #8163FF); border-color: transparent; color: #fff; font-weight: 700; }
.pg:disabled { opacity: .35; cursor: default; }

/* ── 상세 */
.postwrap { padding: 24px; }
.taglist { display: flex; gap: 6px; margin-bottom: 10px; }
.tag { font-size: 11.5px; font-weight: 700; border-radius: 999px; padding: 4px 10px;
  background: var(--bg3, #232220); color: var(--text2, #ADA89E); }
.tag.warn { background: rgba(233, 99, 95, .15); color: #E9635F; }
.tag.mini { padding: 2px 7px; font-size: 10.5px; }
.postt { font-size: 22px; font-weight: 800; letter-spacing: -.03em; color: var(--text, #EDEAE3); line-height: 1.4; }
.postmeta { display: flex; align-items: center; gap: 10px; margin: 14px 0 18px; padding-bottom: 16px;
  border-bottom: 1px solid var(--border, #2E2C29); }
.ava { width: 38px; height: 38px; border-radius: 50%; object-fit: cover; flex-shrink: 0; }
.ava.sm { width: 32px; height: 32px; }
.ava-empty { display: flex; align-items: center; justify-content: center; font-weight: 800; font-size: 14px;
  background: linear-gradient(135deg, var(--accent, #8163FF), var(--accent2, #9880FF)); color: #fff; }
.postby { flex: 1; min-width: 0; }
.byname { font-weight: 700; font-size: 13.5px; color: var(--text, #EDEAE3); }
.bysub { font-size: 11.5px; color: var(--text3, #8A857C); margin-top: 2px; }
.postbody { font-size: 14.5px; line-height: 1.9; color: var(--text, #EDEAE3); white-space: pre-wrap; word-break: break-word; }
.postact { display: flex; gap: 6px; margin-top: 22px; padding-top: 16px; border-top: 1px solid var(--border, #2E2C29); }
.postact .ghost { margin-left: auto; }

/* ── 댓글 */
.cmtwrap { margin-top: 22px; }
.sec-head { font-size: 15px; font-weight: 800; color: var(--text, #EDEAE3); margin-bottom: 12px; }
.cmt-write { display: flex; gap: 8px; align-items: flex-end; }
.cmt-write textarea { flex: 1; background: var(--bg3, #232220); border: 1px solid var(--border, #2E2C29);
  border-radius: 10px; padding: 10px 12px; color: var(--text, #EDEAE3); font-size: 13px; outline: none;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif; resize: vertical; }
.cmt-list { display: flex; flex-direction: column; gap: 14px; margin-top: 16px; }
.cmt-item { display: flex; gap: 10px; align-items: flex-start; }
.cmt-body { flex: 1; min-width: 0; }
.cmt-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 3px; }
.cmt-nick { font-size: 12.5px; font-weight: 700; color: var(--text2, #ADA89E); }
.cmt-date { font-size: 11px; color: var(--text3, #8A857C); }
.cmt-del { margin-left: auto; background: none; border: none; font-size: 11px; color: var(--text3, #8A857C);
  cursor: pointer; font-family: 'Pretendard', sans-serif; }
.cmt-del:hover { color: #E9635F; }
.cmt-text { font-size: 13.5px; color: var(--text, #EDEAE3); line-height: 1.7; white-space: pre-wrap; word-break: break-word; }

/* ── 같은 게시판 다른 글 */
.bestrow { display: flex; align-items: center; gap: 11px; border: none; background: none; text-align: left;
  padding: 12px 16px; color: var(--text, #EDEAE3); border-bottom: 1px solid var(--border, #2E2C29);
  width: 100%; cursor: pointer; font-family: 'Pretendard', 'Noto Sans KR', sans-serif; }
.bestrow:last-child { border-bottom: none; }
.bestrow:hover { background: var(--bg3, #232220); }
.bestrow .bt { font-size: 13.5px; font-weight: 600; overflow: hidden; text-overflow: ellipsis;
  white-space: nowrap; min-width: 0; flex: 1; }

/* ── 우 사이드 인기글 */
.hotpost { display: flex; align-items: center; gap: 6px; width: 100%; text-align: left; border: none;
  background: none; color: var(--text, #EDEAE3); font-size: 12.5px; padding: 8px 4px; cursor: pointer;
  border-bottom: 1px solid var(--border, #2E2C29); font-family: 'Pretendard', 'Noto Sans KR', sans-serif; }
.hotpost:last-child { border-bottom: none; }
.hotpost:hover { color: var(--accent3, #C9B8FF); }
.hp { color: var(--text3, #8A857C); flex-shrink: 0; font-size: 11.5px; }
.hpt { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; min-width: 0; flex: 1; }

/* ── 글쓰기 */
.wrhead { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; flex-wrap: wrap; }
.wrtitle { font-size: 19px; font-weight: 800; letter-spacing: -.03em; color: var(--text, #EDEAE3); }
.board-sel { max-width: 240px; margin-left: auto; }
.wr-title { width: 100%; font-size: 15px; font-weight: 700; margin-bottom: 10px; }
.wr-body { width: 100%; min-height: 320px; resize: vertical; line-height: 1.8; }
.wr-foot { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 14px; flex-wrap: wrap; }

/* ── 모바일 게시판 바 */
.mobbar { display: none; gap: 6px; overflow-x: auto; margin-bottom: 12px; scrollbar-width: none; }
.mobbar::-webkit-scrollbar { display: none; }
.mobbar button { flex-shrink: 0; white-space: nowrap; border: 1.5px solid var(--border2, #403D38);
  background: var(--bg2, #1B1A18); color: var(--text2, #ADA89E); border-radius: 999px; padding: 8px 15px;
  font-size: 13px; font-weight: 700; cursor: pointer; font-family: 'Pretendard', 'Noto Sans KR', sans-serif; }
.mobbar button.on { background: var(--accent, #8163FF); border-color: transparent; color: #fff; }
.wbtn { display: none; }

@media (max-width: 1500px) {
  .comwrap { grid-template-columns: 198px minmax(0, 1fr) 256px; gap: 16px; }
}
@media (max-width: 1320px) {
  .comwrap { grid-template-columns: 190px minmax(0, 1fr); gap: 16px; }
  .comright { display: none; }
}
/* ≤1024: 모바일 상단바(고정 54px) 아래로 sticky 기준선 내림 */
@media (max-width: 1024px) {
  .stickyin { top: 70px; max-height: calc(100vh - 86px); }
  .midhead { top: 54px; }
}
@media (max-width: 960px) {
  .comwrap { grid-template-columns: 1fr; }
  .comside { display: none; }
  /* 모바일: 게시판 칩바만 상단바 아래 고정 — 목록 헤더까지 고정하면 화면을 너무 먹는다 */
  .mobbar { display: flex; position: sticky; top: 54px; z-index: 40;
    background: var(--bg, #131211); padding: 6px 0 8px; margin: -6px 0 12px; }
  .midhead { position: static; padding-top: 0; margin-top: 0; }
  .wbtn { display: inline-flex; }
  .pthead, .ptrow { grid-template-columns: 78px minmax(0, 1fr) 56px; }
  .pthead .pby, .ptrow .pby, .pthead .pv, .ptrow .pv, .pthead .pd, .ptrow .pd { display: none; }
  .pthead.ck, .ptrow.ck { grid-template-columns: 22px minmax(0, 1fr) 104px; }
  .pthead.ck .pcat, .ptrow.ck .pcat, .pthead.ck .pb, .ptrow.ck .pb { display: none; }
  .searchbox .inp { width: 110px; }
  .cb2 { font-size: 16px; }
}

/* ── 데스크톱(>960): 고정 프레임 — 페이지 자체는 스크롤하지 않고, 좌 게시판 트리·우 인기글·중앙 헤더는
   제자리, 게시글 목록(.commid)만 자체 스크롤. (sticky 방식은 글이 적어 중앙이 트리보다 짧으면 고정될 여지가
   없어 트리가 같이 움직였음 — 2026-08-22 뮤마랭 실측 지적) */
@media (min-width: 961px) {
  .com { height: 100vh; height: 100dvh; padding: 16px 0 0; display: flex; flex-direction: column; overflow: hidden; box-sizing: border-box; }
  .com-inner { display: flex; flex-direction: column; flex: 1; min-height: 0; width: 100%; }
  .combanner { flex-shrink: 0; }
  .comwrap { flex: 1; min-height: 0; grid-template-rows: minmax(0, 1fr); align-items: stretch; }
  .comside, .comright, .commid { min-height: 0; overflow-y: auto; scrollbar-width: none; }
  .comside::-webkit-scrollbar, .comright::-webkit-scrollbar, .commid::-webkit-scrollbar { display: none; }
  .stickyin { position: static; max-height: none; overflow: visible; }
  .commid { padding-bottom: 24px; }
  .midhead { top: 0; }
  .comfoot { display: block; }
}
.comfoot { display: none; margin-top: 28px; padding: 14px 0 6px; border-top: 1px solid var(--border, #2E2C29);
  font-size: 11.5px; color: var(--text3, #8A857C); text-align: center; }
</style>
<style>
/* 커뮤니티(데스크톱)는 고정 프레임이라 전역 푸터를 감추고 중앙 컬럼 끝에 축약 푸터(.comfoot)를 둔다.
   ⚠️.snuk-bottom 전체를 숨기면 안 됨 — 로그인/회원가입/마이페이지/신청 등 셸 모달이 전부 그 안에 있어
   버튼을 눌러도 모달이 안 뜬다(2026-08-25). 푸터만 골라 숨긴다. */
@media (min-width: 961px) {
  .snuk-page:has(.com-inner) .snuk-bottom > footer { display: none; }
}
</style>
