import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { useUserStore } from '@/stores/user'

type HttpClient = Omit<AxiosInstance, 'get' | 'post' | 'put' | 'delete' | 'patch'> & {
    get<R = unknown, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    delete<R = unknown, D = unknown>(url: string, config?: AxiosRequestConfig<D>): Promise<R>
    post<R = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
    put<R = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
    patch<R = unknown, D = unknown>(url: string, data?: D, config?: AxiosRequestConfig<D>): Promise<R>
}

let handlingUnauthorized = false
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
}) as HttpClient

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
    (error: unknown) => {
        // Handle auth errors (401)
        if (axios.isAxiosError(error) && error.response?.status === 401) {
            const userStore = useUserStore()
            // 只处理一次，避免并发请求同时 401 导致循环跳转/刷新
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
