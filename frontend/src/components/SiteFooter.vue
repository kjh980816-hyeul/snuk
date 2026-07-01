<script setup lang="ts">
// CONTACT US (S6) — 전 페이지 공통 푸터.
</script>

<template>
  <footer class="footer">
    <div class="wrap foot-inner">
      <div class="foot-logo">SNUK</div>
      <div class="foot-info">
        <p class="brand">SNUK</p>
        <p>E-mail: mumu_mm@naver.com</p>
        <p class="copy">Copyright © 2026 SNUK NETWORK Inc. All Rights Reserved</p>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.footer { background: #1a1a1a; color: #ddd; margin-top: 64px; padding: 40px 0; }
.foot-inner { display: flex; justify-content: space-between; align-items: center; gap: 20px; flex-wrap: wrap; }
.foot-logo { font-weight: 900; font-size: 28px; color: var(--brand-yellow); }
.foot-info { text-align: right; }
.foot-info .brand { font-weight: 700; color: #fff; }
.foot-info .copy { font-size: 12px; color: #888; margin-top: 8px; }

@media (max-width: 760px) {
  .foot-inner { flex-direction: column; text-align: center; }
  .foot-info { text-align: center; }
}
</style>
