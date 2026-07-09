<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 메인: 히어로 + SNUK 컨텐츠 + 스트리머 + 게임체험단 + 영상 + 굿즈
const show = ['hero', 'snuk-contents', 'streamers-channel', 'game-trial', 'videos', 'goods']
</script>

<template>
  <SnukSections :show="show" />
</template>
