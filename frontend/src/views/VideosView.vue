<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { collabApi } from '@/api'
import type { ContentVideo } from '@/api/types'
import { toEmbed } from '@/composables/useApply'

const videos = ref<ContentVideo[]>([])
const loading = ref(true)
const current = ref<ContentVideo | null>(null)

const main = computed(() => current.value ?? videos.value.find((v) => v.featured) ?? videos.value[0] ?? null)

onMounted(async () => {
  try {
    videos.value = await collabApi.videos()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="wrap section">
    <h2 class="section-label">컨텐츠 영상</h2>
    <div v-if="loading" class="empty-state">불러오는 중…</div>
    <div v-else-if="videos.length" class="video-layout">
      <div class="video-main">
        <div class="hero-frame">
          <iframe v-if="main" :src="toEmbed(main.videoUrl)" title="영상" allowfullscreen></iframe>
        </div>
        <p class="video-title">{{ main?.title }}</p>
      </div>
      <ul class="video-side">
        <li v-for="v in videos" :key="v.id" :class="{ on: main?.id === v.id }" @click="current = v">
          <div class="thumb">
            <img v-if="v.thumbnailUrl" :src="v.thumbnailUrl" :alt="v.title" />
            <div v-else class="placeholder">썸네일</div>
          </div>
          <span>{{ v.title }}</span>
        </li>
      </ul>
    </div>
    <div v-else class="empty-state">등록된 영상이 없습니다.</div>
  </div>
</template>

<style scoped>
.video-layout { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }
.hero-frame { position: relative; width: 100%; aspect-ratio: 16/9; border-radius: var(--radius); overflow: hidden; background: #000; }
.hero-frame iframe { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; }
.video-title { font-weight: 700; margin-top: 10px; }
.video-side { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 12px; max-height: 460px; overflow-y: auto; }
.video-side li { display: flex; gap: 10px; align-items: center; cursor: pointer; padding: 4px; border-radius: 8px; }
.video-side li.on { background: #fff7ec; }
.video-side .thumb { width: 110px; aspect-ratio: 16/9; flex: none; }
.thumb img { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; }
.thumb .placeholder { width: 100%; height: 100%; }

@media (max-width: 860px) { .video-layout { grid-template-columns: 1fr; } }
</style>
