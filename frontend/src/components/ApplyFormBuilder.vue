<script setup lang="ts">
// 신청 질문 빌더(네이버 폼 스타일) — 캠페인/대회 공용.
// 질문 카드: 질문 + 유형(단답/장문/객관식/체크박스) + 선택지 + 필수 토글 + 순서/삭제.
// modelValue 는 항상 새 배열로 emit (부모 editing 객체에 그대로 대입 가능).
import type { ApplyQuestion, ApplyQuestionType } from '@/api/types'

const props = defineProps<{ modelValue?: ApplyQuestion[] | null }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: ApplyQuestion[]): void }>()

const TYPE_LABELS: Array<{ v: ApplyQuestionType; label: string }> = [
  { v: 'SHORT', label: '단답형' },
  { v: 'LONG', label: '장문형' },
  { v: 'SELECT', label: '객관식 (하나 선택)' },
  { v: 'MULTI', label: '체크박스 (복수 선택)' },
]

function qs(): ApplyQuestion[] {
  return (props.modelValue ?? []).map((q) => ({
    q: q.q, required: q.required, type: q.type ?? 'SHORT', options: q.options ? [...q.options] : [],
  }))
}
function commit(next: ApplyQuestion[]) {
  emit('update:modelValue', next)
}
function patch(i: number, part: Partial<ApplyQuestion>) {
  const next = qs()
  next[i] = { ...next[i], ...part }
  commit(next)
}
function add() {
  commit([...qs(), { q: '', required: true, type: 'SHORT', options: [] }])
}
function remove(i: number) {
  commit(qs().filter((_, j) => j !== i))
}
function move(i: number, dir: -1 | 1) {
  const next = qs()
  const j = i + dir
  if (j < 0 || j >= next.length) return
  ;[next[i], next[j]] = [next[j], next[i]]
  commit(next)
}
function setType(i: number, t: ApplyQuestionType) {
  const cur = qs()[i]
  const opts = (t === 'SELECT' || t === 'MULTI')
    ? (cur.options?.length ? cur.options : ['옵션 1'])
    : []
  patch(i, { type: t, options: opts })
}
function setOption(i: number, oi: number, v: string) {
  const next = qs()
  next[i].options![oi] = v
  commit(next)
}
function addOption(i: number) {
  const next = qs()
  next[i].options = [...(next[i].options ?? []), `옵션 ${(next[i].options?.length ?? 0) + 1}`]
  commit(next)
}
function removeOption(i: number, oi: number) {
  const next = qs()
  next[i].options = next[i].options!.filter((_, j) => j !== oi)
  commit(next)
}
</script>

<template>
  <div class="afb">
    <div v-for="(q, i) in (modelValue ?? [])" :key="i" class="afb-card">
      <div class="afb-head">
        <span class="afb-no">Q{{ i + 1 }}</span>
        <input class="afb-q" :value="q.q" placeholder="질문을 입력하세요"
          @input="patch(i, { q: ($event.target as HTMLInputElement).value })" />
        <select class="afb-type" :value="q.type ?? 'SHORT'"
          @change="setType(i, ($event.target as HTMLSelectElement).value as ApplyQuestionType)">
          <option v-for="t in TYPE_LABELS" :key="t.v" :value="t.v">{{ t.label }}</option>
        </select>
      </div>

      <div v-if="q.type === 'SELECT' || q.type === 'MULTI'" class="afb-opts">
        <div v-for="(o, oi) in q.options ?? []" :key="oi" class="afb-opt">
          <span class="afb-opt-mark">{{ q.type === 'SELECT' ? '◯' : '☐' }}</span>
          <input :value="o" placeholder="선택지"
            @input="setOption(i, oi, ($event.target as HTMLInputElement).value)" />
          <button type="button" class="afb-x" title="선택지 삭제"
            :disabled="(q.options?.length ?? 0) <= 1" @click="removeOption(i, oi)">✕</button>
        </div>
        <button type="button" class="afb-add-opt" @click="addOption(i)">＋ 선택지 추가</button>
      </div>
      <div v-else class="afb-preview">
        {{ q.type === 'LONG' ? '신청자가 여러 줄로 답변을 입력합니다' : '신청자가 한 줄로 답변을 입력합니다' }}
      </div>

      <div class="afb-foot">
        <label class="afb-req">
          <input type="checkbox" :checked="q.required"
            @change="patch(i, { required: ($event.target as HTMLInputElement).checked })" />
          필수
        </label>
        <span class="afb-spacer"></span>
        <button type="button" class="afb-mv" title="위로" :disabled="i === 0" @click="move(i, -1)">↑</button>
        <button type="button" class="afb-mv" title="아래로" :disabled="i === (modelValue?.length ?? 0) - 1" @click="move(i, 1)">↓</button>
        <button type="button" class="afb-del" @click="remove(i)">삭제</button>
      </div>
    </div>

    <button type="button" class="afb-add" @click="add">＋ 질문 추가</button>
    <p class="afb-hint">신청자가 신청할 때 위 순서대로 답변합니다. 객관식·체크박스는 선택지 중에서 고르게 돼요.</p>
  </div>
</template>

<style scoped>
.afb { display: flex; flex-direction: column; gap: 10px; margin-top: 6px; }
.afb-card {
  border: 1px solid var(--a-border, #e2e2e8); border-radius: 12px; padding: 12px;
  background: var(--a-bg3, #fafafc); display: flex; flex-direction: column; gap: 9px;
}
.afb-head { display: flex; gap: 8px; align-items: center; }
.afb-no { font-size: 12px; font-weight: 800; color: var(--a-accent, #7c5cff); flex-shrink: 0; }
.afb-q {
  flex: 1; min-width: 0; padding: 8px 10px; border: 1px solid var(--a-border2, #d8d8e0);
  border-radius: 8px; font-size: 13px; background: var(--a-bg4, #fff); color: var(--a-text, #222);
}
.afb-type {
  flex-shrink: 0; padding: 8px 6px; border: 1px solid var(--a-border2, #d8d8e0);
  border-radius: 8px; font-size: 12px; background: var(--a-bg4, #fff); color: var(--a-text2, #555);
}
.afb-opts { display: flex; flex-direction: column; gap: 6px; padding-left: 4px; }
.afb-opt { display: flex; gap: 7px; align-items: center; }
.afb-opt-mark { color: var(--a-text3, #999); font-size: 13px; flex-shrink: 0; }
.afb-opt input {
  flex: 1; min-width: 0; padding: 6px 9px; border: 1px solid var(--a-border2, #d8d8e0);
  border-radius: 7px; font-size: 12.5px; background: var(--a-bg4, #fff); color: var(--a-text, #222);
}
.afb-x {
  border: none; background: none; color: var(--a-text3, #999); cursor: pointer;
  font-size: 12px; padding: 4px 6px; border-radius: 6px;
}
.afb-x:hover:not(:disabled) { color: var(--a-red, #e5484d); background: rgba(229, 72, 77, .08); }
.afb-x:disabled { opacity: .3; cursor: default; }
.afb-add-opt {
  align-self: flex-start; border: none; background: none; cursor: pointer; padding: 4px 6px;
  font-size: 12px; font-weight: 700; color: var(--a-accent, #7c5cff); border-radius: 6px;
}
.afb-add-opt:hover { background: var(--a-tint, rgba(124, 92, 255, .08)); }
.afb-preview {
  font-size: 12px; color: var(--a-text3, #999); border-bottom: 1px dashed var(--a-border2, #d8d8e0);
  padding: 2px 4px 7px;
}
.afb-foot { display: flex; align-items: center; gap: 6px; border-top: 1px solid var(--a-border, #ececf2); padding-top: 8px; }
.afb-req { display: flex; align-items: center; gap: 5px; font-size: 12.5px; font-weight: 700; color: var(--a-text2, #555); cursor: pointer; }
.afb-spacer { flex: 1; }
.afb-mv, .afb-del {
  border: 1px solid var(--a-border2, #d8d8e0); background: var(--a-bg4, #fff); cursor: pointer;
  font-size: 12px; padding: 4px 9px; border-radius: 7px; color: var(--a-text2, #555);
}
.afb-mv:disabled { opacity: .3; cursor: default; }
.afb-del { color: var(--a-red, #e5484d); }
.afb-del:hover { background: rgba(229, 72, 77, .08); }
.afb-add {
  border: 1.5px dashed var(--a-border2, #d0d0da); background: none; cursor: pointer;
  padding: 10px; border-radius: 10px; font-size: 13px; font-weight: 700; color: var(--a-accent, #7c5cff);
}
.afb-add:hover { background: var(--a-tint, rgba(124, 92, 255, .06)); }
.afb-hint { font-size: 11.5px; color: var(--a-text3, #999); margin: 0; }
</style>
