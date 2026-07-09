<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 대회: 목록/신청 + 대진표 + 참여 스트리머 (생방송 임베드는 /live 로 분리)
const show = ['mugchamps']
const hide = ['mug-live-embed']
</script>

<template>
  <SnukSections :show="show" :hide="hide" />
</template>
