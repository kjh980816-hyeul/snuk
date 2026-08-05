/// <reference types="vite/client" />

/** 빌드 시각 버전 (vite define) — public/ 셸 에셋 캐시버스팅용 */
declare const __SNUK_ASSET_V__: string

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default component
}
