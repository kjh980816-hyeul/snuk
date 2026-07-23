<script setup lang="ts">
// 스눅 뉴스 매거진(항목 11/16) — 목록·상세·작성(REPORTER/ADMIN)
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { newsApi } from '@/api'
import type { News } from '@/api/types'
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

const detailId = computed(() => {
  const raw = route.params.id
  return raw ? Number(raw) : null
})

function dt(v: string) {
  return v?.slice(0, 10).split('-').join('.')
}

async function reload() {
  list.value = await newsApi.list()
}

async function loadDetail() {
  if (detailId.value == null) {
    detail.value = null
    return
  }
  try {
    detail.value = await newsApi.detail(detailId.value)
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
      <!-- 상세 -->
      <template v-if="detail">
        <RouterLink to="/news" class="back">&lt; 뉴스 목록</RouterLink>
        <article class="news-detail">
          <h1 class="news-detail-title">{{ detail.title }}</h1>
          <div class="news-detail-meta">
            <span>{{ detail.authorName }} 기자</span>
            <span>·</span>
            <span>{{ dt(detail.createdAt) }}</span>
            <div v-if="canManage(detail)" class="manage">
              <button class="mini" @click="openEdit(detail)">수정</button>
              <button class="mini danger" @click="remove(detail)">삭제</button>
            </div>
          </div>
          <img v-if="detail.thumbnailUrl" :src="detail.thumbnailUrl" class="news-detail-thumb" alt="" />
          <div class="news-detail-content">{{ detail.content }}</div>
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

      <!-- 작성/수정 폼 -->
      <div v-if="writeOpen" class="write-card">
        <div class="write-head">{{ editingId != null ? '기사 수정' : '새 기사 작성' }}</div>
        <input v-model="title" placeholder="기사 제목" maxlength="200" />
        <textarea v-model="content" rows="10" placeholder="기사 내용을 입력하세요."></textarea>
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
.back { display: inline-block; font-size: 13px; color: var(--text2, #a0a0a8); text-decoration: none; margin-bottom: 16px; }
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
.mini { padding: 5px 10px; border-radius: 7px; font-size: 11px; cursor: pointer; border: 1px solid var(--border, #333);
  background: var(--bg3, #1c1c22); color: var(--text2, #a0a0a8); font-family: 'Pretendard', sans-serif; }
.mini.danger { color: #ff7070; border-color: rgba(255, 80, 80, .35); }
.news-detail { max-width: 820px; }
.news-detail-title { font-size: 26px; font-weight: 800; color: var(--text, #eee); line-height: 1.35; margin-bottom: 12px; }
.news-detail-meta { display: flex; align-items: center; gap: 8px; font-size: 12.5px; color: var(--text3, #6c6c74);
  padding-bottom: 16px; border-bottom: 1px solid var(--border, #222); margin-bottom: 20px; }
.news-detail-thumb { width: 100%; border-radius: 14px; margin-bottom: 20px; }
.news-detail-content { font-size: 14.5px; color: var(--text, #ddd); line-height: 1.9; white-space: pre-wrap; }
.write-card { margin-top: 24px; background: var(--bg2, #141418); border: 1px solid var(--border, #222);
  border-radius: 14px; padding: 18px; display: flex; flex-direction: column; gap: 10px; }
.write-head { font-size: 14px; font-weight: 700; color: var(--text, #eee); }
.write-card input, .write-card textarea { background: var(--bg3, #1c1c22); border: 1px solid var(--border, #333);
  border-radius: 9px; padding: 10px 12px; color: var(--text, #eee); font-size: 13px; outline: none;
  font-family: 'Pretendard', 'Noto Sans KR', sans-serif; resize: vertical; }
.thumb-row { display: flex; align-items: center; gap: 10px; }
.upload-label { padding: 8px 14px; border-radius: 8px; border: 1px dashed var(--border2, #444); font-size: 12px;
  color: var(--text2, #a0a0a8); cursor: pointer; }
.thumb-preview { height: 52px; border-radius: 8px; }
.submit { align-self: flex-end; padding: 9px 22px; border-radius: 9px; border: none; cursor: pointer;
  background: linear-gradient(135deg, #5b7cfa, #9b5cff); color: #fff; font-size: 13px; font-weight: 700;
  font-family: 'Pretendard', sans-serif; }
.submit:disabled { opacity: .5; cursor: default; }
@media (max-width: 600px) { .row { flex-direction: column; align-items: stretch; } .row-thumb { width: 100%; } }
</style>
