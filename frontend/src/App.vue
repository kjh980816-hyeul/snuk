<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'
import TopStrip from '@/components/TopStrip.vue'

const auth = useAuthStore()
onMounted(() => {
  if (!auth.loaded) auth.fetchMe()
})
</script>

<template>
  <TopStrip />
  <div class="layout">
    <div class="content">
      <main>
        <RouterView />
      </main>
      <SiteFooter />
    </div>
    <SiteHeader />
  </div>
</template>

<style>
:root { --header-w: 220px; }
main { min-height: 60vh; }
.layout { display: flex; align-items: flex-start; }
/* 좌측에 사이드바 폭만큼 패딩 → 본문이 화면 전체 기준 중앙 */
.content { flex: 1; min-width: 0; padding-left: var(--header-w); }

@media (max-width: 760px) {
  .layout { flex-direction: column; }
  .layout > .site-header { order: -1; width: 100%; }
  .content { padding-left: 0; width: 100%; }
}
</style>
