<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 대회 목록: 대표 대회 + 대회 카드 (카드 클릭 → /championship/:id 상세)
// 참여 스트리머 명단·홍보 상세는 상세 페이지로 이동, 생방송 임베드는 /live 로 분리
const show = ['mugchamps']
const hide = ['mug-live-embed', 'roster-box']
</script>

<template>
  <SnukSections :show="show" :hide="hide" />
</template>
