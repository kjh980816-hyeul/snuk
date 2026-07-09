<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { campaignApi } from '@/api'
import type { Campaign, Review } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const id = Number(route.params.id)

const campaign = ref<Campaign | null>(null)
const reviews = ref<Review[]>([])
const loading = ref(true)

const writeOpen = ref(false)
const title = ref('')
const content = ref('')
const submitting = ref(false)

async function reload() {
  reviews.value = await campaignApi.reviews(id)
}

async function submit() {
  if (!title.value.trim() || submitting.value) return
  submitting.value = true
  try {
    await campaignApi.writeReview(id, { title: title.value.trim(), content: content.value.trim() })
    title.value = ''
    content.value = ''
    writeOpen.value = false
    await reload()
  } catch (e) {
    const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
    alert(msg ?? '후기 작성에 실패했어요. 이 컨텐츠 참가자만 작성할 수 있습니다.')
  } finally {
    submitting.value = false
  }
}

function dt(v: string) {
  return v?.slice(0, 10)
}

onMounted(async () => {
  try {
    campaign.value = await campaignApi.detail(id)
    await reload()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section>
    <div class="inner">
    <RouterLink to="/campaigns" class="back">&lt; 컨텐츠 목록</RouterLink>
    <div class="section-header">
      <h2 class="section-title">{{ campaign ? `‘${campaign.title}’ 후기 게시판` : '후기 게시판' }}</h2>
    </div>
    <p v-if="campaign" class="game">게임 · {{ campaign.gameName }}</p>

    <div class="board-head">
      <span class="count">후기 {{ reviews.length }}개</span>
      <button v-if="auth.isLoggedIn" class="btn orange sm" @click="writeOpen = !writeOpen">
        {{ writeOpen ? '작성 취소' : '후기 작성' }}
      </button>
      <button v-else class="btn ghost sm" @click="auth.login()">로그인 후 작성</button>
    </div>

    <div v-if="writeOpen" class="write-card">
      <input v-model="title" placeholder="제목" maxlength="100" />
      <textarea v-model="content" rows="6" placeholder="이 게임/방송은 어땠나요? 참가자만 작성할 수 있어요."></textarea>
      <button class="btn sm" :disabled="submitting || !title.trim()" @click="submit">
        {{ submitting ? '등록 중…' : '등록' }}
      </button>
    </div>

    <div v-if="loading" class="empty-state">불러오는 중…</div>
    <div v-else-if="!reviews.length" class="empty-state">아직 후기가 없습니다. 첫 후기를 남겨보세요!</div>
    <article v-for="r in reviews" :key="r.id" class="review">
      <div class="review-head">
        <h4>{{ r.title }}</h4>
        <span class="meta">회원 #{{ r.memberId }} · {{ dt(r.createdAt) }}</span>
      </div>
      <p v-if="r.content" class="content">{{ r.content }}</p>
    </article>
    </div>
  </section>
</template>

<style scoped>
/* 시안 다크 테마 정합 — home-snuk.css 변수 사용 */
.back { font-size: 13px; color: var(--text3); display: inline-block; margin-bottom: 8px; }
.game { color: var(--text3); margin: -8px 0 18px; font-size: 14px; }
.board-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.count { font-weight: 700; color: var(--text); }
.write-card { border: 1px solid var(--border); background: var(--bg2); border-radius: 12px; padding: 16px; margin-bottom: 18px;
  display: flex; flex-direction: column; gap: 10px; }
.write-card input, .write-card textarea { width: 100%; padding: 10px; border: 1px solid var(--border); background: var(--bg3); color: var(--text); border-radius: 8px; font-size: 14px; outline: none; }
.write-card button { align-self: flex-end; }
.review { border-bottom: 1px solid var(--border); padding: 16px 4px; }
.review-head { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; }
.review-head h4 { margin: 0; color: var(--text); }
.review-head .meta { font-size: 12px; color: var(--text3); white-space: nowrap; }
.review .content { margin: 8px 0 0; color: var(--text2); white-space: pre-wrap; }
.btn.sm, .btn.orange.sm, .btn.ghost.sm {
  padding: 8px 14px; font-size: 13px; border-radius: 8px; font-weight: 700; cursor: pointer;
  background: linear-gradient(135deg, var(--accent), var(--accent2)); color: #fff; border: none;
}
.btn.ghost.sm { background: transparent; color: var(--text2); border: 1px solid var(--border); }
.btn.sm:disabled { opacity: .5; cursor: not-allowed; }
.empty-state { padding: 40px 20px; text-align: center; color: var(--text3); background: transparent; border: 1px dashed var(--border2); border-radius: 12px; }
</style>
