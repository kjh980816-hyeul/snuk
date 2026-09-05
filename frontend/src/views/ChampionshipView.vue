<script setup lang="ts">
import SnukSections from '@/components/SnukSections.vue'
// 대회 목록: 대표 대회 + 대회 카드 (카드 클릭 → /championship/:id 상세)
// 참여 스트리머 명단·홍보 상세는 상세 페이지(/championship/:id), 생방송은 /live
const show = ['mugchamps']
</script>

<template>
  <SnukSections :show="show" />
</template>
