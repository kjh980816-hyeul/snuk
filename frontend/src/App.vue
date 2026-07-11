<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SnukShell from '@/components/SnukShell.vue'

const route = useRoute()
// OAuth 콜백(리다이렉트)·어드민은 시안 셸 없이 렌더.
// 라우트 해석 전(name undefined)에도 bare — 셸이 잠깐 마운트됐다 언마운트되는 낭비/레이스 방지
const bare = computed(
  () => route.name === undefined || route.name === 'oauth-callback' || route.name === 'admin',
)
</script>

<template>
  <RouterView v-if="bare" />
  <SnukShell v-else>
    <RouterView />
  </SnukShell>
</template>
