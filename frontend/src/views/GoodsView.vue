<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import SnukSections from '@/components/SnukSections.vue'
import { goodsApi } from '@/api'
import type { Goods } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useCheckout } from '@/composables/useCheckout'
import { GOODS_READY } from '@/config'

// 굿즈샵: 시안 goods 섹션(카드/슬라이더는 시안 렌더러) + 실 결제 패널(PortOne)
// 카드의 "구매하기" → __snukActions.buyGoods → /goods?buy={id} → 이 패널 오픈
const show = ['goods']

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const selected = ref<Goods | null>(null)

const form = reactive({
  quantity: 1,
  receiverName: '',
  receiverPhone: '',
  zipcode: '',
  address: '',
  addressDetail: '',
  memo: '',
})

const { processing, message, doneOrder, checkout } = useCheckout()

function won(v: number) {
  return v.toLocaleString('ko-KR') + '원'
}

async function openByQuery() {
  const buyId = Number(route.query.buy)
  if (!GOODS_READY || !buyId) return
  try {
    const g = await goodsApi.detail(buyId)
    if (g.purchasable) {
      selected.value = g
      form.quantity = 1
      message.value = ''
      doneOrder.value = null
    }
  } catch {
    /* 없는 상품 — 무시 */
  }
}

function closePanel() {
  selected.value = null
  if (route.query.buy) router.replace({ path: '/goods' })
}

async function buy() {
  if (!selected.value) return
  await checkout(selected.value, {
    quantity: form.quantity,
    receiverName: form.receiverName,
    receiverPhone: form.receiverPhone,
    zipcode: form.zipcode,
    address: form.address,
    addressDetail: form.addressDetail,
    memo: form.memo,
  })
  if (doneOrder.value?.status === 'PAID') closePanel()
}

onMounted(openByQuery)
watch(() => route.query.buy, openByQuery)
</script>

<template>
  <SnukSections :show="show" />

  <!-- 구매 패널 (다크 테마) -->
  <div v-if="selected" class="buy-overlay" @click.self="closePanel">
    <div class="buy-panel">
      <button class="buy-close" @click="closePanel">✕</button>
      <h3>{{ selected.name }} 주문</h3>
      <p class="buy-price">{{ won(selected.price) }} · 재고 {{ selected.stock }}개</p>

      <template v-if="!auth.isLoggedIn">
        <p class="buy-notice">구매하려면 로그인이 필요합니다.</p>
        <button class="buy-btn" @click="auth.login()">로그인</button>
      </template>

      <template v-else>
        <label class="buy-field">
          수량
          <input type="number" v-model.number="form.quantity" min="1" :max="selected.stock" />
        </label>
        <label class="buy-field">받는 분<input v-model="form.receiverName" placeholder="이름" /></label>
        <label class="buy-field">연락처<input v-model="form.receiverPhone" placeholder="010-0000-0000" /></label>
        <label class="buy-field">우편번호<input v-model="form.zipcode" placeholder="선택" /></label>
        <label class="buy-field">주소<input v-model="form.address" placeholder="배송지 주소" /></label>
        <label class="buy-field">상세주소<input v-model="form.addressDetail" placeholder="동/호수 등" /></label>
        <label class="buy-field">메모<input v-model="form.memo" placeholder="배송 요청사항(선택)" /></label>

        <p class="buy-total">결제금액 <b>{{ won(selected.price * (form.quantity || 0)) }}</b></p>
        <button
          class="buy-btn"
          :disabled="processing || !form.receiverName || !form.receiverPhone || !form.address || form.quantity < 1"
          @click="buy"
        >
          {{ processing ? '처리 중…' : '결제하기' }}
        </button>
      </template>

      <p v-if="message" class="buy-msg">{{ message }}</p>
    </div>
  </div>
</template>

<style scoped>
.buy-overlay { position: fixed; inset: 0; background: rgba(0,0,0,.6); display: flex; align-items: center; justify-content: center; z-index: 1000; padding: 20px; }
.buy-panel { position: relative; background: #1c1c1f; color: #e8e8e8; border: 1px solid #2e2e33; border-radius: 14px; padding: 26px; width: 100%; max-width: 420px; max-height: 90vh; overflow-y: auto; }
.buy-panel h3 { margin: 0 0 6px; font-size: 17px; }
.buy-close { position: absolute; top: 14px; right: 16px; background: none; border: none; color: #aaa; font-size: 16px; cursor: pointer; }
.buy-price { color: #ffb300; font-weight: 700; margin: 0 0 10px; }
.buy-field { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: #b8b8c0; margin-top: 12px; }
.buy-field input { padding: 9px 11px; border: 1px solid #2e2e33; background: #131316; color: #e8e8e8; border-radius: 8px; font-size: 14px; outline: none; }
.buy-total { margin: 18px 0 12px; font-size: 15px; }
.buy-total b { color: #ffb300; font-size: 18px; }
.buy-btn { width: 100%; padding: 12px; border: none; border-radius: 10px; font-weight: 700; font-size: 14px; color: #fff; background: linear-gradient(135deg, #6a5cff, #8c5cff); cursor: pointer; }
.buy-btn:disabled { opacity: .5; cursor: not-allowed; }
.buy-notice { margin: 14px 0; color: #b8b8c0; }
.buy-msg { margin-top: 14px; color: #ffb300; font-weight: 700; }
</style>
