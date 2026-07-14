<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 메인: 히어로 + 라이브 배너(어드민 on/off) + 컨텐츠·대회 통합(큰 칸=공식 컨텐츠) + 게임체험단 + 굿즈 + 영상 + 뉴스 + 스트리머 스트립
const show = ['hero', 'live-banner', 'spotlight-section', 'big-contents', 'game-trial', 'goods-best', 'videos', 'news-section', 'streamer-strip']
</script>

<template>
  <SnukSections :show="show" />
</template>
