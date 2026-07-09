<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 콘텐츠: SNUK 컨텐츠(캠페인) — 게임체험단은 /games 로 분리
const show = ['snuk-contents']
</script>

<template>
  <SnukSections :show="show" />
</template>
