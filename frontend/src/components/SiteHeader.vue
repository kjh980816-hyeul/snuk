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
    <div class="inner">
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
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  z-index: 50;
  width: var(--header-w, 220px);
  background: #fff;
  border-left: 1px solid #eee;
}
.inner {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 28px;
  height: 100%;
  padding: 28px 22px;
  overflow-y: auto;
}
.logo {
  font-weight: 900;
  font-size: 26px;
  color: var(--brand-yellow);
  -webkit-text-stroke: 1px #1a1a1a;
  letter-spacing: 1px;
  text-align: center;
}
.nav { display: flex; flex-direction: column; gap: 14px; flex: 1; }
.nav-link { font-weight: 600; color: var(--text-body); }
.nav-link.router-link-exact-active { color: var(--text-strong); border-left: 3px solid var(--accent-orange); padding-left: 8px; }
.nav-link.admin { color: var(--label-red); }
.account { display: flex; flex-direction: column; align-items: stretch; gap: 12px; }
.me { font-weight: 700; color: var(--text-strong); text-align: center; }
.btn.sm { padding: 8px 14px; font-size: 13px; }

@media (max-width: 760px) {
  .site-header {
    position: sticky;
    top: 0;
    right: auto;
    bottom: auto;
    width: auto;
    border-left: none;
    border-bottom: 1px solid #eee;
  }
  .inner {
    flex-direction: row;
    flex-wrap: wrap;
    align-items: center;
    height: auto;
    padding: 10px 16px;
    gap: 12px;
    overflow-y: visible;
  }
  .logo { text-align: left; }
  .nav { flex-direction: row; order: 3; flex-basis: 100%; gap: 14px; overflow-x: auto; }
  .nav-link.router-link-exact-active { border-left: none; padding-left: 0; border-bottom: 2px solid var(--accent-orange); padding-bottom: 4px; }
  .account { flex-direction: row; align-items: center; gap: 14px; margin-left: auto; }
}
</style>
