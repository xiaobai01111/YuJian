import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { useUserStore } from '@/stores/user'
import type { LoginVO } from '@/api/auth'
import { readRefreshToken, writeRefreshToken } from '@/utils/refreshTokenStorage'

type HttpClient = Omit<AxiosInstance, 'get' | 'post' | 'put' | 'delete' | 'patch'> & {
    get<R = unknown, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    delete<R = unknown, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    post<R = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
    put<R = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
    patch<R = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
}

let handlingUnauthorized = false
let refreshPromise: Promise<LoginVO> | null = null
let refreshingToken: Promise<LoginVO> | null = null
const DEVICE_ID_STORAGE_KEY = 'campus:device-id'

const resolveDeviceId = (): string => {
    const fallback = () => {
        const random = Math.random().toString(36).slice(2, 10)
        return `web-${Date.now().toString(36)}-${random}`
    }

    if (typeof window === 'undefined') {
        return fallback()
    }

    try {
        const stored = window.localStorage.getItem(DEVICE_ID_STORAGE_KEY)
        if (stored) {
            return stored
        }
        const generated = (window.crypto && typeof window.crypto.randomUUID === 'function')
            ? `web-${window.crypto.randomUUID()}`
            : fallback()
        window.localStorage.setItem(DEVICE_ID_STORAGE_KEY, generated)
        return generated
    } catch {
        return fallback()
    }
}

const service: HttpClient = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '',
    timeout: 10000,
    withCredentials: true,
}) as HttpClient

const AUTH_LOGIN_URL = '/api/v1/auth/login'
const AUTH_REFRESH_URL = '/api/v1/auth/refresh'
const AUTH_CAPTCHA_URL = '/api/v1/auth/login-captcha'

type RetryRequestConfig = InternalAxiosRequestConfig & { _retry?: boolean }

const runRefreshRequest = async (): Promise<LoginVO> => {
    const payload: Record<string, string> = {}
    const storedToken = readRefreshToken()
    if (storedToken) {
        payload.refreshToken = storedToken
    }
    const response = await axios.post(
        `${import.meta.env.VITE_API_BASE_URL || ''}${AUTH_REFRESH_URL}`,
        payload,
        {
            timeout: 10000,
            withCredentials: true,
            headers: {
                'X-Device-Id': resolveDeviceId()
            }
        }
    )
    const res = response.data as {
        code?: number
        message?: string
        data?: LoginVO
    }
    if (res.code !== 200 || !res.data?.token) {
        throw new Error(res.message || '刷新登录态失败')
    }
    if (res.data.refreshToken) {
        writeRefreshToken(res.data.refreshToken)
    }
    return res.data
}

const requestAccessTokenRefresh = async (): Promise<LoginVO> => {
    if (refreshPromise) {
        return refreshPromise
    }
    refreshPromise = runRefreshRequest()
    try {
        return await refreshPromise
    } finally {
        refreshPromise = null
    }
}

const isAuthEndpoint = (url?: string): boolean => {
    if (!url) {
        return false
    }
    return url.includes(AUTH_LOGIN_URL) || url.includes(AUTH_REFRESH_URL) || url.includes(AUTH_CAPTCHA_URL)
}

service.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const userStore = useUserStore()
        config.headers['X-Device-Id'] = resolveDeviceId()
        if (userStore.token) {
            handlingUnauthorized = false
            config.headers['Authorization'] = `Bearer ${userStore.token}`
        }
        return config
    },
    (error: unknown) => {
        return Promise.reject(error)
    }
)

service.interceptors.response.use(
    (response: AxiosResponse) => {
        // Handle blob responses (for file downloads)
        if (response.config.responseType === 'blob') {
            return response.data
        }
        
        const res = response.data as { code?: number; message?: string; data?: unknown }
        // Handle backend R wrapper format
        if (res.code === 200) {
            return res.data
        } else {
            return Promise.reject(new Error(res.message || '请求失败'))
        }
    },
    async (error: unknown) => {
        // Handle auth errors (401)
        if (axios.isAxiosError(error) && error.response?.status === 401) {
            const userStore = useUserStore()
            const originalConfig = error.config as RetryRequestConfig | undefined
            const canRetry = !!originalConfig && !originalConfig._retry && !isAuthEndpoint(originalConfig.url)
            if (userStore.token && canRetry) {
                originalConfig._retry = true
                try {
                    if (!refreshingToken) {
                        refreshingToken = requestAccessTokenRefresh().finally(() => {
                            refreshingToken = null
                        })
                    }
                    const loginVO = await refreshingToken
                    if (loginVO?.token) {
                        userStore.token = loginVO.token
                        originalConfig.headers = originalConfig.headers ?? {}
                        originalConfig.headers['Authorization'] = `Bearer ${loginVO.token}`
                        return service.request(originalConfig)
                    }
                } catch {
                    // refresh 失败后走统一登出
                }
            }

            // 只处理一次，避免并发请求同时 401 导致循环跳转
            if (userStore.token && !handlingUnauthorized) {
                handlingUnauthorized = true
                userStore.forceLogout()
                window.location.replace('/')
            }
        }
        if (axios.isAxiosError(error) && error.response?.data) {
            const data = error.response.data as { message?: string; msg?: string }
            error.message = data.message || data.msg || error.message
        }
        return Promise.reject(error)
    }
)

export default service
