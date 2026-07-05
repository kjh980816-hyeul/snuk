import api from './client'
import type {
  Campaign, ClientLogo, CollabGame, ContentVideo, Goods, Me, MyApplication,
  MyParticipation, MypageSummary, OrderCreateRequest, OrderResponse, OrderView, Review, Tournament,
} from './types'

// ----- auth -----
export const authApi = {
  me: () => api.get<Me>('/api/auth/me').then((r) => r.data),
  // 프사 파일 업로드
  uploadProfileImage: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post<Me>('/api/auth/me/profile-image', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data)
  },
  // null/빈값이면 치지직 프사로 복원
  updateProfileImage: (imageUrl: string | null) =>
    api.patch<Me>('/api/auth/me/profile-image', { imageUrl }).then((r) => r.data),
  refresh: (refreshToken: string) =>
    api.post('/api/auth/refresh', { refreshToken }).then((r) => r.data),
  logout: () => api.post('/api/auth/logout'),
}

// ----- campaigns (public) -----
export const campaignApi = {
  list: () => api.get<Campaign[]>('/api/campaigns').then((r) => r.data),
  detail: (id: number) => api.get<Campaign>(`/api/campaigns/${id}`).then((r) => r.data),
  apply: (id: number) =>
    api.post<{ applicationId: number; status: string }>(`/api/campaigns/${id}/apply`).then((r) => r.data),
  myApplication: (id: number, reveal = false) =>
    api.get<MyApplication>(`/api/campaigns/${id}/my-application`, { params: { reveal } }).then((r) => r.data),
  reviews: (id: number) => api.get<Review[]>(`/api/campaigns/${id}/reviews`).then((r) => r.data),
  writeReview: (id: number, body: { title: string; content: string }) =>
    api.post<Review>(`/api/campaigns/${id}/reviews`, body).then((r) => r.data),
}

// ----- mypage (로그인 필요) -----
export const mypageApi = {
  summary: () => api.get<MypageSummary>('/api/mypage/summary').then((r) => r.data),
}

// ----- tournaments (public) -----
export const tournamentApi = {
  list: () => api.get<Tournament[]>('/api/tournaments').then((r) => r.data),
  detail: (id: number) => api.get<Tournament>(`/api/tournaments/${id}`).then((r) => r.data),
  apply: (id: number) =>
    api.post<{ participantId: number; status: string }>(`/api/tournaments/${id}/apply`).then((r) => r.data),
  myParticipation: (id: number) =>
    api.get<MyParticipation>(`/api/tournaments/${id}/my-participation`).then((r) => r.data),
}

// ----- goods (public storefront) -----
export const goodsApi = {
  list: () => api.get<Goods[]>('/api/goods').then((r) => r.data),
  detail: (id: number) => api.get<Goods>(`/api/goods/${id}`).then((r) => r.data),
}

// ----- orders (로그인 필요) -----
export const orderApi = {
  create: (body: OrderCreateRequest) =>
    api.post<OrderResponse>('/api/orders', body).then((r) => r.data),
  confirm: (orderId: number) =>
    api.post<OrderView>(`/api/orders/${orderId}/confirm`).then((r) => r.data),
  mine: () => api.get<OrderView[]>('/api/orders/me').then((r) => r.data),
  detail: (orderId: number) => api.get<OrderView>(`/api/orders/${orderId}`).then((r) => r.data),
}

// ----- collab (public) -----
export const collabApi = {
  games: () => api.get<CollabGame[]>('/api/collab/games').then((r) => r.data),
  videos: () => api.get<ContentVideo[]>('/api/collab/videos').then((r) => r.data),
  clients: () => api.get<ClientLogo[]>('/api/collab/clients').then((r) => r.data),
  allReviews: () => api.get<Review[]>('/api/reviews').then((r) => r.data),
}
