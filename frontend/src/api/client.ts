import axios, { type AxiosInstance } from 'axios'

/**
 * Axios 인스턴스. access 토큰 자동 첨부 + 401 시 refresh 1회 재시도(무한루프 제외).
 * 토큰은 localStorage 보관(HttpOnly 쿠키 미사용 — 추후 강화 가능).
 */
const ACCESS_KEY = 'snuk_access'
const REFRESH_KEY = 'snuk_refresh'

export function getAccessToken(): string | null {
  return localStorage.getItem(ACCESS_KEY)
}
export function getRefreshToken(): string | null {
  return localStorage.getItem(REFRESH_KEY)
}
export function setTokens(access: string, refresh: string): void {
  localStorage.setItem(ACCESS_KEY, access)
  localStorage.setItem(REFRESH_KEY, refresh)
}
export function clearTokens(): void {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

const api: AxiosInstance = axios.create({
  baseURL: '/',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

let refreshing: Promise<string> | null = null

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    const isRefreshCall = original?.url?.includes('/api/auth/refresh')
    // 403: 가입 후 관리자/스트리머로 승격돼도 기존 access 토큰에 옛 role 이 박혀 거부됨
    // (2026-07-28 뮤마랭 어드민 저장/업로드 전부 403 사건). refresh 는 DB 최신 role 로
    // 재발급하므로 1회 갱신 후 재시도하면 재로그인 없이 즉시 권한이 반영된다.
    if (error.response?.status === 403 && !original?._retry403 && !isRefreshCall && getRefreshToken()) {
      original._retry403 = true
      try {
        const r = await axios.post('/api/auth/refresh', { refreshToken: getRefreshToken() })
        setTokens(r.data.accessToken, r.data.refreshToken)
        original.headers.Authorization = `Bearer ${r.data.accessToken}`
        return api(original)
      } catch {
        return Promise.reject(error)
      }
    }
    if (error.response?.status === 401 && !original?._retry && !isRefreshCall && getRefreshToken()) {
      original._retry = true
      try {
        if (!refreshing) {
          refreshing = axios
            .post('/api/auth/refresh', { refreshToken: getRefreshToken() })
            .then((r) => {
              setTokens(r.data.accessToken, r.data.refreshToken)
              return r.data.accessToken as string
            })
            .finally(() => {
              refreshing = null
            })
        }
        const newToken = await refreshing
        original.headers.Authorization = `Bearer ${newToken}`
        return api(original)
      } catch (e) {
        clearTokens()
        return Promise.reject(e)
      }
    }
    return Promise.reject(error)
  },
)

export default api
