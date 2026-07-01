<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { goodsApi } from '@/api'
import type { Goods } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { useCheckout } from '@/composables/useCheckout'

const auth = useAuthStore()
const goods = ref<Goods[]>([])
const loading = ref(true)
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

async function reload() {
  goods.value = await goodsApi.list()
}
const { processing, message, doneOrder, checkout } = useCheckout(reload)

function won(v: number) {
  return v.toLocaleString('ko-KR') + '원'
}

function open(g: Goods) {
  selected.value = g
  form.quantity = 1
  message.value = ''
  doneOrder.value = null
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
  if (doneOrder.value?.status === 'PAID') selected.value = null
}

onMounted(async () => {
  try {
    await reload()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="wrap section">
    <h2 class="section-label">굿즈</h2>

    <div v-if="loading" class="empty-state">불러오는 중…</div>
    <div v-else-if="!goods.length" class="empty-state">아직 등록된 굿즈가 없습니다.</div>
    <div v-else class="card-grid">
      <div v-for="g in goods" :key="g.id" class="goods-card">
        <div class="thumb">
          <img v-if="g.imageUrl" :src="g.imageUrl" :alt="g.name" />
          <div v-else class="placeholder">굿즈 사진</div>
          <span v-if="!g.purchasable" class="soldout">품절</span>
        </div>
        <div class="body">
          <h3>{{ g.name }}</h3>
          <p class="price">{{ won(g.price) }}</p>
          <p class="stock">재고 {{ g.stock }}개</p>
          <button class="btn" :disabled="!g.purchasable" @click="open(g)">
            {{ g.purchasable ? '구매하기' : '품절' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 구매 패널 -->
    <div v-if="selected" class="overlay" @click.self="selected = null">
      <div class="panel">
        <button class="close" @click="selected = null">✕</button>
        <h3>{{ selected.name }} 주문</h3>
        <p class="price">{{ won(selected.price) }} · 재고 {{ selected.stock }}개</p>

        <template v-if="!auth.isLoggedIn">
          <p class="notice">구매하려면 로그인이 필요합니다.</p>
          <button class="btn orange" @click="auth.login()">치지직 로그인</button>
        </template>

        <template v-else>
          <label class="field">
            수량
            <input type="number" v-model.number="form.quantity" min="1" :max="selected.stock" />
          </label>
          <label class="field">받는 분<input v-model="form.receiverName" placeholder="이름" /></label>
          <label class="field">연락처<input v-model="form.receiverPhone" placeholder="010-0000-0000" /></label>
          <label class="field">우편번호<input v-model="form.zipcode" placeholder="선택" /></label>
          <label class="field">주소<input v-model="form.address" placeholder="배송지 주소" /></label>
          <label class="field">상세주소<input v-model="form.addressDetail" placeholder="동/호수 등" /></label>
          <label class="field">메모<input v-model="form.memo" placeholder="배송 요청사항(선택)" /></label>

          <p class="total">결제금액 <b>{{ won(selected.price * (form.quantity || 0)) }}</b></p>
          <button
            class="btn orange"
            :disabled="processing || !form.receiverName || !form.receiverPhone || !form.address || form.quantity < 1"
            @click="buy"
          >
            {{ processing ? '처리 중…' : '결제하기' }}
          </button>
        </template>

        <p v-if="message" class="msg">{{ message }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.card-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 20px; }
.goods-card { border: 1px solid #eee; border-radius: var(--radius); overflow: hidden; background: #fff; }
.thumb { position: relative; aspect-ratio: 1 / 1; background: #f6f6f6; }
.thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.placeholder { display: flex; align-items: center; justify-content: center; height: 100%; color: var(--text-muted); }
.soldout { position: absolute; top: 10px; left: 10px; background: #1a1a1a; color: #fff; padding: 3px 10px; border-radius: 4px; font-size: 12px; }
.body { padding: 14px; }
.body h3 { font-size: 16px; color: var(--text-strong); margin: 0 0 6px; }
.price { font-weight: 800; color: var(--text-strong); }
.stock { font-size: 13px; color: var(--text-muted); margin: 4px 0 12px; }

.overlay { position: fixed; inset: 0; background: rgba(0,0,0,.5); display: flex; align-items: center; justify-content: center; z-index: 100; padding: 20px; }
.panel { position: relative; background: #fff; border-radius: var(--radius); padding: 26px; width: 100%; max-width: 420px; max-height: 90vh; overflow-y: auto; }
.close { position: absolute; top: 14px; right: 16px; background: none; font-size: 18px; cursor: pointer; }
.field { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: var(--text-body); margin-top: 12px; }
.field input { padding: 9px 11px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; }
.total { margin: 18px 0 12px; font-size: 15px; }
.total b { color: var(--accent-orange); font-size: 18px; }
.notice { margin: 14px 0; color: var(--text-body); }
.msg { margin-top: 14px; color: var(--accent-orange); font-weight: 700; }
</style>
