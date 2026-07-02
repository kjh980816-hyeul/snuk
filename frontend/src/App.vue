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
  <SiteHeader />
  <div class="page-body">
    <TopStrip />
    <main>
      <RouterView />
    </main>
    <SiteFooter />
  </div>
</template>

<style>
:root { --header-w: 220px; }
main { min-height: 60vh; }
.page-body { margin-right: var(--header-w); }

@media (max-width: 760px) {
  .page-body { margin-right: 0; }
}
</style>
