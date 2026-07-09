<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 스트리머: 공식 파트너 스트리머(STREAMER 등급 회원)
const show = ['streamers-channel']
</script>

<template>
  <SnukSections :show="show" />
</template>
