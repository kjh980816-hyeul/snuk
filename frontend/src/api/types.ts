export type Role = 'GUEST' | 'VIEWER' | 'STREAMER' | 'ADMIN'
export type CampaignStatus = 'SCHEDULED' | 'OPEN' | 'CLOSED'
export type DistributionType = 'FCFS' | 'APPROVAL'
export type KeyMode = 'QUANTITY' | 'UNIQUE_KEY'

export interface Me {
  id: number
  nickname: string
  profileImageUrl: string | null
  followerCount: number | null
  role: Role
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
}

export interface CollabGame {
  id: number
  name: string
  description: string | null
  thumbnailUrl: string | null
  gameLinkUrl: string | null
  reviewLinkUrl: string | null
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

export interface Tournament {
  id: number
  title: string
  description: string | null
  gameName: string | null
  bannerImageUrl: string | null
  eventDate: string | null
  applyStart: string | null
  applyEnd: string | null
  capacity: number
  filledSlots: number
  status: TournamentStatus
  resultText: string | null
  featured: boolean
  sortOrder: number
}

export interface MyParticipation {
  participantId: number
  tournamentId: number
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  appliedAt: string
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
