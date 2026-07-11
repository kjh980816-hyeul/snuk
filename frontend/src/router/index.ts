import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
    { path: '/campaigns', name: 'campaigns', component: () => import('@/views/CampaignsView.vue') },
    { path: '/campaigns/:id/reviews', name: 'campaign-reviews', component: () => import('@/views/ReviewBoardView.vue') },
    { path: '/videos', name: 'videos', component: () => import('@/views/VideosView.vue') },
    { path: '/games', name: 'games', component: () => import('@/views/GameTrialView.vue') },
    { path: '/live', name: 'live', component: () => import('@/views/LiveView.vue') },
    { path: '/streamers', name: 'streamers', component: () => import('@/views/StreamersView.vue') },
    { path: '/streamers/:id', name: 'streamer-profile', component: () => import('@/views/StreamerProfileView.vue') },
    // 구 경로 → 시안 페이지로 통합
    { path: '/collab', redirect: '/campaigns' },
    { path: '/tournaments', redirect: '/championship' },
    { path: '/championship', name: 'championship', component: () => import('@/views/ChampionshipView.vue') },
    { path: '/championship/:id', name: 'tournament-detail', component: () => import('@/views/TournamentDetailView.vue') },
    { path: '/goods', name: 'goods', component: () => import('@/views/GoodsView.vue') },
    { path: '/clients', name: 'clients', component: () => import('@/views/ClientsView.vue') },
    { path: '/oauth/callback', name: 'oauth-callback', component: () => import('@/views/OAuthCallbackView.vue') },
    { path: '/mypage', name: 'mypage', component: () => import('@/views/MyPageView.vue'), meta: { auth: true } },
    { path: '/admin', name: 'admin', component: () => import('@/views/AdminView.vue'), meta: { admin: true } },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

// 프론트 가드는 UX용(실제 인가는 백엔드 강제 — security.md)
router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (!auth.loaded) {
    await auth.fetchMe()
  }
  if (to.meta.admin && !auth.isAdmin) {
    return { name: 'home' }
  }
  if (to.meta.auth && !auth.isLoggedIn) {
    return { name: 'home' }
  }
  return true
})

export default router
