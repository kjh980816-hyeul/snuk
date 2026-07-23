<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { OFFICIAL_CHZZK_CHANNEL_ID } from '@/config'
import { siteSettingsApi } from '@/api'

// 생방송: SNUK 공식 치지직 채널 라이브 임베드 (chzzk.naver.com/live/{채널ID})
// 채널 ID·배너 이미지·문구는 어드민 "설정" 탭(LIVE_CHANNEL_ID / BANNER_LIVE_*)에서 관리
const channelId = ref(OFFICIAL_CHZZK_CHANNEL_ID)
const loaded = ref(false)
const isLive = ref(false)
const liveTitle = ref('')

const bannerUrl = ref('https://images.unsplash.com/photo-1598550476439-6847785fcea6?w=1600&q=80')
const bannerTitle = ref('SNUK LIVE')
const bannerSub = ref('SNUK 공식 채널의 실시간 방송을 만나보세요')
const bannerBroken = ref(false)

onMounted(async () => {
  try {
    const s = await siteSettingsApi.get()
    if (s.LIVE_CHANNEL_ID && s.LIVE_CHANNEL_ID !== '-') channelId.value = s.LIVE_CHANNEL_ID
    if (s.BANNER_LIVE_URL && s.BANNER_LIVE_URL !== '-') bannerUrl.value = s.BANNER_LIVE_URL
    if (s.BANNER_LIVE_TITLE && s.BANNER_LIVE_TITLE !== '-') bannerTitle.value = s.BANNER_LIVE_TITLE
    if (s.BANNER_LIVE_SUB && s.BANNER_LIVE_SUB !== '-') bannerSub.value = s.BANNER_LIVE_SUB
  } catch {
    /* 설정 조회 실패 시 기본값 유지 */
  }
  try {
    const st = await fetch('/api/live/status').then((r) => r.json())
    isLive.value = !!st.live
    liveTitle.value = st.liveTitle || ''
  } catch {
    isLive.value = false
  }
})

function liveUrl() {
  return channelId.value ? `https://chzzk.naver.com/live/${channelId.value}` : 'https://chzzk.naver.com'
}

// 크롭 스케일: 컨테이너 폭에 맞춰 치지직 페이지(1620 로드)의 영상 영역(x240,y60,1026 폭)만 보이게
const cropEl = ref<HTMLElement | null>(null)
const frameEl = ref<HTMLIFrameElement | null>(null)
function fitCrop() {
  if (!cropEl.value || !frameEl.value || !cropEl.value.clientWidth) return
  const s = cropEl.value.clientWidth / 1026
  frameEl.value.style.transform = `scale(${s}) translate(-240px, -60px)`
}
watch(loaded, async (v) => { if (v) { await nextTick(); fitCrop() } })
window.addEventListener('resize', fitCrop)
onBeforeUnmount(() => window.removeEventListener('resize', fitCrop))
</script>

<template>
  <section class="live-page">
    <div class="inner">
      <div class="section-header">
        <h2 class="section-title">생방송</h2>
      </div>

      <div class="goods-banner" style="border-radius: var(--radius2); overflow: hidden; margin-bottom: 28px;">
        <img v-if="!bannerBroken" :src="bannerUrl" alt="생방송 배너" @error="bannerBroken = true" />
        <div v-else style="position: absolute; inset: 0; background: #222226;"></div>
        <div class="goods-banner-overlay" style="padding-left: 40px;">
          <div class="goods-banner-text">
            <h2>{{ bannerTitle }}</h2>
            <p>{{ bannerSub }}</p>
          </div>
        </div>
      </div>

      <div class="live-bar" :class="{ on: isLive }">
        <div class="live-dot" :class="isLive ? 'on' : 'off'"></div>
        <div class="live-bar-text">
          <div class="live-bar-title">
            SNUK 공식 채널 — 치지직
            <span v-if="isLive" class="live-chip">LIVE</span>
          </div>
          <div class="live-bar-sub">{{ isLive ? (liveTitle || '지금 방송 중입니다!') : '현재 오프라인 — 방송이 시작되면 이곳에서 볼 수 있어요' }}</div>
        </div>
        <button v-if="channelId && !loaded" class="live-btn primary" :class="{ on: isLive }" @click="loaded = true">라이브 보기</button>
        <a :href="liveUrl()" target="_blank" rel="noopener"><button class="live-btn">치지직에서 보기 ↗</button></a>
      </div>

      <!-- 채널 미설정: 준비중 -->
      <div v-if="!channelId" class="live-empty">
        <div style="font-size: 40px; margin-bottom: 10px;">📺</div>
        공식 채널 라이브는 준비 중입니다.<br>
        채널이 연결되면 이곳에서 바로 시청할 수 있어요.
      </div>

      <!-- 임베드 -->
      <div v-else class="live-player-wrap">
        <div v-if="!loaded" class="live-placeholder" @click="loaded = true">
          <div style="font-size: 48px; margin-bottom: 10px;">▶</div>
          <div style="font-size: 14px; font-weight: 700;">라이브 보기 버튼을 눌러주세요</div>
        </div>
        <!-- 치지직 전체 페이지를 확대 로드 후 영상 영역만 크롭 (채팅 없이 영상만) -->
        <div v-else ref="cropEl" class="live-crop">
          <iframe ref="frameEl" :src="liveUrl()" class="live-video-frame" allowfullscreen
            allow="autoplay; fullscreen; encrypted-media; picture-in-picture"></iframe>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.live-bar {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  background: var(--bg2); border: 1px solid var(--border);
  border-radius: 14px; padding: 14px 18px; margin-bottom: 18px;
  transition: all .3s;
}
/* 온라인: 치지직 특유의 초록 톤 */
.live-bar.on { background: rgba(0, 230, 118, .05); border-color: rgba(0, 230, 118, .3); }
.live-dot { width: 10px; height: 10px; border-radius: 50%; flex: none; }
.live-dot.off { background: #55555e; }
.live-dot.on { background: #00e676; animation: liveGlow 1.4s ease-in-out infinite; }
@keyframes liveGlow {
  0%, 100% { box-shadow: 0 0 0 0 rgba(0, 230, 118, .55); opacity: 1; }
  50% { box-shadow: 0 0 0 7px rgba(0, 230, 118, 0); opacity: .75; }
}
.live-chip {
  display: inline-block; margin-left: 6px; padding: 1px 7px; border-radius: 5px;
  background: #00e676; color: #06130a; font-size: 10px; font-weight: 800; letter-spacing: 1px;
  animation: livePulse 1.4s ease-in-out infinite; vertical-align: 2px;
}
@keyframes livePulse { 0%, 100% { opacity: 1; } 50% { opacity: .55; } }
.live-bar-text { flex: 1; min-width: 180px; }
.live-bar-title { font-size: 14px; font-weight: 700; color: var(--text); }
.live-bar-sub { font-size: 12px; color: var(--text3); margin-top: 2px; }
.live-btn {
  padding: 8px 14px; font-size: 12px; font-weight: 700; border-radius: 8px; cursor: pointer;
  background: transparent; color: var(--text2); border: 1px solid var(--border);
}
.live-btn.primary { background: linear-gradient(135deg, #ff4040, #fc5c7d); color: #fff; border: none; }
.live-btn.primary.on { background: linear-gradient(135deg, #00c766, #00e676); color: #06130a; }

.live-empty {
  border: 1px dashed var(--border2); border-radius: 16px; padding: 70px 20px;
  text-align: center; color: var(--text3); font-size: 14px; line-height: 1.9;
}
.live-player-wrap { background: #000; border-radius: 16px; overflow: hidden; border: 1px solid var(--border); }
.live-placeholder {
  aspect-ratio: 16 / 9; display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; color: var(--text2); background: linear-gradient(135deg, #0d0010, #1a0030);
}
.live-crop { position: relative; aspect-ratio: 16 / 9; overflow: hidden; background: #000; }
.live-video-frame {
  position: absolute; left: 0; top: 0; width: 1620px; height: 900px;
  border: none; transform-origin: 0 0;
}
</style>
