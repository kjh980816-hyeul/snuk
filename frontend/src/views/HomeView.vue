<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 메인: 히어로 + 통합 빅그리드(컨텐츠+대회) + 게임체험단 + 굿즈 베스트10 + 영상 + 스트리머 스트립
const show = ['hero', 'big-contents', 'game-trial', 'goods-best', 'videos', 'streamer-strip']
</script>

<template>
  <SnukSections :show="show" />
</template>
