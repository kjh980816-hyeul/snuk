<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { campaignApi, tournamentApi } from '@/api'

interface StripItem {
  key: string
  label: string
  title: string
  to: string
}

const items = ref<StripItem[]>([])
const idx = ref(0)
let timer: ReturnType<typeof setInterval> | null = null

const current = computed(() => items.value[idx.value] ?? null)

onMounted(async () => {
  // 모집중(OPEN)인 컨텐츠·대회만 노출. 없으면 스트립 자체를 숨김(empty state 원칙).
  const [campaigns, tournaments] = await Promise.all([
    campaignApi.list().catch(() => []),
    tournamentApi.list().catch(() => []),
  ])
  items.value = [
    ...campaigns.filter((c) => c.status === 'OPEN').map((c) => ({
      key: `c${c.id}`, label: '모집중', title: c.title, to: '/campaigns',
    })),
    ...tournaments.filter((t) => t.status === 'OPEN').map((t) => ({
      key: `t${t.id}`, label: '대회', title: t.title, to: '/tournaments',
    })),
  ]
  if (items.value.length > 1) {
    timer = setInterval(() => {
      idx.value = (idx.value + 1) % items.value.length
    }, 4000)
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <RouterLink v-if="current" :to="current.to" class="top-strip">
    <span class="strip-badge">{{ current.label }}</span>
    <Transition name="roll" mode="out-in">
      <span :key="current.key" class="strip-title">{{ current.title }}</span>
    </Transition>
    <span class="strip-cta">바로가기 &gt;</span>
  </RouterLink>
</template>

<style scoped>
.top-strip {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 24px;
  background: linear-gradient(90deg, #1a1a1a, #33291a);
  color: #fff;
  font-size: 14px;
  transition: filter 0.15s ease;
}
.top-strip:hover { filter: brightness(1.25); }
.strip-badge {
  flex-shrink: 0;
  background: var(--accent-orange);
  color: #fff;
  font-weight: 800;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 999px;
}
.strip-title {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.strip-cta { flex-shrink: 0; margin-left: auto; font-weight: 700; color: var(--brand-yellow); }

.roll-enter-active, .roll-leave-active { transition: opacity 0.3s ease, transform 0.3s ease; }
.roll-enter-from { opacity: 0; transform: translateY(8px); }
.roll-leave-to { opacity: 0; transform: translateY(-8px); }
</style>
