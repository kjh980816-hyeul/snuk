import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: () => import('@/views/HomeView.vue') },
    { path: '/campaigns', name: 'campaigns', component: () => import('@/views/CampaignsView.vue') },
    { path: '/videos', name: 'videos', component: () => import('@/views/VideosView.vue') },
    { path: '/collab', name: 'collab', component: () => import('@/views/CollabView.vue') },
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
