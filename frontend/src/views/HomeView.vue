<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 메인: 히어로 + 컨텐츠 그리드 + 대회 그리드(분리) + 게임체험단 + 굿즈 베스트10 + 영상 + 스트리머 스트립
const show = ['hero', 'big-contents', 'big-tournaments', 'game-trial', 'goods-best', 'videos', 'streamer-strip']
</script>

<template>
  <SnukSections :show="show" />
</template>
