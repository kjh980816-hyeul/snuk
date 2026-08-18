/**
 * 치지직 라이브 인페이지 플레이어 (데모 livePlayer/bindHls 이식).
 *  - 치지직은 플레이어 전용 임베드가 없어 예전엔 전체 페이지를 확대 로드 후 크롭 → 휠 스크롤로 어긋나 pointer-events:none 으로
 *    화면을 고정했더니 클릭까지 막힘(뮤마랭 08-18). 이제는 /api/live/hls 로 받은 m3u8 을 hls.js 로 <video> 에 직접 재생.
 *    (치지직 CDN 은 CORS * — 서버 중계 불필요)
 *  - 클릭 = 소리 켜기/끄기, 더블클릭 = 전체화면. 컨트롤(소리·전체화면)은 데모 .lvctl 과 동일.
 *  - 재생주소를 못 받으면(19금/비공개/API 실패) onFallback 으로 알려 호출측이 예전 크롭 임베드로 폴백.
 * 홈 무대(home-snuk-init.js)와 /live(LiveView) 가 같이 쓴다 — window.__snukPlayer 로 노출.
 */
export type PlayerState = 'loading' | 'playing' | 'offline' | 'fallback' | 'error'

export interface PlayerHandle {
  destroy(): void
  toggleMute(): boolean
  fullscreen(): void
  readonly video: HTMLVideoElement | null
}

export interface PlayerOptions {
  onState?: (s: PlayerState, info?: HlsInfo) => void
  /** 재생주소가 없을 때(19금 등) 호출 — 호출측이 대체 렌더(크롭 임베드 등) */
  onFallback?: (info: HlsInfo) => void
  poster?: string
  muted?: boolean
}

export interface HlsInfo {
  live: boolean
  hlsUrl?: string
  title?: string
  viewers?: number
  adult?: boolean
  channelName?: string
}

const ICON_MUTED = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 5 6 9H2v6h4l5 4V5z"/><line x1="23" y1="9" x2="17" y2="15"/><line x1="17" y1="9" x2="23" y2="15"/></svg>'
const ICON_SOUND = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 5 6 9H2v6h4l5 4V5z"/><path d="M15.5 8.5a5 5 0 0 1 0 7"/><path d="M19 5a9 9 0 0 1 0 14"/></svg>'
const ICON_EXPAND = '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"><path d="M8 3H3v5M16 3h5v5M8 21H3v-5M16 21h5v-5"/></svg>'

type HlsCtor = typeof import('hls.js').default
let hlsLib: Promise<HlsCtor> | null = null
function loadHls() {
  if (!hlsLib) hlsLib = import('hls.js').then((m) => m.default)
  return hlsLib
}

export async function fetchHlsInfo(channelId: string): Promise<HlsInfo> {
  try {
    const r = await fetch(`/api/live/hls?channel=${encodeURIComponent(channelId)}`, { cache: 'no-store' })
    if (!r.ok) return { live: false }
    return (await r.json()) as HlsInfo
  } catch {
    return { live: false }
  }
}

/** host(position:relative 컨테이너) 안에 플레이어를 그린다. */
export function mountChzzkPlayer(host: HTMLElement, channelId: string, opts: PlayerOptions = {}): PlayerHandle {
  let destroyed = false
  let hls: InstanceType<HlsCtor> | null = null
  let video: HTMLVideoElement | null = null
  let retryTimer: number | undefined
  let clickTimer: number | undefined
  const state = (s: PlayerState, info?: HlsInfo) => { if (!destroyed) opts.onState?.(s, info) }

  host.classList.add('snuk-player')
  host.innerHTML = `
    ${opts.poster ? `<img class="sp-poster" src="${opts.poster.replace(/"/g, '&quot;')}" alt="" onerror="this.remove()">` : ''}
    <video class="sp-video" autoplay playsinline ${opts.muted === false ? '' : 'muted'}></video>
    <span class="sp-load">방송을 불러오는 중…</span>
    <span class="sp-ctl">
      <button type="button" class="sp-btn sp-mute">${ICON_MUTED}<span>소리 켜기</span></button>
      <button type="button" class="sp-btn sp-full" title="전체화면">${ICON_EXPAND}</button>
    </span>`
  video = host.querySelector('video')
  const loadEl = host.querySelector<HTMLElement>('.sp-load')!
  const muteBtn = host.querySelector<HTMLButtonElement>('.sp-mute')!
  const fullBtn = host.querySelector<HTMLButtonElement>('.sp-full')!
  const v = video!

  const paintMute = () => {
    const on = !v.muted
    muteBtn.classList.toggle('on', on)
    muteBtn.innerHTML = on ? `${ICON_SOUND}<span>소리 켜짐</span>` : `${ICON_MUTED}<span>소리 켜기</span>`
  }
  const toggleMute = () => {
    v.muted = !v.muted
    if (!v.muted) { v.volume = 1; v.play().catch(() => {}) }
    paintMute()
    return !v.muted
  }
  const fullscreen = () => {
    if (document.fullscreenElement) { document.exitFullscreen().catch(() => {}); return }
    const anyV = v as HTMLVideoElement & { webkitEnterFullscreen?: () => void }
    if (host.requestFullscreen) host.requestFullscreen().catch(() => {})
    else if (anyV.webkitEnterFullscreen) anyV.webkitEnterFullscreen()
  }
  muteBtn.onclick = (e) => { e.stopPropagation(); toggleMute() }
  fullBtn.onclick = (e) => { e.stopPropagation(); fullscreen() }
  // 화면 클릭 = 소리 토글(뮤트 자동재생이라 가장 기대되는 동작), 더블클릭 = 전체화면
  v.addEventListener('click', (e) => {
    e.stopPropagation()
    window.clearTimeout(clickTimer)
    clickTimer = window.setTimeout(() => toggleMute(), 220)
  })
  v.addEventListener('dblclick', (e) => { e.stopPropagation(); window.clearTimeout(clickTimer); fullscreen() })

  const setPlaying = (on: boolean) => {
    host.classList.toggle('playing', on)
    loadEl.style.display = on ? 'none' : ''
    if (on) state('playing')
  }
  v.addEventListener('playing', () => setPlaying(true))
  v.addEventListener('play', () => setPlaying(true))
  // 라이브라 일시정지 개념이 없음 — 브라우저가 멈추면 다시 재생 (버퍼링 waiting 은 깜빡임 방지 위해 무시, 데모와 동일)
  v.addEventListener('pause', () => { if (!destroyed && !v.ended) v.play().catch(() => {}) })

  const attach = async (src: string) => {
    if (destroyed) return
    if (v.canPlayType('application/vnd.apple.mpegurl')) {
      // iOS 사파리 — 네이티브 HLS
      v.src = src
      v.play().catch(() => {})
      return
    }
    const Hls = await loadHls()
    if (destroyed) return
    if (!Hls.isSupported()) { v.src = src; v.play().catch(() => {}); return }
    hls?.destroy()
    hls = new Hls({ lowLatencyMode: true, enableWorker: true, backBufferLength: 30 })
    hls.on(Hls.Events.ERROR, (_e, data) => {
      if (!data.fatal || destroyed) return
      if (data.type === Hls.ErrorTypes.NETWORK_ERROR) {
        // 세그먼트/플레이리스트 실패 — 재생주소 만료 가능성 → 3초 뒤 주소 재조회
        loadEl.textContent = '연결이 끊겼습니다 · 다시 붙는 중'
        loadEl.style.display = ''
        window.clearTimeout(retryTimer)
        retryTimer = window.setTimeout(() => { void start() }, 3000)
      } else if (data.type === Hls.ErrorTypes.MEDIA_ERROR) {
        hls?.recoverMediaError()
      } else {
        state('error')
      }
    })
    hls.loadSource(src)
    hls.attachMedia(v)
    hls.on(Hls.Events.MANIFEST_PARSED, () => { v.play().catch(() => {}) })
  }

  const start = async () => {
    if (destroyed) return
    state('loading')
    const info = await fetchHlsInfo(channelId)
    if (destroyed) return
    if (!info.live) { loadEl.textContent = '지금은 방송 중이 아닙니다'; state('offline', info); return }
    if (!info.hlsUrl) {
      // 19금·비공개 등 재생주소 없음 → 호출측 폴백
      state('fallback', info)
      opts.onFallback?.(info)
      return
    }
    state('loading', info)
    await attach(info.hlsUrl)
  }
  paintMute()
  void start()

  return {
    get video() { return video },
    toggleMute,
    fullscreen,
    destroy() {
      destroyed = true
      window.clearTimeout(retryTimer)
      window.clearTimeout(clickTimer)
      try { hls?.destroy() } catch { /* noop */ }
      hls = null
      try { v.pause(); v.removeAttribute('src'); v.load() } catch { /* noop */ }
      video = null
      host.classList.remove('snuk-player', 'playing')
    },
  }
}

declare global {
  interface Window { __snukPlayer?: typeof mountChzzkPlayer; __snukHlsInfo?: typeof fetchHlsInfo }
}
window.__snukPlayer = mountChzzkPlayer
window.__snukHlsInfo = fetchHlsInfo
