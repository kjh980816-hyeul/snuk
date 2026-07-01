import { ref } from 'vue'
import { orderApi } from '@/api'
import type { Goods, OrderCreateRequest, OrderView } from '@/api/types'

/**
 * 굿즈 결제 흐름:
 *  1) 서버에 주문 생성(재고 선점) → paymentId·금액·storeId·channelKey 수신
 *  2) PortOne 결제창(requestPayment) 호출 — 카드정보는 PG가 처리(우리 서버 미저장)
 *  3) 완료 후 서버 confirm → 서버가 PortOne 재조회로 금액 검증 후 PAID 확정
 * 결제금액은 서버가 확정한 값(order.amount)을 그대로 전달 = 위변조 불가.
 */

const SDK_URL = 'https://cdn.portone.io/v2/browser-sdk.js'
let sdkLoading: Promise<void> | null = null

function loadSdk(): Promise<void> {
  if ((window as any).PortOne) return Promise.resolve()
  if (sdkLoading) return sdkLoading
  sdkLoading = new Promise<void>((resolve, reject) => {
    const s = document.createElement('script')
    s.src = SDK_URL
    s.onload = () => resolve()
    s.onerror = () => reject(new Error('PortOne SDK 로드 실패'))
    document.head.appendChild(s)
  })
  return sdkLoading
}

export function useCheckout(reload?: () => Promise<void> | void) {
  const processing = ref(false)
  const message = ref('')
  const doneOrder = ref<OrderView | null>(null)

  async function checkout(goods: Goods, form: Omit<OrderCreateRequest, 'goodsId'>) {
    message.value = ''
    doneOrder.value = null
    processing.value = true
    try {
      // 1) 주문 생성(서버가 금액 확정 + 재고 선점)
      const order = await orderApi.create({ goodsId: goods.id, ...form })

      // 2) 결제창 호출
      await loadSdk()
      const PortOne = (window as any).PortOne
      const res = await PortOne.requestPayment({
        storeId: order.storeId,
        channelKey: order.channelKey,
        paymentId: order.paymentId,
        orderName: order.orderName,
        totalAmount: order.amount,
        currency: 'CURRENCY_KRW',
        payMethod: 'CARD',
        customer: {
          fullName: form.receiverName,
          phoneNumber: form.receiverPhone,
        },
      })
      if (res?.code) {
        // 사용자가 창을 닫거나 결제 실패 (재고 선점분은 서버 정리 대상)
        message.value = res.message || '결제가 취소되었습니다.'
        return
      }

      // 3) 서버 검증(금액 대조)
      const confirmed = await orderApi.confirm(order.orderId)
      doneOrder.value = confirmed
      if (confirmed.status === 'PAID') {
        message.value = '결제가 완료되었습니다.'
      } else if (confirmed.status === 'FAILED') {
        message.value = '결제 검증에 실패했습니다. 결제가 취소되었습니다.'
      } else {
        message.value = '결제 확인 중입니다. 잠시 후 주문내역에서 확인해 주세요.'
      }
      if (reload) await reload()
    } catch (e: any) {
      message.value = e?.response?.data?.message ?? e?.message ?? '결제 처리 중 오류가 발생했습니다.'
    } finally {
      processing.value = false
    }
  }

  return { processing, message, doneOrder, checkout }
}
