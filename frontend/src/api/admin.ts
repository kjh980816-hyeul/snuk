import api from './client'
import type { Campaign, CollabGame, ClientLogo, ContentVideo, Goods, Notice, OrderView, Spotlight, Tournament } from './types'

/** 어드민 API (ADMIN 토큰 필요). */
export const adminApi = {
  // campaigns
  createCampaign: (body: Partial<Campaign>) =>
    api.post<Campaign>('/api/admin/campaigns', body).then((r) => r.data),
  updateCampaign: (id: number, body: Partial<Campaign>) =>
    api.put<Campaign>(`/api/admin/campaigns/${id}`, body).then((r) => r.data),
  deleteCampaign: (id: number) => api.delete(`/api/admin/campaigns/${id}`),

  // keys
  registerKeys: (id: number, rawKeys: string) =>
    api.post<{ registered: number; duplicated: number; blank: number; totalAvailable: number }>(
      `/api/admin/campaigns/${id}/keys`, { rawKeys },
    ).then((r) => r.data),
  listKeys: (id: number) =>
    api.get<Array<{ id: number; maskedKey: string; status: string; assignedMemberId: number | null }>>(
      `/api/admin/campaigns/${id}/keys`,
    ).then((r) => r.data),
  deleteKey: (id: number, keyId: number) => api.delete(`/api/admin/campaigns/${id}/keys/${keyId}`),
  revokeKey: (id: number, keyId: number) => api.post(`/api/admin/campaigns/${id}/keys/${keyId}/revoke`),

  // applications
  applications: (id: number) =>
    api.get<Array<{ applicationId: number; memberId: number; nickname: string; profileImageUrl: string | null; status: string; followerSnapshot: number }>>(
      `/api/admin/campaigns/${id}/applications`,
    ).then((r) => r.data),
  approve: (applicationId: number) => api.post(`/api/admin/applications/${applicationId}/approve`),
  reject: (applicationId: number) => api.post(`/api/admin/applications/${applicationId}/reject`),

  // collab
  createGame: (body: Partial<CollabGame>) => api.post('/api/admin/collab/games', body),
  updateGame: (id: number, body: Partial<CollabGame>) => api.put(`/api/admin/collab/games/${id}`, body),
  deleteGame: (id: number) => api.delete(`/api/admin/collab/games/${id}`),
  createVideo: (body: Partial<ContentVideo>) => api.post('/api/admin/collab/videos', body),
  updateVideo: (id: number, body: Partial<ContentVideo>) => api.put(`/api/admin/collab/videos/${id}`, body),
  deleteVideo: (id: number) => api.delete(`/api/admin/collab/videos/${id}`),
  createLogo: (body: Partial<ClientLogo>) => api.post('/api/admin/collab/clients', body),
  updateLogo: (id: number, body: Partial<ClientLogo>) => api.put(`/api/admin/collab/clients/${id}`, body),
  deleteLogo: (id: number) => api.delete(`/api/admin/collab/clients/${id}`),

  // tournaments
  createTournament: (body: Partial<Tournament>) =>
    api.post<Tournament>('/api/admin/tournaments', body).then((r) => r.data),
  updateTournament: (id: number, body: Partial<Tournament>) =>
    api.put<Tournament>(`/api/admin/tournaments/${id}`, body).then((r) => r.data),
  deleteTournament: (id: number) => api.delete(`/api/admin/tournaments/${id}`),
  participants: (id: number) =>
    api.get<Array<{ participantId: number; memberId: number; nickname: string; profileImageUrl: string | null; status: string; followerSnapshot: number; answers: Array<{ text: string | null; imageUrl: string | null }> }>>(
      `/api/admin/tournaments/${id}/participants`,
    ).then((r) => r.data),
  approveParticipant: (participantId: number) => api.post(`/api/admin/participants/${participantId}/approve`),
  rejectParticipant: (participantId: number) => api.post(`/api/admin/participants/${participantId}/reject`),

  // goods / orders
  listGoods: () => api.get<Goods[]>('/api/admin/goods').then((r) => r.data),
  createGoods: (body: Partial<Goods>) => api.post<Goods>('/api/admin/goods', body).then((r) => r.data),
  updateGoods: (id: number, body: Partial<Goods>) =>
    api.put<Goods>(`/api/admin/goods/${id}`, body).then((r) => r.data),
  deleteGoods: (id: number) => api.delete(`/api/admin/goods/${id}`),
  orders: () => api.get<OrderView[]>('/api/admin/goods/orders').then((r) => r.data),

  // uploads
  uploadImage: (file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return api.post<{ url: string }>('/api/admin/uploads/image', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data)
  },

  // notices
  createNotice: (body: { title: string; content: string; pinned: boolean }) =>
    api.post<Notice>('/api/admin/notices', body).then((r) => r.data),
  updateNotice: (id: number, body: { title: string; content: string; pinned: boolean }) =>
    api.patch<Notice>(`/api/admin/notices/${id}`, body).then((r) => r.data),
  deleteNotice: (id: number) => api.delete(`/api/admin/notices/${id}`),

  // spotlights (승인제 — 승인 시각부터 2시간 노출)
  spotlights: () => api.get<Spotlight[]>('/api/admin/spotlights').then((r) => r.data),
  approveSpotlight: (id: number) => api.post(`/api/admin/spotlights/${id}/approve`),
  deleteSpotlight: (id: number) => api.delete(`/api/admin/spotlights/${id}`),

  // 후기 미작성 경고 로그
  reviewWarnings: () =>
    api.get<Array<{ applicationId: number; memberId: number; nickname: string; campaignId: number; campaignTitle: string; reviewDeadline: string; warnedAt: string; deadlineExtended: boolean; reviewWritten: boolean }>>(
      '/api/admin/review-warnings',
    ).then((r) => r.data),

  // 스트리머 게시판 신고함 (항목 3)
  postReports: () =>
    api.get<Array<{ reportId: number; postId: number; reason: string | null; reporterName: string; createdAt: string; postTitle: string; postContent: string; postAuthorName: string; streamerName: string }>>(
      '/api/admin/post-reports',
    ).then((r) => r.data),
  dismissPostReport: (reportId: number) => api.delete(`/api/admin/post-reports/${reportId}`),
  deleteReportedPost: (postId: number) => api.delete(`/api/streamer-posts/${postId}`),

  // 무료소스 자료실 (항목 19) — 링크(주소) 또는 파일 중 하나 필수
  createResource: (title: string, description: string, opts: { link?: string; file?: File | null; image?: File | null }) => {
    const fd = new FormData()
    fd.append('title', title)
    fd.append('description', description)
    if (opts.link) fd.append('link', opts.link)
    if (opts.file) fd.append('file', opts.file)
    if (opts.image) fd.append('image', opts.image)
    return api.post('/api/admin/resources', fd, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then((r) => r.data)
  },
  deleteResource: (id: number) => api.delete(`/api/admin/resources/${id}`),

  // settings / members / logs
  settings: () => api.get<Array<{ settingKey: string; settingValue: string; description: string | null }>>(
    '/api/admin/settings',
  ).then((r) => r.data),
  updateSetting: (key: string, value: string) =>
    api.put(`/api/admin/settings/${key}`, { value }),
  members: (page = 0, size = 50) =>
    api.get<{ content: Array<{ id: number; provider: string; channelId: string; nickname: string; profileImageUrl: string | null; followerCount: number | null; role: string; roleOverridden: boolean; createdAt: string }> }>(
      '/api/admin/members', { params: { page, size } },
    ).then((r) => r.data),
  overrideRole: (memberId: number, role: string) =>
    api.post(`/api/admin/members/${memberId}/role-override`, { role }),
  clearOverride: (memberId: number) => api.delete(`/api/admin/members/${memberId}/role-override`),
  logs: (page = 0, size = 50) =>
    api.get(`/api/admin/logs`, { params: { page, size } }).then((r) => r.data),
}
