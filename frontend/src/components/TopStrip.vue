<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { campaignApi, tournamentApi } from '@/api'

interface StripItem {
  key: string
  label: string
  title: string
  desc: string | null
  image: string | null
  to: string
}

const items = ref<StripItem[]>([])
const idx = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const current = computed(() => items.value[idx.value] ?? null)

onMounted(async () => {
  // 모집중(OPEN)인 컨텐츠·대회만 노출. 없으면 배너 자체를 숨김(empty state 원칙).
  const [campaigns, tournaments] = await Promise.all([
    campaignApi.list().catch(() => []),
    tournamentApi.list().catch(() => []),
  ])
  items.value = [
    ...campaigns.filter((c) => c.status === 'OPEN').map((c) => ({
      key: `c${c.id}`, label: '모집중', title: c.title, desc: c.description,
      image: c.promoImageUrl, to: '/campaigns',
    })),
    ...tournaments.filter((t) => t.status === 'OPEN').map((t) => ({
      key: `t${t.id}`, label: '대회', title: t.title, desc: t.description,
      image: t.bannerImageUrl, to: '/tournaments',
    })),
  ]
  if (items.value.length > 1) {
    timer = setInterval(() => {
      idx.value = (idx.value + 1) % items.value.length
    }, 5000)
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <RouterLink v-if="current" :to="current.to" class="top-banner">
    <Transition name="roll" mode="out-in">
      <div :key="current.key" class="banner-inner">
        <div
          v-if="current.image"
          class="banner-bg"
          :style="{ backgroundImage: `url(${current.image})` }"
        ></div>
        <div class="banner-content">
          <span class="banner-badge">{{ current.label }}</span>
          <h3 class="banner-title">{{ current.title }}</h3>
          <p v-if="current.desc" class="banner-desc">{{ current.desc }}</p>
          <span class="banner-cta">자세히 보기 &gt;</span>
        </div>
      </div>
    </Transition>
    <div v-if="items.length > 1" class="dots">
      <span v-for="(it, i) in items" :key="it.key" class="dot" :class="{ on: i === idx }"></span>
    </div>
  </RouterLink>
</template>

<style scoped>
.top-banner {
  position: relative;
  display: block;
  overflow: hidden;
  background: #1a1a1a;
  color: #fff;
}
.banner-inner { position: relative; min-height: 160px; display: flex; align-items: center; }
.banner-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  opacity: 0.45;
  transition: transform 0.4s ease;
}
.top-banner:hover .banner-bg { transform: scale(1.04); }
.banner-content {
  position: relative;
  padding: 26px 48px;
  max-width: 900px;
}
.banner-badge {
  display: inline-block;
  background: var(--accent-orange);
  color: #fff;
  font-weight: 800;
  font-size: 13px;
  padding: 4px 14px;
  border-radius: 999px;
  margin-bottom: 10px;
}
.banner-title {
  font-size: 26px;
  font-weight: 900;
  margin: 0 0 6px;
  line-height: 1.25;
  text-shadow: 0 2px 8px rgba(0, 0, 0, 0.5);
}
.banner-desc {
  margin: 0 0 10px;
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 640px;
}
.banner-cta { font-weight: 800; color: var(--brand-yellow); font-size: 14px; }
.top-banner:hover .banner-cta { text-decoration: underline; }

.dots { position: absolute; right: 20px; bottom: 14px; display: flex; gap: 6px; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: rgba(255, 255, 255, 0.35); transition: background 0.2s; }
.dot.on { background: var(--accent-orange); }

.roll-enter-active, .roll-leave-active { transition: opacity 0.35s ease, transform 0.35s ease; }
.roll-enter-from { opacity: 0; transform: translateX(24px); }
.roll-leave-to { opacity: 0; transform: translateX(-24px); }

@media (max-width: 760px) {
  .banner-inner { min-height: 120px; }
  .banner-content { padding: 18px 20px; }
  .banner-title { font-size: 19px; }
  .banner-desc { display: none; }
}
</style>
