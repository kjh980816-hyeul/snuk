import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'
import './styles/tokens.css'
import './snuk/chzzkPlayer' // window.__snukPlayer — 홈 무대(home-snuk-init.js)·/live 공용 치지직 HLS 플레이어

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
