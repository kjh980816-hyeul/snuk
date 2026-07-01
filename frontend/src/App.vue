<script setup lang="ts">
import { onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'

const auth = useAuthStore()
onMounted(() => {
  if (!auth.loaded) auth.fetchMe()
})
</script>

<template>
  <SiteHeader />
  <main>
    <RouterView />
  </main>
  <SiteFooter />
</template>

<style>
main { min-height: 60vh; }
</style>
