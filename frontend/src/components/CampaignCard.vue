<script setup lang="ts">
import type { Campaign } from '@/api/types'

defineProps<{ campaign: Campaign }>()
const emit = defineEmits<{ apply: [c: Campaign] }>()

const statusKo: Record<string, string> = { SCHEDULED: '예정', OPEN: '모집중', CLOSED: '마감' }
</script>

<template>
  <article class="card">
    <div class="card-img">
      <span class="badge" :class="campaign.status">{{ statusKo[campaign.status] ?? campaign.status }}</span>
      <img v-if="campaign.promoImageUrl" :src="campaign.promoImageUrl" :alt="campaign.title" />
      <div v-else class="placeholder">홍보사진</div>
    </div>
    <div class="card-body">
      <h5>{{ campaign.title }}</h5>
      <p class="desc">{{ campaign.description }}</p>
      <p class="date" v-if="campaign.eventDate">{{ campaign.eventDate }}</p>
      <div class="card-acts">
        <button class="btn sm" :disabled="campaign.status !== 'OPEN'" @click="emit('apply', campaign)">
          신청하기 &gt;
        </button>
        <RouterLink :to="`/campaigns/${campaign.id}/reviews`" class="btn ghost sm">후기</RouterLink>
      </div>
    </div>
  </article>
</template>

<style scoped>
.card { border: 1px solid #eee; border-radius: var(--radius); overflow: hidden; background: #fff; }
.card-img { position: relative; aspect-ratio: 16/10; }
.card-img img { width: 100%; height: 100%; object-fit: cover; }
.card-img .placeholder { width: 100%; height: 100%; }
.card-img .badge { position: absolute; top: 8px; left: 8px; z-index: 1; }
.card-body { padding: 12px 14px; }
.card-body h5 { margin: 0 0 6px; color: var(--text-strong); }
.card-body .desc { font-size: 13px; color: var(--text-muted); min-height: 34px; margin: 0 0 6px;
  display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-body .date { font-size: 12px; color: var(--text-muted); margin: 0 0 10px; }
.btn.sm { padding: 8px 14px; font-size: 13px; }
.card-acts { display: flex; gap: 8px; align-items: center; }
</style>
