<script setup lang="ts">
// 스눅 뉴스 매거진(항목 11/16) — 목록·상세·작성(REPORTER/ADMIN) + 본문 이미지 + 댓글
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { newsApi } from '@/api'
import type { News, NewsComment } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const list = ref<News[]>([])
const loading = ref(true)
const detail = ref<News | null>(null)

const writeOpen = ref(false)
const editingId = ref<number | null>(null)
const title = ref('')
const content = ref('')
const thumbnailUrl = ref<string | null>(null)
const submitting = ref(false)
const uploading = ref(false)
const bodyUploading = ref(false)
const contentEl = ref<HTMLTextAreaElement | null>(null)

const detailId = computed(() => {
  const raw = route.params.id
  return raw ? Number(raw) : null
})

function dt(v: string) {
  return v?.slice(0, 10).split('-').join('.')
}

// ── 본문 렌더링: 이미지 URL 단독 줄은 <img>로, 나머지는 문단으로 (텍스트 보간이라 XSS 없음)
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

async function reload() {
  list.value = await newsApi.list()
}

// ── 댓글
const comments = ref<NewsComment[]>([])
const commentText = ref('')
const commentBusy = ref(false)

async function loadComments() {
  if (detailId.value == null) { comments.value = []; return }
  comments.value = await newsApi.comments(detailId.value).catch(() => [])
}

async function submitComment() {
  if (!auth.me) { alert('댓글은 로그인 후 작성할 수 있어요.'); return }
  const text = commentText.value.trim()
  if (!text || commentBusy.value || detailId.value == null) return
  commentBusy.value = true
  try {
    await newsApi.addComment(detailId.value, text)
    commentText.value = ''
    await loadComments()
  } catch {
    alert('댓글 등록에 실패했어요.')
  } finally {
    commentBusy.value = false
  }
}

async function removeComment(c: NewsComment) {
  if (detailId.value == null) return
  if (!confirm('댓글을 삭제할까요?')) return
  try {
    await newsApi.deleteComment(detailId.value, c.id)
    await loadComments()
  } catch {
    alert('삭제에 실패했어요.')
  }
}

function canDeleteComment(c: NewsComment) {
  return auth.isAdmin || auth.me?.id === c.memberId
}

async function loadDetail() {
  if (detailId.value == null) {
    detail.value = null
    comments.value = []
    return
  }
  try {
    detail.value = await newsApi.detail(detailId.value)
    await loadComments()
  } catch {
    detail.value = null
    router.replace('/news')
  }
}

function openWrite() {
  editingId.value = null
  title.value = ''
  content.value = ''
  thumbnailUrl.value = null
  writeOpen.value = true
}

function openEdit(n: News) {
  editingId.value = n.id
  title.value = n.title
  content.value = n.content ?? ''
  thumbnailUrl.value = n.thumbnailUrl
  writeOpen.value = true
}

async function onThumbFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  uploading.value = true
  try {
    const { url } = await newsApi.uploadImage(file)
    thumbnailUrl.value = url
  } catch {
    alert('썸네일 업로드에 실패했어요.')
  } finally {
    uploading.value = false
  }
}

// 본문 이미지: 업로드 후 커서 위치(없으면 끝)에 이미지 URL 한 줄 삽입 → 상세에서 그 자리에 이미지로 노출
async function onBodyImageFile(e: Event) {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  bodyUploading.value = true
  try {
    const { url } = await newsApi.uploadImage(file)
    const el = contentEl.value
    const pos = el ? el.selectionStart : content.value.length
    const before = content.value.slice(0, pos)
    const after = content.value.slice(pos)
    content.value = `${before}${before.endsWith('\n') || !before ? '' : '\n'}${url}\n${after}`
  } catch {
    alert('이미지 업로드에 실패했어요.')
  } finally {
    bodyUploading.value = false
    input.value = ''
  }
}

async function submit() {
  if (!title.value.trim() || submitting.value) return
  submitting.value = true
  try {
    const body = { title: title.value.trim(), content: content.value.trim(), thumbnailUrl: thumbnailUrl.value }
    if (editingId.value != null) {
      const updated = await newsApi.edit(editingId.value, body)
      if (detail.value?.id === updated.id) detail.value = updated
    } else {
      await newsApi.write(body)
    }
    writeOpen.value = false
    await reload()
  } catch (e) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    alert(msg ?? '기사 저장에 실패했어요. (기자 등급 이상만 작성 가능)')
  } finally {
    submitting.value = false
  }
}

async function remove(n: News) {
  if (!confirm(`"${n.title}" 기사를 삭제할까요?`)) return
  try {
    await newsApi.remove(n.id)
    if (detail.value?.id === n.id) router.replace('/news')
    await reload()
  } catch {
    alert('삭제에 실패했어요.')
  }
}

function canManage(n: News) {
  return auth.isAdmin || (auth.isReporter && auth.me?.id === n.authorId)
}

watch(detailId, loadDetail)

onMounted(async () => {
  try {
    await Promise.all([reload(), loadDetail()])
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section>
    <div class="inner">
      <!-- 상세 — 기사형 레이아웃(중앙 정렬) -->
      <template v-if="detail">
        <article class="news-detail">
          <RouterLink to="/news" class="back">&lt; 뉴스 목록</RouterLink>
          <h1 class="news-detail-title">{{ detail.title }}</h1>
          <div class="news-detail-meta">
            <img v-if="detail.authorImageUrl" :src="detail.authorImageUrl" class="author-pic" alt="" />
            <span class="author-name">{{ detail.authorName }} 기자</span>
            <span class="dot">·</span>
            <span>{{ dt(detail.createdAt) }}</span>
          </div>
          <div v-if="canManage(detail)" class="manage center">
            <button class="mini" @click="openEdit(detail)">수정</button>
            <button class="mini danger" @click="remove(detail)">삭제</button>
          </div>
          <img v-if="detail.thumbnailUrl" :src="detail.thumbnailUrl" class="news-detail-thumb" alt="" />

          <div class="news-detail-content">
            <template v-for="(b, i) in contentBlocks" :key="i">
              <img v-if="b.type === 'img'" :src="b.text" class="body-img" alt="" loading="lazy" />
              <p v-else class="body-p">{{ b.text }}</p>
            </template>
          </div>

          <!-- 댓글 -->
          <div class="cmt">
            <div class="cmt-head">댓글 <b>{{ comments.length }}</b></div>
            <div class="cmt-list">
              <div v-for="c in comments" :key="c.id" class="cmt-item">
                <img v-if="c.profileImageUrl" :src="c.profileImageUrl" class="cmt-pic" alt="" />
                <div v-else class="cmt-pic cmt-pic-empty">👤</div>
                <div class="cmt-body">
                  <div class="cmt-meta">
                    <span class="cmt-nick">{{ c.nickname }}</span>
                    <span class="cmt-date">{{ c.createdAt?.slice(0, 16).replace('T', ' ') }}</span>
                    <button v-if="canDeleteComment(c)" class="cmt-del" @click="removeComment(c)">삭제</button>
                  </div>
                  <div class="cmt-text">{{ c.content }}</div>
                </div>
              </div>
              <div v-if="!comments.length" class="cmt-empty">첫 댓글을 남겨보세요!</div>
            </div>
            <div class="cmt-write">
              <textarea v-model="commentText" rows="2" maxlength="1000"
                :placeholder="auth.me ? '댓글을 입력하세요.' : '로그인 후 댓글을 작성할 수 있어요.'"
                :disabled="!auth.me" @keydown.enter.exact.prevent="submitComment"></textarea>
              <button class="cmt-submit" :disabled="commentBusy || !commentText.trim() || !auth.me" @click="submitComment">등록</button>
            </div>
          </div>
        </article>
      </template>

      <!-- 목록 -->
      <template v-else>
        <div class="section-header">
          <h2 class="section-title">스눅 뉴스</h2>
          <button v-if="auth.isReporter" class="write-btn" @click="writeOpen ? (writeOpen = false) : openWrite()">
            {{ writeOpen ? '작성 취소' : '✏️ 기사 쓰기' }}
          </button>
        </div>
        <p class="sub">SNUK 기자단이 전하는 최신 소식</p>

        <div v-if="loading" class="empty">불러오는 중…</div>
        <div v-else-if="!list.length" class="empty">아직 등록된 기사가 없습니다.</div>
        <template v-else>
          <!-- 헤드라인: 첫 기사만 크게 (항목 4) -->
          <div class="headline" @click="router.push(`/news/${list[0].id}`)">
            <div class="headline-media">
              <img v-if="list[0].thumbnailUrl" :src="list[0].thumbnailUrl" alt="" />
              <span v-else class="headline-emoji">📰</span>
              <span class="headline-badge">HEADLINE</span>
            </div>
            <div class="headline-body">
              <div class="headline-title">{{ list[0].title }}</div>
              <div class="headline-excerpt">{{ (list[0].content ?? '').slice(0, 200) }}</div>
              <div class="headline-meta">
                <span>{{ list[0].authorName }} 기자 · {{ dt(list[0].createdAt) }}</span>
                <span v-if="canManage(list[0])" class="manage" @click.stop>
                  <button class="mini" @click="openEdit(list[0])">수정</button>
                  <button class="mini danger" @click="remove(list[0])">삭제</button>
                </span>
              </div>
            </div>
          </div>

          <!-- 나머지: 게시판 리스트 -->
          <div class="list">
            <div v-for="n in list.slice(1)" :key="n.id" class="row" @click="router.push(`/news/${n.id}`)">
              <div class="row-thumb">
                <img v-if="n.thumbnailUrl" :src="n.thumbnailUrl" alt="" />
                <span v-else>📰</span>
              </div>
              <div class="row-body">
                <div class="row-title">{{ n.title }}</div>
                <div class="row-excerpt">{{ (n.content ?? '').slice(0, 120) }}</div>
                <div class="row-meta">{{ n.authorName }} 기자 · {{ dt(n.createdAt) }}</div>
              </div>
              <div v-if="canManage(n)" class="manage" @click.stop>
                <button class="mini" @click="openEdit(n)">수정</button>
                <button class="mini danger" @click="remove(n)">삭제</button>
              </div>
            </div>
          </div>
        </template>
      </template>

      <!-- 작성/수정 폼 — 제목/본문/이미지 구분 -->
      <div v-if="writeOpen" class="write-card">
        <div class="write-head">{{ editingId != null ? '기사 수정' : '새 기사 작성' }}</div>

        <label class="write-label">제목</label>
        <input v-model="title" class="write-title" placeholder="기사 제목을 입력하세요" maxlength="200" />

        <label class="write-label">본문</label>
        <textarea ref="contentEl" v-model="content" rows="14"
          placeholder="기사 내용을 입력하세요. '본문 이미지 추가'를 누르면 커서 위치에 이미지가 들어갑니다."></textarea>
        <div class="body-img-row">
          <label class="upload-label">
            {{ bodyUploading ? '업로드 중…' : '🖼 본문 이미지 추가' }}
            <input type="file" accept="image/*" style="display:none" @change="onBodyImageFile" />
          </label>
          <span class="hint-inline">본문 사이사이에 이미지를 넣을 수 있어요 (여러 장 가능)</span>
        </div>

        <label class="write-label">썸네일 <span class="hint-inline">— 목록·헤드라인 카드에 노출되는 대표 이미지</span></label>
        <div class="thumb-row">
          <label class="upload-label">
            {{ uploading ? '업로드 중…' : '썸네일 업로드' }}
            <input type="file" accept="image/*" style="display:none" @change="onThumbFile" />
          </label>
          <img v-if="thumbnailUrl" :src="thumbnailUrl" class="thumb-preview" alt="" />
          <button v-if="thumbnailUrl" class="mini" @click="thumbnailUrl = null">썸네일 제거</button>
        </div>

        <button class="submit" :disabled="submitting || !title.trim()" @click="submit">
          {{ submitting ? '저장 중…' : editingId != null ? '수정 완료' : '기사 등록' }}
        </button>
      </div>
    </div>
  </section>
</template>

<style scoped>
section { padding: 40px 0 60px; }
.back { display: inline-block; font-size: 13px; color: var(--text2, #a0a0a8); text-decoration: none; margin-bottom: 20px; }
.sub { font-size: 13px; color: var(--text3, #6c6c74); margin: -14px 0 24px; }
.write-btn { margin-left: auto; padding: 8px 16px; border-radius: 9px; border: none; cursor: pointer;
  background: linear-gradient(135deg, #5b7cfa, #9b5cff); color: #fff; font-size: 13px; font-weight: 700;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif; }
.empty { border: 1px dashed var(--border2, #333); border-radius: 12px; padding: 40px 16px; text-align: center;
  color: var(--text3, #6c6c74); font-size: 13px; }
/* 헤드라인 카드 — 첫 기사 크게 (항목 4) */
.headline { display: grid; grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr); gap: 0;
  background: var(--bg2, #141418); border: 1px solid var(--border, #222); border-radius: 18px;
  overflow: hidden; cursor: pointer; margin-bottom: 18px; transition: all .2s; }
.headline:hover { border-color: #5b7cfa; box-shadow: 0 14px 44px rgba(91, 124, 250, .18); }
.headline-media { position: relative; aspect-ratio: 16/9; background: var(--bg3, #1c1c22);
  display: flex; align-items: center; justify-content: center; }
.headline-media img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.headline-emoji { font-size: 54px; opacity: .4; }
.headline-badge { position: absolute; top: 14px; left: 14px; font-size: 10px; font-weight: 800;
  letter-spacing: 1.5px; color: #fff; background: linear-gradient(135deg, #5b7cfa, #9b5cff);
  border-radius: 6px; padding: 4px 10px; }
.headline-body { padding: 26px 28px; display: flex; flex-direction: column; justify-content: center; min-width: 0; }
.headline-title { font-size: 22px; font-weight: 800; color: var(--text, #eee); line-height: 1.4; margin-bottom: 12px;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.headline-excerpt { font-size: 13.5px; color: var(--text3, #8a8a92); line-height: 1.7;
  display: -webkit-box; -webkit-line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; }
.headline-meta { display: flex; align-items: center; gap: 10px; font-size: 12px; color: var(--text3, #6c6c74); margin-top: 14px; }
@media (max-width: 800px) { .headline { grid-template-columns: 1fr; } .headline-body { padding: 16px 18px; } .headline-title { font-size: 17px; } }

.list { display: flex; flex-direction: column; gap: 12px; }
.row { display: flex; gap: 16px; align-items: center; background: var(--bg2, #141418); border: 1px solid var(--border, #222);
  border-radius: 14px; padding: 14px; cursor: pointer; transition: all .2s; }
.row:hover { border-color: #5b7cfa; transform: translateY(-1px); }
.row-thumb { width: 150px; aspect-ratio: 16/9; border-radius: 10px; overflow: hidden; flex-shrink: 0;
  background: var(--bg3, #1c1c22); display: flex; align-items: center; justify-content: center; font-size: 26px; }
.row-thumb img { width: 100%; height: 100%; object-fit: cover; }
.row-body { flex: 1; min-width: 0; }
.row-title { font-size: 15px; font-weight: 700; color: var(--text, #eee); margin-bottom: 5px; }
.row-excerpt { font-size: 12.5px; color: var(--text3, #6c6c74); line-height: 1.5; overflow: hidden;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; }
.row-meta { font-size: 11px; color: var(--text3, #6c6c74); margin-top: 8px; }
.manage { display: flex; gap: 6px; margin-left: auto; flex-shrink: 0; }
.manage.center { margin: 0 auto 18px; justify-content: center; }
.mini { padding: 5px 10px; border-radius: 7px; font-size: 11px; cursor: pointer; border: 1px solid var(--border, #333);
  background: var(--bg3, #1c1c22); color: var(--text2, #a0a0a8); font-family: 'Pretendard', sans-serif; }
.mini.danger { color: var(--live); border-color: rgba(255, 80, 80, .35); }

/* ── 상세: 기사형 중앙 정렬 레이아웃 ── */
.news-detail { max-width: 760px; margin: 0 auto; }
.news-detail-title { font-size: clamp(24px, 3.4vw, 32px); font-weight: 800; color: var(--text, #eee);
  line-height: 1.35; letter-spacing: -0.5px; margin-bottom: 14px; text-align: center; }
.news-detail-meta { display: flex; align-items: center; justify-content: center; gap: 8px;
  font-size: 13px; color: var(--text3, #6c6c74);
  padding-bottom: 20px; border-bottom: 1px solid var(--border, #222); margin-bottom: 26px; }
.author-pic { width: 26px; height: 26px; border-radius: 50%; object-fit: cover; }
.author-name { font-weight: 700; color: var(--text2, #a0a0a8); }
.news-detail-thumb { display: block; width: 100%; border-radius: 14px; margin-bottom: 26px; }
.news-detail-content { font-size: 16px; color: var(--text, #ddd); line-height: 1.95; letter-spacing: -0.2px; }
.body-p { white-space: pre-wrap; margin: 0 0 18px; }
.body-img { display: block; max-width: 100%; margin: 26px auto; border-radius: 12px; }

/* ── 댓글 ── */
.cmt { margin-top: 44px; border-top: 1px solid var(--border, #222); padding-top: 24px; }
.cmt-head { font-size: 15px; font-weight: 800; color: var(--text, #eee); margin-bottom: 16px; }
.cmt-head b { color: #9b5cff; }
.cmt-list { display: flex; flex-direction: column; gap: 14px; margin-bottom: 18px; }
.cmt-item { display: flex; gap: 10px; align-items: flex-start; }
.cmt-pic { width: 34px; height: 34px; border-radius: 50%; object-fit: cover; flex-shrink: 0;
  background: var(--bg3, #1c1c22); display: flex; align-items: center; justify-content: center; font-size: 15px; }
.cmt-body { flex: 1; min-width: 0; }
.cmt-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 3px; }
.cmt-nick { font-size: 12.5px; font-weight: 700; color: var(--text2, #a0a0a8); }
.cmt-date { font-size: 11px; color: var(--text3, #6c6c74); }
.cmt-del { margin-left: auto; background: none; border: none; font-size: 11px; color: var(--text3, #6c6c74);
  cursor: pointer; font-family: 'Pretendard', sans-serif; }
.cmt-del:hover { color: var(--live, #ff5050); }
.cmt-text { font-size: 13.5px; color: var(--text, #ddd); line-height: 1.7; white-space: pre-wrap; word-break: break-word; }
.cmt-empty { font-size: 12.5px; color: var(--text3, #6c6c74); text-align: center; padding: 18px 0; }
.cmt-write { display: flex; gap: 8px; align-items: flex-end; }
.cmt-write textarea { flex: 1; background: var(--bg3, #1c1c22); border: 1px solid var(--border, #333);
  border-radius: 10px; padding: 10px 12px; color: var(--text, #eee); font-size: 13px; outline: none;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif; resize: vertical; }
.cmt-write textarea:disabled { opacity: .55; }
.cmt-submit { padding: 10px 18px; border-radius: 9px; border: none; cursor: pointer;
  background: linear-gradient(135deg, #5b7cfa, #9b5cff); color: #fff; font-size: 12.5px; font-weight: 700;
  font-family: 'Pretendard', sans-serif; }
.cmt-submit:disabled { opacity: .45; cursor: default; }

/* ── 작성 폼 ── */
.write-card { margin: 24px auto 0; max-width: 760px; background: var(--bg2, #141418); border: 1px solid var(--border, #222);
  border-radius: 14px; padding: 22px; display: flex; flex-direction: column; gap: 8px; }
.write-head { font-size: 15px; font-weight: 800; color: var(--text, #eee); margin-bottom: 6px; }
.write-label { font-size: 12.5px; font-weight: 700; color: var(--text2, #a0a0a8); margin-top: 8px; }
.hint-inline { font-size: 11.5px; font-weight: 400; color: var(--text3, #6c6c74); }
.write-card input, .write-card textarea { background: var(--bg3, #1c1c22); border: 1px solid var(--border, #333);
  border-radius: 9px; padding: 11px 13px; color: var(--text, #eee); font-size: 13.5px; outline: none;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif; resize: vertical; }
.write-title { font-size: 15px !important; font-weight: 700; }
.body-img-row { display: flex; align-items: center; gap: 10px; }
.thumb-row { display: flex; align-items: center; gap: 10px; }
.upload-label { padding: 8px 14px; border-radius: 8px; border: 1px dashed var(--border2, #444); font-size: 12px;
  color: var(--text2, #a0a0a8); cursor: pointer; }
.thumb-preview { height: 52px; border-radius: 8px; }
.submit { align-self: flex-end; padding: 10px 24px; border-radius: 9px; border: none; cursor: pointer; margin-top: 8px;
  background: linear-gradient(135deg, #5b7cfa, #9b5cff); color: #fff; font-size: 13px; font-weight: 700;
  font-family: 'Pretendard', sans-serif; }
.submit:disabled { opacity: .5; cursor: default; }
@media (max-width: 600px) { .row { flex-direction: column; align-items: stretch; } .row-thumb { width: 100%; } }
</style>
