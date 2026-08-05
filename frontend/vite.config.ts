import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  // public/ 셸 에셋(home-snuk.css 등)은 해시가 없어 모바일 브라우저가 옛 캐시를 재사용함
  // → 빌드 시각 버전을 쿼리로 붙여 배포 즉시 반영 (SnukShell/SnukSections 에서 사용)
  define: {
    __SNUK_ASSET_V__: JSON.stringify(Date.now().toString(36)),
  },
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 개발 중 API/OAuth 는 백엔드(8080)로 프록시
      '/api': 'http://localhost:8080',
      '/oauth2': 'http://localhost:8080',
      '/login': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
    },
  },
})
