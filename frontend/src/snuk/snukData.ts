/**
 * 시안(home-snuk-init.js) 렌더러가 소비하는 window.__SNUK_DATA 를 실제 API 로 구성.
 * 시안 더미 배열과 동일한 필드 계약을 유지하되, 실 데이터의 id/액션 정보를 추가한다.
 * 실패한 소스는 빈 배열로 두고 나머지는 정상 노출(부분 실패 허용).
 */
import { adApi, campaignApi, collabApi, communityApi, goodsApi, liveApi, newsApi, noticeApi, resourceApi, siteSettingsApi, spotlightApi, streamerApi, tournamentApi } from '@/api'
import type {
  ApplyQuestion, Campaign, CollabGame, CommunityPostSummary, ContentVideo, FreeResource, Goods, News, Notice,
  ParticipantPublic, Review, Spotlight, StreamerLive, StreamerPublic, Tournament,
  AdSlot,
} from '@/api/types'
import { GOODS_READY, OFFICIAL_CHZZK_CHANNEL_ID } from '@/config'

// ---------- 시안 계약 타입 ----------

export interface SnukCard {
  id: number
  kind: 'campaign' | 'tournament'
  title: string
  desc: string
  max: number
  filled: number
  /** preparing=준비중(내용만 공개) / open=모집중 / upcoming=오픈예정 / ongoing=모집 마감·진행중 / closed=종료 */
  status: 'preparing' | 'open' | 'upcoming' | 'ongoing' | 'closed'
  statusLabel: string
  /** 서버 판정 — 지금 신청 가능(카드 버튼·클릭 액션은 이 값 기준) */
  applyOpen: boolean
  /** 서버 판정 — 준비중이면 모집 인원·기간·D-day 등 모집 정보를 숨기고 내용만 보여준다 */
  preparing: boolean
  img: string | null
  eventDate: string | null
  /** 신청 기간(데모 카드 스펙 줄) */
  applyStart: string | null
  applyEnd: string | null
  resultText?: string | null
  /** 관리자 등록 여부(스눅 공식) — 홈 큰 카드는 관리자 컨텐츠만 */
  adminMade?: boolean
  /** 등록 스트리머 id (본인 수정/삭제 버튼 노출용) */
  ownerId?: number | null
  /** 신청 질문(주최자 작성) — 있으면 신청 모달에 답변 입력 노출 */
  applyQuestions?: ApplyQuestion[]
  /** 게임체험단 연계 캠페인(컨텐츠 페이지 "체험단" 종류) */
  trial?: boolean
}

export interface SnukGame {
  campaignId: number | null
  gameId: number
  name: string
  publisher: string
  desc: string
  img: string | null
  gameLinkUrl: string | null
  members: number
  max: number
  /** 서버 판정 — 연결 캠페인이 OPEN */
  applyOpen: boolean
  /** 서버 판정 — 연결 캠페인이 준비중(내용만 공개, 수량·마감·신청 숨김) */
  preparing: boolean
  /** 연결 캠페인의 모집 마감일(YYYY-MM-DD, 없으면 null) */
  applyEnd: string | null
  /** 선발 방식 — 데모 체험단 카드의 두 번째 태그 */
  pick: '선착순' | '선정'
  reviewsCount: number
  reviews: Array<{ name: string; title: string; text: string }>
  /** 연결 캠페인의 신청 질문 — 있으면 신청 시 답변 입력 */
  applyQuestions: ApplyQuestion[]
}

export interface SnukData {
  snukContents: SnukCard[]
  /** 게임 연계 캠페인(체험단) — 컨텐츠 페이지 상태 탭에서만 사용 */
  trialContents: SnukCard[]
  snukFeatured: SnukCard | null
  mugContents: SnukCard[]
  mugFeatured: SnukCard | null
  games: SnukGame[]
  videos: Array<{ id: number; title: string; meta: string; ytId: string | null; thumb: string | null; url: string }>
  goods: Array<{
    id: number; name: string; streamer: string; price: string; img: string | null
    status: 'ongoing' | 'closed'; purchasable: boolean
  }>
  goodsReady: boolean
  partners: Array<{ name: string; logoUrl: string; linkUrl: string | null }>
  notices: Array<{ id: number; title: string; date: string; content: string }>
  spotlights: Array<{ name: string; sub: string; platform: 'chz' | 'soop' | 'yt'; img: string | null; url: string }>
  roster: Array<{ name: string; img: string | null; platform: 'chz' | 'soop' | 'cime'; streamUrl: string | null }>
  rosterTournamentTitle: string
  streamers: Array<{
    id: number; name: string; img: string | null; platform: 'chz' | 'soop' | 'cime'
    followers: number | null; channelUrl: string | null
    live: boolean; liveTitle: string
  }>
  news: Array<{
    id: number; title: string; author: string; authorImg: string | null
    date: string; thumb: string | null; excerpt: string
  }>
  chzzkChannelId: string
  /** 어드민 "설정" 탭에서 관리하는 공개 설정 (배너/히어로 이미지 등, '-'=미설정) */
  siteSettings: Record<string, string>
  /** 홈 상단 AD 배너 슬라이드 — 광고 슬롯(어드민 등록, 노출 중인 것만). 비어 있으면 렌더러가 사이트 이미지로 폴백 */
  ads: Array<{ id: number; title: string; img: string; link: string | null }>
  /** 방송도우미 무료소스 (홈 "방송도우미 인기 소스" 줄) */
  resources: Array<{ id: number; title: string; desc: string; img: string | null; url: string | null; date: string }>
  /** 체험단 후기 (홈 "체험단 후기" 카드) */
  trialReviews: Array<{
    id: number; campaignId: number | null; campaignTitle: string; publisher: string
    img: string | null; videoUrl: string | null; title: string; content: string
    author: string; date: string
  }>
  /** 커뮤니티 인기글 (홈 "커뮤니티 인기글" 목록) */
  communityPosts: Array<{ id: number; boardName: string; title: string; date: string; comments: number }>
}

// ---------- 매핑 ----------

function ytIdOf(url: string): string | null {
  const m = url.match(/(?:youtu\.be\/|[?&]v=|\/embed\/|\/shorts\/)([\w-]{11})/)
  return m ? m[1] : null
}

function dateOf(iso: string | null): string {
  if (!iso) return ''
  return iso.slice(0, 10).split('-').join('.')
}

function campaignCard(c: Campaign): SnukCard {
  const status = c.status === 'PREPARING' ? 'preparing' : c.status === 'OPEN' ? 'open'
    : c.status === 'SCHEDULED' ? 'upcoming' : c.status === 'ONGOING' ? 'ongoing' : 'closed'
  return {
    id: c.id, kind: 'campaign', title: c.title, desc: c.description ?? c.gameName ?? '',
    max: c.totalSlots, filled: c.filledSlots, status,
    statusLabel: c.status === 'PREPARING' ? '준비중' : c.status === 'OPEN' ? '모집중'
      : c.status === 'SCHEDULED' ? '오픈예정' : c.status === 'ONGOING' ? '진행중' : '종료',
    // 서버 판정값(응답에 없는 구버전 캐시 대비 status 폴백)
    applyOpen: c.applyOpen ?? c.status === 'OPEN',
    preparing: c.preparing ?? c.status === 'PREPARING',
    img: c.promoImageUrl, eventDate: c.eventDate,
    applyStart: c.applyStart ? c.applyStart.slice(0, 10) : null,
    applyEnd: c.applyEnd ? c.applyEnd.slice(0, 10) : null,
    adminMade: c.ownerMemberId == null, // 스눅 공식(관리자 등록) — 스트리머 등록은 작게만
    ownerId: c.ownerMemberId,
    applyQuestions: c.applyQuestions ?? [],
  }
}

function tournamentCard(t: Tournament): SnukCard {
  // 대회: CLOSED=모집 마감(대회 진행 전·중) → 진행중, DONE=종료
  const status = t.status === 'PREPARING' ? 'preparing' : t.status === 'OPEN' ? 'open'
    : t.status === 'SCHEDULED' ? 'upcoming' : t.status === 'CLOSED' ? 'ongoing' : 'closed'
  return {
    id: t.id, kind: 'tournament', title: t.title, desc: t.description ?? t.gameName ?? '',
    max: t.capacity, filled: t.filledSlots, status,
    statusLabel: t.status === 'PREPARING' ? '준비중' : t.status === 'OPEN' ? '모집중'
      : t.status === 'SCHEDULED' ? '오픈예정' : t.status === 'DONE' ? '종료' : '진행중',
    applyOpen: t.applyOpen ?? t.status === 'OPEN',
    preparing: t.preparing ?? t.status === 'PREPARING',
    img: t.bannerImageUrl, eventDate: t.eventDate,
    applyStart: t.applyStart ? t.applyStart.slice(0, 10) : null,
    applyEnd: t.applyEnd ? t.applyEnd.slice(0, 10) : null,
    resultText: t.resultText,
    adminMade: t.ownerMemberId == null, // 스눅 공식(관리자 등록) — 스트리머 등록은 작게만
    ownerId: t.ownerMemberId,
    applyQuestions: t.applyQuestions ?? [],
  }
}

const PROVIDER_PLAT: Record<string, 'chz' | 'soop' | 'cime'> = { CHZZK: 'chz', SOOP: 'soop', CIME: 'cime' }
const SPOTLIGHT_PLAT: Record<string, 'chz' | 'soop' | 'yt'> = { CHZZK: 'chz', SOOP: 'soop', YOUTUBE: 'yt' }

async function safe<T>(p: Promise<T>, fallback: T): Promise<T> {
  try {
    return await p
  } catch {
    return fallback
  }
}

/** 전 페이지 공용 데이터 로드(공개 API만 — 로그인 불필요). */
export async function loadSnukData(): Promise<SnukData> {
  const [campaigns, tournaments, videos, goods, clients, games, notices, spotlights, streamers, siteSettings, news, liveStreamers, resources, allReviews, ads] = await Promise.all([
    safe<Campaign[]>(campaignApi.list(), []),
    safe<Tournament[]>(tournamentApi.list(), []),
    safe<ContentVideo[]>(collabApi.videos(), []),
    safe<Goods[]>(goodsApi.list(), []),
    safe(collabApi.clients(), []),
    safe<CollabGame[]>(collabApi.games(), []),
    safe<Notice[]>(noticeApi.list(5), []),
    safe<Spotlight[]>(spotlightApi.active(), []),
    safe<StreamerPublic[]>(streamerApi.list(), []),
    safe<Record<string, string>>(siteSettingsApi.get(), {}),
    safe<News[]>(newsApi.list(), []),
    safe<StreamerLive[]>(liveApi.streamers(), []),
    safe<FreeResource[]>(resourceApi.list(), []),
    safe<Review[]>(collabApi.allReviews(), []),
    safe<AdSlot[]>(adApi.list(), []),
  ])
  const liveById = new Map(liveStreamers.map((l) => [l.memberId, l]))

  // 게임체험단: 콜라보 게임 ↔ 연결된 캠페인(V6) + 후기 3건 미리보기
  const campaignById = new Map(campaigns.map((c) => [c.id, c]))
  const gameByCampaign = new Map(
    games.filter((g) => g.campaignId != null).map((g) => [g.campaignId as number, g]),
  )
  const games2: SnukGame[] = await Promise.all(games.map(async (g) => {
    const linked = g.campaignId != null ? campaignById.get(g.campaignId) : undefined
    const reviews = g.campaignId != null
      ? await safe<Review[]>(campaignApi.reviews(g.campaignId), [])
      : []
    return {
      campaignId: g.campaignId, gameId: g.id, name: g.name,
      publisher: linked?.gameName ?? '',
      desc: g.description ?? '',
      img: g.thumbnailUrl ?? linked?.promoImageUrl ?? null,
      gameLinkUrl: g.gameLinkUrl,
      members: linked?.filledSlots ?? 0,
      max: linked?.totalSlots ?? 0,
      applyOpen: linked ? (linked.applyOpen ?? linked.status === 'OPEN') : false,
      preparing: linked ? (linked.preparing ?? linked.status === 'PREPARING') : false,
      applyEnd: linked?.applyEnd ? linked.applyEnd.slice(0, 10) : null,
      pick: linked?.distributionType === 'FCFS' ? '선착순' : '선정',
      reviewsCount: reviews.length,
      reviews: reviews.slice(0, 3).map((r) => ({
        name: `참가자 #${r.memberId}`, title: r.title, text: r.content ?? r.title,
      })),
      applyQuestions: linked?.applyQuestions ?? [],
    }
  }))

  // 챔피언십 로스터: featured(없으면 첫) 대회의 승인 참가자
  const rosterTarget = tournaments.find((t) => t.featured) ?? tournaments[0]
  const participants = rosterTarget
    ? await safe<ParticipantPublic[]>(tournamentApi.participants(rosterTarget.id), [])
    : []

  // 홈 "커뮤니티 인기글" — 커뮤니티 인기글 API(댓글·조회수 가중치) 상위 5건
  const communityPosts = (await safe<CommunityPostSummary[]>(communityApi.popular(null, 5), []))
    .map((p) => ({
      id: p.id, boardName: p.boardName, title: p.title,
      date: dateOf(p.createdAt), comments: p.commentCount,
    }))

  // 게임체험단 연계 캠페인(키 배포용)은 컨텐츠 목록에서 제외 — 게임체험단 섹션에서만 노출
  const gameLinkedIds = new Set(games.map((g) => g.campaignId).filter((id) => id != null))
  const pureCampaigns = campaigns.filter((c) => !gameLinkedIds.has(c.id))

  // featured(홈 큰 칸)는 스눅 공식(관리자 등록)만 — 스트리머 등록분은 후보 제외
  const featuredCampaign = pureCampaigns.find((c) => c.featured && c.ownerMemberId == null) ?? null
  const featuredTournament = tournaments.find((t) => t.featured && t.ownerMemberId == null) ?? null

  // 컨텐츠 페이지(/campaigns) "체험단" 종류 — 게임 연계 캠페인을 카드로(썸네일은 게임 이미지 폴백)
  const trialContents: SnukCard[] = campaigns.filter((c) => gameLinkedIds.has(c.id)).map((c) => {
    const g = gameByCampaign.get(c.id)
    const card = campaignCard(c)
    return { ...card, trial: true, img: card.img ?? g?.thumbnailUrl ?? null, title: card.title || g?.name || '' }
  })

  return {
    snukContents: pureCampaigns.map(campaignCard),
    trialContents,
    snukFeatured: featuredCampaign ? campaignCard(featuredCampaign) : null,
    mugContents: tournaments.map(tournamentCard),
    mugFeatured: featuredTournament ? tournamentCard(featuredTournament) : null,
    games: games2,
    videos: videos.map((v) => ({
      id: v.id, title: v.title, meta: v.featured ? '추천 영상' : '', ytId: ytIdOf(v.videoUrl),
      thumb: v.thumbnailUrl ?? (ytIdOf(v.videoUrl) ? `https://img.youtube.com/vi/${ytIdOf(v.videoUrl)}/mqdefault.jpg` : null),
      url: v.videoUrl,
    })),
    goods: goods.map((g) => ({
      id: g.id, name: g.name, streamer: 'SNUK 공식',
      price: g.price.toLocaleString('ko-KR'), img: g.imageUrl,
      status: g.purchasable ? 'ongoing' : 'closed',
      purchasable: g.purchasable && GOODS_READY,
    })),
    goodsReady: GOODS_READY,
    partners: clients.map((c) => ({ name: c.name ?? '', logoUrl: c.logoUrl, linkUrl: c.linkUrl })),
    notices: notices.map((n) => ({
      id: n.id, title: n.title, date: dateOf(n.createdAt), content: n.content ?? '',
    })),
    spotlights: spotlights.map((s) => ({
      name: s.streamerName, sub: s.title,
      platform: SPOTLIGHT_PLAT[s.platform] ?? 'chz',
      img: s.streamerImageUrl, url: s.streamUrl,
      // 방송 예정 일시(선택) — "MM/DD HH:mm" 표기
      when: s.scheduledAt
        ? `${s.scheduledAt.slice(5, 10).replace('-', '/')} ${s.scheduledAt.slice(11, 16)}`
        : '',
    })),
    roster: participants.map((p) => ({
      name: p.nickname, img: p.profileImageUrl,
      platform: PROVIDER_PLAT[p.provider] ?? 'chz', streamUrl: null,
    })),
    rosterTournamentTitle: rosterTarget?.title ?? '',
    // 라이브 중인 스트리머 우선 노출(항목 7)
    streamers: streamers.map((s) => ({
      id: s.id, name: s.nickname, img: s.profileImageUrl,
      platform: PROVIDER_PLAT[s.provider] ?? 'chz',
      followers: s.followerCount, channelUrl: s.channelUrl,
      live: liveById.get(s.id)?.live ?? false,
      liveTitle: liveById.get(s.id)?.liveTitle ?? '',
    })).sort((a, b) => (b.live ? 1 : 0) - (a.live ? 1 : 0)),
    news: news.map((n) => ({
      id: n.id, title: n.title, author: n.authorName, authorImg: n.authorImageUrl,
      date: dateOf(n.createdAt), thumb: n.thumbnailUrl,
      excerpt: (n.content ?? '').replace(/\s+/g, ' ').slice(0, 80),
    })),
    chzzkChannelId:
      (siteSettings.LIVE_CHANNEL_ID && siteSettings.LIVE_CHANNEL_ID !== '-')
        ? siteSettings.LIVE_CHANNEL_ID
        : OFFICIAL_CHZZK_CHANNEL_ID,
    siteSettings,
    ads: ads.map((a) => ({ id: a.id, title: a.title ?? '', img: a.imageUrl, link: a.linkUrl })),
    resources: resources.map((r) => ({
      id: r.id, title: r.title, desc: r.description ?? '',
      img: r.imageUrl, url: r.fileUrl, date: dateOf(r.createdAt),
    })),
    trialReviews: allReviews.map((r) => {
      const c = r.campaignId != null ? campaignById.get(r.campaignId) : undefined
      const g = r.campaignId != null ? gameByCampaign.get(r.campaignId) : undefined
      return {
        id: r.id, campaignId: r.campaignId,
        campaignTitle: g?.name ?? c?.title ?? '체험단',
        publisher: c?.gameName ?? '',
        img: g?.thumbnailUrl ?? c?.promoImageUrl ?? null,
        videoUrl: g?.reviewLinkUrl ?? null,
        title: r.title, content: r.content ?? '',
        author: `참가자 #${r.memberId}`, date: dateOf(r.createdAt),
      }
    }),
    communityPosts,
  }
}
