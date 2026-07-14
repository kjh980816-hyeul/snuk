<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 컨텐츠·대회 통합 페이지 — 한 목록에 컨텐츠+대회 함께(모집중 우선), 상단 피처드는 공식 컨텐츠
const show = ['snuk-contents']
</script>

<template>
  <SnukSections :show="show" />
</template>
