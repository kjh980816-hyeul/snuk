<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { campaignApi } from '@/api'
import type { Campaign } from '@/api/types'
import CampaignCard from '@/components/CampaignCard.vue'
import { useApply } from '@/composables/useApply'

const campaigns = ref<Campaign[]>([])
const loading = ref(true)

const featured = computed(() => campaigns.value.find((c) => c.featured && c.status === 'OPEN')
  ?? campaigns.value.find((c) => c.featured) ?? null)
const others = computed(() => campaigns.value.filter((c) => c.id !== featured.value?.id))

async function reload() {
  campaigns.value = await campaignApi.list()
}
const { applyMsg, apply } = useApply(reload)

onMounted(async () => {
  try {
    await reload()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="wrap section">
    <h2 class="section-label">신청가능/진행중인 컨텐츠</h2>

    <div v-if="loading" class="empty-state">불러오는 중…</div>
    <template v-else>
      <div v-if="featured" class="featured">
        <div class="featured-img">
          <img v-if="featured.promoImageUrl" :src="featured.promoImageUrl" alt="홍보 이미지" />
          <div v-else class="placeholder tall">컨텐츠 홍보사진</div>
        </div>
        <div class="featured-body">
          <span class="badge" :class="featured.status">{{ featured.status }}</span>
          <h3>{{ featured.title }}</h3>
          <p class="desc">{{ featured.description }}</p>
          <p class="meta" v-if="featured.gameName">게임: {{ featured.gameName }}</p>
          <p class="meta" v-if="featured.eventDate">일자: {{ featured.eventDate }}</p>
          <p class="meta" v-if="featured.applyStart">
            신청기간: {{ featured.applyStart?.slice(0, 10) }} ~ {{ featured.applyEnd?.slice(0, 10) }}
          </p>
          <p class="slots">모집 {{ featured.filledSlots }}/{{ featured.totalSlots }}</p>
          <button class="btn" :disabled="featured.status !== 'OPEN'" @click="apply(featured)">
            신청하기 &gt;
          </button>
          <p v-if="applyMsg" class="apply-msg">{{ applyMsg }}</p>
        </div>
      </div>

      <h4 class="sub" v-if="others.length">다른 컨텐츠</h4>
      <div v-if="others.length" class="card-grid">
        <CampaignCard v-for="c in others" :key="c.id" :campaign="c" @apply="apply" />
      </div>
      <div v-else-if="!featured" class="empty-state">아직 등록된 컨텐츠가 없습니다.</div>
    </template>
  </div>
</template>

<style scoped>
.featured { display: grid; grid-template-columns: 1.1fr 1fr; gap: 28px; align-items: center; }
.featured-img img { width: 100%; border-radius: var(--radius); display: block; }
.placeholder.tall { aspect-ratio: 4 / 3; }
.featured-body h3 { font-size: 26px; color: var(--text-strong); margin: 12px 0 8px; }
.featured-body .desc { color: var(--text-body); }
.featured-body .meta { font-size: 14px; color: var(--text-muted); margin: 2px 0; }
.featured-body .slots { font-weight: 700; color: var(--text-strong); margin: 10px 0; }
.apply-msg { margin-top: 10px; color: var(--accent-orange); font-weight: 700; }
.sub { margin: 36px 0 14px; color: var(--text-strong); }
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 18px; }

@media (max-width: 860px) {
  .featured { grid-template-columns: 1fr; }
}
</style>
