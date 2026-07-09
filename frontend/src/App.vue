<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SnukShell from '@/components/SnukShell.vue'

const route = useRoute()
// OAuth 콜백(리다이렉트)·어드민(자체 라이트 UI)은 시안 셸 없이 렌더
const bare = computed(() => route.name === 'oauth-callback' || route.name === 'admin')
</script>

<template>
  <RouterView v-if="bare" />
  <SnukShell v-else>
    <RouterView />
  </SnukShell>
</template>
