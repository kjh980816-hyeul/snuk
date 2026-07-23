<script setup lang="ts">
// 무료소스 자료실(항목 19) — 공개 목록·다운로드. 등록/삭제는 어드민 탭에서.
import { onMounted, ref } from 'vue'
import { resourceApi } from '@/api'
import type { FreeResource } from '@/api/types'

const list = ref<FreeResource[]>([])
const loading = ref(true)

function dt(v: string) {
  return v?.slice(0, 10).split('-').join('.')
}

onMounted(async () => {
  try {
    list.value = await resourceApi.list()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section>
    <div class="inner">
      <div class="section-header">
        <h2 class="section-title">무료소스</h2>
      </div>
      <p class="sub">방송·영상 제작에 자유롭게 쓸 수 있는 무료 소스 모음 — 링크로 이동하거나 바로 내려받으세요</p>

      <div v-if="loading" class="empty">불러오는 중…</div>
      <div v-else-if="!list.length" class="empty">아직 등록된 소스가 없습니다.</div>
      <div v-else class="grid">
        <div v-for="r in list" :key="r.id" class="card">
          <div class="thumb">
            <img v-if="r.imageUrl" :src="r.imageUrl" alt="" />
            <span v-else>🎁</span>
          </div>
          <div class="body">
            <div class="title">{{ r.title }}</div>
            <div v-if="r.description" class="desc">{{ r.description }}</div>
            <div class="meta">{{ dt(r.createdAt) }}</div>
            <a v-if="r.fileUrl && r.fileUrl.startsWith('http')" :href="r.fileUrl" target="_blank" rel="noopener" class="dl">🔗 소스 보러가기</a>
            <a v-else-if="r.fileUrl" :href="r.fileUrl" download class="dl">⬇ 다운로드</a>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
section { padding: 40px 0 60px; }
.sub { font-size: 13px; color: var(--text3, #6c6c74); margin: -14px 0 24px; }
.empty { border: 1px dashed var(--border2, #333); border-radius: 12px; padding: 40px 16px; text-align: center;
  color: var(--text3, #6c6c74); font-size: 13px; }
.grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 14px; }
.card { background: var(--bg2, #141418); border: 1px solid var(--border, #222); border-radius: 14px;
  overflow: hidden; display: flex; flex-direction: column; transition: all .2s; }
.card:hover { border-color: #5b7cfa; transform: translateY(-2px); }
.thumb { aspect-ratio: 16/9; background: var(--bg3, #1c1c22); display: flex; align-items: center;
  justify-content: center; font-size: 38px; position: relative; overflow: hidden; }
.thumb img { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.body { padding: 14px; display: flex; flex-direction: column; gap: 6px; flex: 1; }
.title { font-size: 14px; font-weight: 700; color: var(--text, #eee); }
.desc { font-size: 12px; color: var(--text3, #8a8a92); line-height: 1.6; white-space: pre-wrap;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; }
.meta { font-size: 11px; color: var(--text3, #6c6c74); }
.dl { margin-top: auto; align-self: flex-start; padding: 8px 16px; border-radius: 9px; font-size: 12px;
  font-weight: 700; color: #fff; background: linear-gradient(135deg, #5b7cfa, #9b5cff); text-decoration: none; }
</style>
