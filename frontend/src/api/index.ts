import api from './client'
import type {
  Campaign, ClientLogo, CollabGame, ContentVideo, Goods, Me, MyApplication,
  MyParticipation, MypageSummary, News, Notice, OrderCreateRequest, OrderResponse, OrderView,
  ParticipantPublic, Review, Spotlight, SpotlightPlatform, StreamerLive, StreamerPost,
  StreamerProfile, StreamerPublic, Tournament,
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

// ----- site settings (공개 화이트리스트 키 — 라이브 채널/배너 이미지) -----
export const siteSettingsApi = {
  get: () => api.get<Record<string, string>>('/api/site-settings').then((r) => r.data),
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
  // 스트리머 본인 컨텐츠 등록/수정/삭제 (STREAMER+, 소유자 또는 ADMIN)
  create: (body: Record<string, unknown>) => api.post<Campaign>('/api/campaigns', body).then((r) => r.data),
  update: (id: number, body: Record<string, unknown>) =>
    api.put<Campaign>(`/api/campaigns/${id}`, body).then((r) => r.data),
  remove: (id: number) => api.delete(`/api/campaigns/${id}`),
}

// ----- 스트리머 이미지 업로드 (STREAMER+) -----
export const uploadApi = {
  image: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post<{ url: string }>('/api/uploads/image', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data)
  },
}

// ----- mypage (로그인 필요) -----
export const mypageApi = {
  summary: () => api.get<MypageSummary>('/api/mypage/summary').then((r) => r.data),
  /** 후기 마감 7일 연장(게임당 1회) */
  extendDeadline: (applicationId: number) =>
    api.post<{ applicationId: number; reviewDeadline: string }>(
      `/api/mypage/applications/${applicationId}/extend`).then((r) => r.data),
}

// ----- 스눅 뉴스 (조회 공개, 작성 REPORTER+) -----
export const newsApi = {
  list: () => api.get<News[]>('/api/news').then((r) => r.data),
  detail: (id: number) => api.get<News>(`/api/news/${id}`).then((r) => r.data),
  write: (body: { title: string; content: string; thumbnailUrl?: string | null }) =>
    api.post<News>('/api/news', body).then((r) => r.data),
  edit: (id: number, body: { title: string; content: string; thumbnailUrl?: string | null }) =>
    api.put<News>(`/api/news/${id}`, body).then((r) => r.data),
  remove: (id: number) => api.delete(`/api/news/${id}`),
  uploadImage: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post<{ url: string }>('/api/news/upload-image', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data)
  },
}

// ----- 라이브 상태 (public) -----
export const liveApi = {
  status: () => api.get<{ live: boolean; liveTitle: string }>('/api/live/status').then((r) => r.data),
  streamers: () => api.get<StreamerLive[]>('/api/live/streamers').then((r) => r.data),
}

// ----- tournaments (public) -----
export const tournamentApi = {
  list: () => api.get<Tournament[]>('/api/tournaments').then((r) => r.data),
  detail: (id: number) => api.get<Tournament>(`/api/tournaments/${id}`).then((r) => r.data),
  apply: (id: number) =>
    api.post<{ participantId: number; status: string }>(`/api/tournaments/${id}/apply`).then((r) => r.data),
  myParticipation: (id: number) =>
    api.get<MyParticipation>(`/api/tournaments/${id}/my-participation`).then((r) => r.data),
  participants: (id: number) =>
    api.get<ParticipantPublic[]>(`/api/tournaments/${id}/participants`).then((r) => r.data),
  // 스트리머 본인 대회 등록/수정/삭제 (STREAMER+, 소유자 또는 ADMIN)
  create: (body: Record<string, unknown>) => api.post<Tournament>('/api/tournaments', body).then((r) => r.data),
  update: (id: number, body: Record<string, unknown>) =>
    api.put<Tournament>(`/api/tournaments/${id}`, body).then((r) => r.data),
  remove: (id: number) => api.delete(`/api/tournaments/${id}`),
  // 주최자 참가자 관리 (소유 스트리머 또는 ADMIN)
  manageParticipants: (id: number) =>
    api.get<Array<{
      participantId: number; memberId: number; nickname: string; profileImageUrl: string | null
      followerSnapshot: number; status: string; appliedAt: string
    }>>(`/api/tournaments/${id}/participants/manage`).then((r) => r.data),
  approveParticipant: (id: number, pid: number) =>
    api.post(`/api/tournaments/${id}/participants/${pid}/approve`),
  rejectParticipant: (id: number, pid: number) =>
    api.post(`/api/tournaments/${id}/participants/${pid}/reject`),
}

// ----- 스트리머 (public + 프로필/팔로우/개인 게시판) -----
export const streamerApi = {
  list: () => api.get<StreamerPublic[]>('/api/streamers').then((r) => r.data),
  profile: (id: number) => api.get<StreamerProfile>(`/api/streamers/${id}`).then((r) => r.data),
  follow: (id: number) =>
    api.post<{ following: boolean; followCount: number }>(`/api/streamers/${id}/follow`).then((r) => r.data),
  unfollow: (id: number) =>
    api.delete<{ following: boolean; followCount: number }>(`/api/streamers/${id}/follow`).then((r) => r.data),
  posts: (id: number) => api.get<StreamerPost[]>(`/api/streamers/${id}/posts`).then((r) => r.data),
  writePost: (id: number, body: { title: string; content: string }) =>
    api.post<StreamerPost>(`/api/streamers/${id}/posts`, body).then((r) => r.data),
  deletePost: (postId: number) => api.delete(`/api/streamer-posts/${postId}`),
}

// ----- 공지 (public) -----
export const noticeApi = {
  list: (limit = 5) => api.get<Notice[]>('/api/notices', { params: { limit } }).then((r) => r.data),
}

// ----- 스포트라이트 -----
export const spotlightApi = {
  active: () => api.get<Spotlight[]>('/api/spotlights/active').then((r) => r.data),
  create: (body: { title: string; platform: SpotlightPlatform; streamUrl: string }) =>
    api.post<Spotlight>('/api/spotlights', body).then((r) => r.data),
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
