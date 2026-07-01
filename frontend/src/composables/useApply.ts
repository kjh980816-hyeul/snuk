import { ref } from 'vue'
import { campaignApi } from '@/api'
import type { Campaign } from '@/api/types'
import { useAuthStore } from '@/stores/auth'

/** 캠페인 신청 플로우 — 비로그인→로그인, VIEWER→안내, STREAMER→신청. */
export function useApply(onChanged?: () => void) {
  const auth = useAuthStore()
  const applyMsg = ref<string | null>(null)

  async function apply(c: Campaign) {
    applyMsg.value = null
    if (!auth.isLoggedIn) {
      auth.login()
      return
    }
    if (!auth.isStreamer) {
      applyMsg.value = '스트리머 등급(팔로워 임계값 이상)부터 신청할 수 있어요.'
      return
    }
    try {
      const res = await campaignApi.apply(c.id)
      applyMsg.value = res.status === 'APPROVED' ? '신청이 완료되어 배정되었습니다!' : '신청이 접수되었습니다(승인 대기).'
      onChanged?.()
    } catch (e: unknown) {
      const msg = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
      applyMsg.value = msg ?? '신청에 실패했습니다.'
    }
  }

  return { applyMsg, apply }
}

/** 유튜브 watch/youtu.be URL → embed URL */
export function toEmbed(url: string): string {
  const yt = url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([\w-]+)/)
  return yt ? `https://www.youtube.com/embed/${yt[1]}` : url
}
