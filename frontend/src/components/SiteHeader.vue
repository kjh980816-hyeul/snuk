<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

async function onLogout() {
  await auth.logout()
  router.push('/')
}
</script>

<template>
  <header class="site-header">
    <div class="wrap inner">
      <RouterLink to="/" class="logo">SNUK</RouterLink>
      <nav class="nav">
        <RouterLink to="/" class="nav-link">홈</RouterLink>
        <RouterLink to="/campaigns" class="nav-link">컨텐츠</RouterLink>
        <RouterLink to="/videos" class="nav-link">영상</RouterLink>
        <RouterLink to="/collab" class="nav-link">콜라보</RouterLink>
        <RouterLink to="/goods" class="nav-link">굿즈</RouterLink>
        <RouterLink to="/clients" class="nav-link">CLIENTS</RouterLink>
      </nav>
      <div class="account">
        <template v-if="auth.isLoggedIn">
          <RouterLink to="/mypage" class="nav-link">마이페이지</RouterLink>
          <RouterLink v-if="auth.isAdmin" to="/admin" class="nav-link admin">관리자</RouterLink>
          <span class="me">{{ auth.me?.nickname }}</span>
          <button class="btn ghost sm" @click="onLogout">로그아웃</button>
        </template>
        <button v-else class="btn orange sm" @click="auth.login()">치지직 로그인</button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.site-header {
  position: sticky;
  top: 0;
  z-index: 50;
  background: #fff;
  border-bottom: 1px solid #eee;
}
.inner {
  display: flex;
  align-items: center;
  gap: 24px;
  height: 64px;
}
.logo {
  font-weight: 900;
  font-size: 26px;
  color: var(--brand-yellow);
  -webkit-text-stroke: 1px #1a1a1a;
  letter-spacing: 1px;
}
.nav { display: flex; align-items: center; gap: 18px; flex: 1; }
.nav-link { font-weight: 600; color: var(--text-body); }
.nav-link.router-link-exact-active { color: var(--text-strong); border-bottom: 2px solid var(--accent-orange); padding-bottom: 4px; }
.nav-link.admin { color: var(--label-red); }
.account { display: flex; align-items: center; gap: 14px; }
.me { font-weight: 700; color: var(--text-strong); }
.btn.sm { padding: 8px 14px; font-size: 13px; }

@media (max-width: 760px) {
  .inner { flex-wrap: wrap; height: auto; padding-top: 10px; padding-bottom: 10px; gap: 12px; }
  .nav { order: 3; flex-basis: 100%; gap: 14px; overflow-x: auto; }
}
</style>
