<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { campaignApi, collabApi } from '@/api'
import type { Campaign, ContentVideo } from '@/api/types'
import { toEmbed } from '@/composables/useApply'

const router = useRouter()
const campaigns = ref<Campaign[]>([])
const videos = ref<ContentVideo[]>([])
const loading = ref(true)

const heroVideo = computed(() => videos.value.find((v) => v.featured) ?? videos.value[0] ?? null)
const featured = computed(() => campaigns.value.find((c) => c.featured && c.status === 'OPEN')
  ?? campaigns.value.find((c) => c.featured) ?? null)

onMounted(async () => {
  try {
    const [c, v] = await Promise.all([campaignApi.list(), collabApi.videos()])
    campaigns.value = c
    videos.value = v
  } finally {
    loading.value = false
  }
})

const sections = [
  { to: '/campaigns', label: '신청가능/진행중인 컨텐츠', desc: '게임 콜라보·체험단 신청' },
  { to: '/videos', label: '컨텐츠 영상', desc: 'SNUK 제작 영상 모아보기' },
  { to: '/collab', label: '콜라보', desc: '콜라보 게임 & 후기 모음' },
  { to: '/clients', label: 'CLIENTS', desc: '함께한 협업 업체' },
]
</script>

<template>
  <div class="home">
    <!-- S1 히어로 -->
    <section class="hero wrap section">
      <div v-if="heroVideo" class="hero-frame">
        <iframe :src="toEmbed(heroVideo.videoUrl)" title="대표 영상" allowfullscreen></iframe>
      </div>
      <div v-else class="hero-frame placeholder">SNUK 대표 영상 / GIF</div>
    </section>

    <!-- 대표 캠페인 티저 -->
    <section class="wrap section" v-if="featured">
      <h2 class="section-label">지금 신청 가능한 컨텐츠</h2>
      <div class="teaser" @click="router.push('/campaigns')">
        <div class="teaser-img">
          <img v-if="featured.promoImageUrl" :src="featured.promoImageUrl" alt="홍보 이미지" />
          <div v-else class="placeholder tall">컨텐츠 홍보사진</div>
        </div>
        <div class="teaser-body">
          <span class="badge" :class="featured.status">{{ featured.status }}</span>
          <h3>{{ featured.title }}</h3>
          <p class="desc">{{ featured.description }}</p>
          <RouterLink to="/campaigns" class="btn" @click.stop>신청하러 가기 &gt;</RouterLink>
        </div>
      </div>
    </section>

    <hr class="divider" />

    <!-- 섹션 바로가기 -->
    <section class="wrap section">
      <div class="nav-cards">
        <RouterLink v-for="s in sections" :key="s.to" :to="s.to" class="nav-card">
          <h4>{{ s.label }}</h4>
          <p>{{ s.desc }}</p>
          <span class="arrow">&#9654;</span>
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero-frame {
  position: relative; width: 100%; aspect-ratio: 16 / 9;
  border-radius: var(--radius); overflow: hidden; background: #000;
}
.hero-frame iframe { position: absolute; inset: 0; width: 100%; height: 100%; border: 0; }
.placeholder.tall { aspect-ratio: 4 / 3; }

.teaser { display: grid; grid-template-columns: 1.1fr 1fr; gap: 28px; align-items: center; cursor: pointer; }
.teaser-img img { width: 100%; border-radius: var(--radius); display: block; }
.teaser-body h3 { font-size: 26px; color: var(--text-strong); margin: 12px 0 8px; }
.teaser-body .desc { color: var(--text-body); margin-bottom: 16px; }

.nav-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 18px; }
.nav-card {
  position: relative; border: 1px solid #eee; border-radius: var(--radius);
  padding: 22px; background: #fff; transition: box-shadow .15s, transform .15s;
}
.nav-card:hover { box-shadow: 0 8px 24px rgba(0,0,0,.08); transform: translateY(-2px); }
.nav-card h4 { margin: 0 0 8px; color: var(--text-strong); font-size: 17px; }
.nav-card p { margin: 0; color: var(--text-muted); font-size: 14px; }
.nav-card .arrow { position: absolute; right: 18px; bottom: 18px; color: var(--accent-orange); }

@media (max-width: 860px) {
  .teaser { grid-template-columns: 1fr; }
}
</style>
