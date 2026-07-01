<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const roleLabel: Record<string, string> = {
  VIEWER: '시청자', STREAMER: '스트리머', ADMIN: '대표(관리자)', GUEST: '게스트',
}
</script>

<template>
  <div class="wrap mypage section" v-if="auth.me">
    <h2 class="section-label">마이페이지</h2>
    <div class="profile">
      <div class="avatar">
        <img v-if="auth.me.profileImageUrl" :src="auth.me.profileImageUrl" alt="프로필" />
        <div v-else class="placeholder">프로필</div>
      </div>
      <div class="info">
        <p class="nick">{{ auth.me.nickname }}</p>
        <p class="row">등급 · <strong>{{ roleLabel[auth.me.role] ?? auth.me.role }}</strong></p>
        <p class="row" v-if="auth.me.followerCount !== null">팔로워 · {{ auth.me.followerCount?.toLocaleString() }}</p>
      </div>
    </div>
    <!-- MY-01(내 신청·받은 키·내 후기 집계)은 발주자 확정 후 구현 예정(미확정 기능은 더미 대신 미표시) -->
    <p class="note">신청 현황·받은 키·후기 집계 화면은 항목 확정 후 제공됩니다.</p>
  </div>
</template>

<style scoped>
.profile { display: flex; gap: 20px; align-items: center; }
.avatar { width: 88px; height: 88px; border-radius: 50%; overflow: hidden; flex: none; }
.avatar img { width: 100%; height: 100%; object-fit: cover; }
.avatar .placeholder { width: 100%; height: 100%; border-radius: 50%; }
.info .nick { font-size: 22px; font-weight: 800; color: var(--text-strong); margin: 0 0 6px; }
.info .row { margin: 2px 0; color: var(--text-body); }
.note { margin-top: 30px; color: var(--text-muted); font-size: 14px; }
</style>
