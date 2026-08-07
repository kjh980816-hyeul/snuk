<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 메인 = 데모 시안 구성 한 섹션(#home-demo): 배너 / 라이브 스테이지 / 스트리머 /
// 모집 컨텐츠 · 체험단 / 체험단 후기 / 커뮤니티 · 뉴스 / 방송도우미 소스
const show = ['home-demo']
</script>

<template>
  <SnukSections :show="show" />
</template>
