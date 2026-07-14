/**
 * 시안(home-snuk-init.js) 렌더러가 소비하는 window.__SNUK_DATA 를 실제 API 로 구성.
 * 시안 더미 배열과 동일한 필드 계약을 유지하되, 실 데이터의 id/액션 정보를 추가한다.
 * 실패한 소스는 빈 배열로 두고 나머지는 정상 노출(부분 실패 허용).
 */
import { campaignApi, collabApi, goodsApi, liveApi, newsApi, noticeApi, siteSettingsApi, spotlightApi, streamerApi, tournamentApi } from '@/api'
import type {
  Campaign, CollabGame, ContentVideo, Goods, News, Notice, ParticipantPublic, Review, Spotlight,
  StreamerLive, StreamerPublic, Tournament,
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
  status: 'open' | 'ongoing' | 'closed'
  statusLabel: string
  img: string | null
  eventDate: string | null
  resultText?: string | null
  /** 관리자 등록 여부(스눅 공식) — 홈 큰 카드는 관리자 컨텐츠만 */
  adminMade?: boolean
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
  applyOpen: boolean
  reviewsCount: number
  reviews: Array<{ name: string; title: string; text: string }>
}

export interface SnukData {
  snukContents: SnukCard[]
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
  const status = c.status === 'OPEN' ? 'open' : c.status === 'SCHEDULED' ? 'ongoing' : 'closed'
  return {
    id: c.id, kind: 'campaign', title: c.title, desc: c.description ?? c.gameName ?? '',
    max: c.totalSlots, filled: c.filledSlots, status,
    statusLabel: c.status === 'OPEN' ? '모집중' : c.status === 'SCHEDULED' ? '오픈예정' : '마감',
    img: c.promoImageUrl, eventDate: c.eventDate,
    adminMade: c.ownerMemberId == null, // 스눅 공식(관리자 등록) — 스트리머 등록은 작게만
  }
}

function tournamentCard(t: Tournament): SnukCard {
  const status = t.status === 'OPEN' ? 'open' : t.status === 'SCHEDULED' ? 'ongoing' : 'closed'
  return {
    id: t.id, kind: 'tournament', title: t.title, desc: t.description ?? t.gameName ?? '',
    max: t.capacity, filled: t.filledSlots, status,
    statusLabel: t.status === 'OPEN' ? '모집중' : t.status === 'SCHEDULED' ? '오픈예정'
      : t.status === 'DONE' ? '종료' : '마감',
    img: t.bannerImageUrl, eventDate: t.eventDate, resultText: t.resultText,
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
  const [campaigns, tournaments, videos, goods, clients, games, notices, spotlights, streamers, siteSettings, news, liveStreamers] = await Promise.all([
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
  ])
  const liveById = new Map(liveStreamers.map((l) => [l.memberId, l]))

  // 게임체험단: 콜라보 게임 ↔ 연결된 캠페인(V6) + 후기 3건 미리보기
  const campaignById = new Map(campaigns.map((c) => [c.id, c]))
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
      applyOpen: linked?.status === 'OPEN',
      reviewsCount: reviews.length,
      reviews: reviews.slice(0, 3).map((r) => ({
        name: `참가자 #${r.memberId}`, title: r.title, text: r.content ?? r.title,
      })),
    }
  }))

  // 챔피언십 로스터: featured(없으면 첫) 대회의 승인 참가자
  const rosterTarget = tournaments.find((t) => t.featured) ?? tournaments[0]
  const participants = rosterTarget
    ? await safe<ParticipantPublic[]>(tournamentApi.participants(rosterTarget.id), [])
    : []

  // 게임체험단 연계 캠페인(키 배포용)은 컨텐츠 목록에서 제외 — 게임체험단 섹션에서만 노출
  const gameLinkedIds = new Set(games.map((g) => g.campaignId).filter((id) => id != null))
  const pureCampaigns = campaigns.filter((c) => !gameLinkedIds.has(c.id))

  const featuredCampaign = pureCampaigns.find((c) => c.featured) ?? null
  const featuredTournament = tournaments.find((t) => t.featured) ?? null

  return {
    snukContents: pureCampaigns.map(campaignCard),
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
  }
}
