<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 온에어: 콘텐츠 영상 (2슬롯 플레이어 + 리스트)
const show = ['videos']
</script>

<template>
  <SnukSections :show="show" />
</template>
