<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { collabApi } from '@/api'
import type { CollabGame, Review } from '@/api/types'

const games = ref<CollabGame[]>([])
const reviews = ref<Review[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    const [g, r] = await Promise.all([collabApi.games(), collabApi.allReviews()])
    games.value = g
    reviews.value = r
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="wrap section">
    <h2 class="section-label">콜라보</h2>
    <div v-if="loading" class="empty-state">불러오는 중…</div>
    <div v-else class="collab-layout">
      <div class="reviews">
        <h4 class="sub">후기 모음</h4>
        <ul v-if="reviews.length" class="review-list">
          <li v-for="r in reviews" :key="r.id">
            <strong>{{ r.title }}</strong>
            <p>{{ r.content }}</p>
          </li>
        </ul>
        <div v-else class="empty-state">아직 후기가 없습니다.</div>
      </div>
      <div class="games">
        <div v-if="games.length" class="game-grid">
          <article v-for="g in games" :key="g.id" class="game-card">
            <div class="thumb">
              <img v-if="g.thumbnailUrl" :src="g.thumbnailUrl" :alt="g.name" />
              <div v-else class="placeholder">게임 썸네일</div>
            </div>
            <h5>{{ g.name }}</h5>
            <p class="desc">{{ g.description }}</p>
            <div class="links">
              <a v-if="g.gameLinkUrl" class="btn orange sm" :href="g.gameLinkUrl" target="_blank" rel="noopener">게임 링크 &#9654;</a>
              <a v-if="g.reviewLinkUrl" class="btn ghost sm" :href="g.reviewLinkUrl" target="_blank" rel="noopener">후기 링크 &#9654;</a>
            </div>
          </article>
        </div>
        <div v-else class="empty-state">등록된 콜라보 게임이 없습니다.</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.collab-layout { display: grid; grid-template-columns: 1fr 2fr; gap: 24px; }
.sub { margin: 0 0 14px; color: var(--text-strong); }
.review-list { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 12px; }
.review-list li { background: #fafafa; border-radius: 8px; padding: 12px 14px; }
.review-list p { margin: 4px 0 0; color: var(--text-muted); font-size: 14px; }
.game-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 18px; }
.thumb { width: 100%; aspect-ratio: 16/9; margin-bottom: 10px; }
.thumb img { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; }
.thumb .placeholder { width: 100%; height: 100%; }
.game-card h5 { margin: 0 0 6px; color: var(--text-strong); }
.game-card .desc { font-size: 14px; color: var(--text-muted); min-height: 38px; }
.game-card .links { display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap; }

@media (max-width: 860px) { .collab-layout { grid-template-columns: 1fr; } }
</style>
