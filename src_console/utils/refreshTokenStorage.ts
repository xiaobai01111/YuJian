const REFRESH_TOKEN_STORAGE_KEY = 'campus:refresh-token'

const safeWindow = typeof window !== 'undefined'

export const readRefreshToken = (): string | null => {
  if (!safeWindow) {
    return null
  }
  return window.sessionStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)
}

export const writeRefreshToken = (token?: string | null) => {
  if (!safeWindow) {
    return
  }
  if (token && token.trim()) {
    window.sessionStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, token.trim())
  } else {
    window.sessionStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY)
  }
}

export const clearRefreshToken = () => {
  if (!safeWindow) {
    return
  }
  window.sessionStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY)
}
