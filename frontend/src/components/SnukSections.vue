<script lang="ts">
// 섹션 마크업은 전 페이지 공용 — 모듈 레벨 1회 fetch 캐시
let sectionsCache: string | null = null
</script>

<script setup lang="ts">
import { onMounted, ref, nextTick } from 'vue'

// 시안 섹션 마크업(공용) 중 show 목록만 노출, hide 목록의 하위 요소는 숨김
const props = defineProps<{ show: string[]; hide?: string[] }>()
const html = ref('')

onMounted(async () => {
  if (sectionsCache === null) {
    sectionsCache = await fetch('/snuk-sections.html').then((r) => r.text())
  }
  html.value = sectionsCache ?? ''
  await nextTick()
  const root = document.getElementById('snuk-sections-root')
  if (root) {
    root.querySelectorAll('section').forEach((sec) => {
      ;(sec as HTMLElement).style.display = props.show.includes(sec.id) ? '' : 'none'
    })
    for (const id of props.hide ?? []) {
      const el = document.getElementById(id)
      if (el) el.style.display = 'none'
    }
  }
  // 실데이터 렌더러 재실행 (스크립트 로드 전이면 no-op — SnukShell 이 로드 후 실행)
  ;(window as unknown as Record<string, undefined | (() => void)>).__snukInit?.()
})
</script>

<template>
  <div id="snuk-sections-root" v-html="html"></div>
</template>
