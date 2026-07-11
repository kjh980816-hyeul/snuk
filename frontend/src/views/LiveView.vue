<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { OFFICIAL_CHZZK_CHANNEL_ID } from '@/config'
import { siteSettingsApi } from '@/api'

// 생방송: SNUK 공식 치지직 채널 라이브 임베드 (chzzk.naver.com/live/{채널ID})
// 채널 ID 는 어드민 "설정" 탭(LIVE_CHANNEL_ID)에서 관리 — 미설정('-')이면 config 기본값
const channelId = ref(OFFICIAL_CHZZK_CHANNEL_ID)
const loaded = ref(false)

onMounted(async () => {
  try {
    const s = await siteSettingsApi.get()
    if (s.LIVE_CHANNEL_ID && s.LIVE_CHANNEL_ID !== '-') channelId.value = s.LIVE_CHANNEL_ID
  } catch {
    /* 설정 조회 실패 시 config 기본값 유지 */
  }
})

function liveUrl() {
  return channelId.value ? `https://chzzk.naver.com/live/${channelId.value}` : 'https://chzzk.naver.com'
}
</script>

<template>
  <section class="live-page">
    <div class="inner">
      <div class="section-header">
        <h2 class="section-title">생방송</h2>
      </div>

      <div class="live-bar">
        <div class="live-dot-red"></div>
        <div class="live-bar-text">
          <div class="live-bar-title">SNUK 공식 채널 — 치지직 LIVE</div>
          <div class="live-bar-sub">SNUK 공식 치지직 채널 실시간 방송</div>
        </div>
        <button v-if="channelId && !loaded" class="live-btn primary" @click="loaded = true">라이브 보기</button>
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
        <div v-else class="live-frames">
          <iframe :src="liveUrl()" class="live-video" allowfullscreen allow="autoplay;fullscreen"></iframe>
          <iframe :src="`https://chzzk.naver.com/chat/${channelId}`" class="live-chat"></iframe>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.live-bar {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  background: rgba(255, 64, 64, .06); border: 1px solid rgba(255, 64, 64, .25);
  border-radius: 14px; padding: 14px 18px; margin-bottom: 18px;
}
.live-dot-red { width: 10px; height: 10px; border-radius: 50%; background: #ff4444; animation: livePulse 1.2s infinite; flex: none; }
@keyframes livePulse { 0%, 100% { opacity: 1; } 50% { opacity: .35; } }
.live-bar-text { flex: 1; min-width: 180px; }
.live-bar-title { font-size: 14px; font-weight: 700; color: var(--text); }
.live-bar-sub { font-size: 12px; color: var(--text3); margin-top: 2px; }
.live-btn {
  padding: 8px 14px; font-size: 12px; font-weight: 700; border-radius: 8px; cursor: pointer;
  background: transparent; color: var(--text2); border: 1px solid var(--border);
}
.live-btn.primary { background: linear-gradient(135deg, #ff4040, #fc5c7d); color: #fff; border: none; }

.live-empty {
  border: 1px dashed var(--border2); border-radius: 16px; padding: 70px 20px;
  text-align: center; color: var(--text3); font-size: 14px; line-height: 1.9;
}
.live-player-wrap { background: #000; border-radius: 16px; overflow: hidden; border: 1px solid var(--border); }
.live-placeholder {
  aspect-ratio: 16 / 9; display: flex; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; color: var(--text2); background: linear-gradient(135deg, #0d0010, #1a0030);
}
.live-frames { display: flex; gap: 0; }
.live-video { flex: 1; aspect-ratio: 16 / 9; border: none; display: block; min-width: 0; }
.live-chat { width: 320px; border: none; border-left: 1px solid var(--border); display: block; }
@media (max-width: 900px) {
  .live-frames { flex-direction: column; }
  .live-chat { width: 100%; height: 360px; border-left: none; border-top: 1px solid var(--border); }
}
</style>
