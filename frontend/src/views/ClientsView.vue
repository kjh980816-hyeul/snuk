<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { collabApi } from '@/api'
import type { ClientLogo } from '@/api/types'

const clients = ref<ClientLogo[]>([])
const loading = ref(true)

onMounted(async () => {
  try {
    clients.value = await collabApi.clients()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="wrap section">
    <h2 class="section-label">CLIENTS</h2>
    <p class="clients-cap">SNUK 과 함께 협업진행 업체</p>
    <div v-if="loading" class="empty-state">불러오는 중…</div>
    <div v-else-if="clients.length" class="logo-grid">
      <a v-for="cl in clients" :key="cl.id" class="logo-cell"
         :href="cl.linkUrl ?? undefined" :target="cl.linkUrl ? '_blank' : undefined" rel="noopener">
        <img :src="cl.logoUrl" :alt="cl.name ?? 'client'" />
      </a>
    </div>
    <div v-else class="empty-state">등록된 협업 업체가 없습니다.</div>
  </div>
</template>

<style scoped>
.clients-cap { color: var(--text-body); margin: -8px 0 18px; }
.logo-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 16px; }
.logo-cell { background: #fafafa; border: 1px solid #eee; border-radius: 8px; height: 90px; display: flex; align-items: center; justify-content: center; }
.logo-cell img { max-width: 70%; max-height: 60%; object-fit: contain; }
</style>
