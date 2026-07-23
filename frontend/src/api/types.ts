export type Role = 'GUEST' | 'VIEWER' | 'STREAMER' | 'REPORTER' | 'ADMIN'
export type CampaignStatus = 'SCHEDULED' | 'OPEN' | 'CLOSED'
export type DistributionType = 'FCFS' | 'APPROVAL'
export type KeyMode = 'QUANTITY' | 'UNIQUE_KEY'

export interface Me {
  id: number
  nickname: string
  profileImageUrl: string | null
  profileImageOverridden?: boolean
  followerCount: number | null
  role: Role
  points: number
}

export interface Campaign {
  id: number
  title: string
  description: string | null
  gameName: string | null
  promoImageUrl: string | null
  eventDate: string | null
  applyStart: string | null
  applyEnd: string | null
  status: CampaignStatus
  distributionType: DistributionType
  keyMode: KeyMode
  totalSlots: number
  filledSlots: number
  featured: boolean
  ownerMemberId: number | null
}

export interface CollabGame {
  id: number
  name: string
  description: string | null
  thumbnailUrl: string | null
  gameLinkUrl: string | null
  reviewLinkUrl: string | null
  campaignId: number | null
  sortOrder: number
}

export interface ContentVideo {
  id: number
  title: string
  videoUrl: string
  thumbnailUrl: string | null
  featured: boolean
  sortOrder: number
}

export interface ClientLogo {
  id: number
  name: string | null
  logoUrl: string
  linkUrl: string | null
  sortOrder: number
}

export interface Review {
  id: number
  campaignId: number | null
  memberId: number
  title: string
  content: string | null
  createdAt: string
}

export interface MyApplication {
  applicationId: number
  campaignId: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  hasAssignedKey: boolean
  assignedKey: string | null
  appliedAt: string
}

// ----- 대회 -----
export type TournamentStatus = 'SCHEDULED' | 'OPEN' | 'CLOSED' | 'DONE'

/** 참가 신청 질문(주최자 작성) — required=false 면 선택 항목. */
export interface ApplyQuestion { q: string; required: boolean }
/** 참가 신청 답변 — 텍스트/사진 중 하나 이상. */
export interface ApplyAnswer { text: string | null; imageUrl: string | null }

export interface Tournament {
  id: number
  title: string
  description: string | null
  gameName: string | null
  bannerImageUrl: string | null
  detailImageUrl: string | null
  eventDate: string | null
  applyStart: string | null
  applyEnd: string | null
  capacity: number
  filledSlots: number
  status: TournamentStatus
  resultText: string | null
  applyQuestions: ApplyQuestion[]
  featured: boolean
  sortOrder: number
  ownerMemberId: number | null
}

export interface MyParticipation {
  participantId: number
  tournamentId: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  appliedAt: string
}

/** 공개 로스터(승인된 참가자만). */
export interface ParticipantPublic {
  participantId: number
  nickname: string
  profileImageUrl: string | null
  provider: 'CHZZK' | 'CIME' | 'SOOP'
}

// ----- 스트리머(공개) -----
export interface StreamerPublic {
  id: number
  nickname: string
  profileImageUrl: string | null
  provider: 'CHZZK' | 'CIME' | 'SOOP'
  followerCount: number | null
  channelUrl: string | null
}

export interface StreamerProfile {
  streamer: StreamerPublic
  followCount: number
  following: boolean
}

export interface StreamerPost {
  id: number
  streamerId: number
  authorId: number
  authorName: string
  authorImageUrl: string | null
  title: string
  content: string | null
  createdAt: string
  deletable: boolean
}

// ----- 공지 -----
export interface Notice {
  id: number
  title: string
  content: string | null
  pinned: boolean
  createdAt: string
}

// ----- 스포트라이트 -----
export type SpotlightPlatform = 'CHZZK' | 'SOOP' | 'YOUTUBE'

export interface Spotlight {
  id: number
  title: string
  platform: SpotlightPlatform
  streamUrl: string
  streamerName: string
  streamerImageUrl: string | null
  approved: boolean
  createdAt: string
  expiresAt: string
  scheduledAt: string | null
}

// ----- 무료소스 자료실 (항목 19) -----
export interface FreeResource {
  id: number
  title: string
  description: string | null
  fileUrl: string | null
  imageUrl: string | null
  createdAt: string
}

// ----- 스눅 뉴스 (REPORTER+ 작성) -----
export interface News {
  id: number
  title: string
  content: string | null
  thumbnailUrl: string | null
  authorId: number
  authorName: string
  authorImageUrl: string | null
  createdAt: string
  updatedAt: string
}

// ----- 라이브(스트리머 일괄) -----
export interface StreamerLive {
  memberId: number
  live: boolean
  liveTitle: string
}

// ----- 마이페이지 -----
export interface MypageSummary {
  applications: Array<{
    applicationId: number
    campaignId: number
    campaignTitle: string
    status: 'PENDING' | 'APPROVED' | 'REJECTED'
    hasAssignedKey: boolean
    maskedKey: string | null
    appliedAt: string
    reviewDeadline: string | null
    deadlineExtended: boolean
    warned: boolean
    reviewWritten: boolean
  }>
  tournaments: Array<{
    participantId: number
    tournamentId: number
    tournamentTitle: string
    status: 'PENDING' | 'APPROVED' | 'REJECTED'
    appliedAt: string
  }>
  reviews: Array<{
    postId: number
    campaignId: number | null
    title: string
    hidden: boolean
    createdAt: string
  }>
  orders: Array<{
    orderId: number
    goodsName: string
    quantity: number
    totalAmount: number
    status: OrderStatus
    createdAt: string
  }>
}

// ----- 굿즈/결제 -----
export type GoodsStatus = 'ACTIVE' | 'HIDDEN'
export type OrderStatus = 'PENDING' | 'PAID' | 'CANCELLED' | 'FAILED'

export interface Goods {
  id: number
  name: string
  description: string | null
  imageUrl: string | null
  price: number
  stock: number
  status: GoodsStatus
  sortOrder: number
  purchasable: boolean
}

export interface OrderCreateRequest {
  goodsId: number
  quantity: number
  receiverName: string
  receiverPhone: string
  zipcode?: string
  address: string
  addressDetail?: string
  memo?: string
}

/** 주문 생성 응답 = PortOne 결제창 호출에 필요한 값. */
export interface OrderResponse {
  orderId: number
  paymentId: string
  orderName: string
  amount: number
  status: OrderStatus
  storeId: string
  channelKey: string
}

export interface OrderView {
  id: number
  paymentId: string
  memberId: number
  goodsId: number
  goodsName: string
  quantity: number
  unitPrice: number
  totalAmount: number
  status: OrderStatus
  receiverName: string
  receiverPhone: string
  zipcode: string | null
  address: string
  addressDetail: string | null
  memo: string | null
  paidAt: string | null
  createdAt: string
}
