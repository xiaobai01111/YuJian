import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { useUserStore } from '@/stores/user'

const service: AxiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '/api/v1',
    timeout: 10000,
})

service.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
        const userStore = useUserStore()
        if (userStore.token) {
            // Sa-Token expects token directly without Bearer prefix
            config.headers['Authorization'] = userStore.token
        }
        return config
    },
    (error: any) => {
        return Promise.reject(error)
    }
)

service.interceptors.response.use(
    (response: AxiosResponse) => {
        const res = response.data
        // Handle backend R wrapper format
        if (res.code === 200) {
            return res.data
        } else {
            return Promise.reject(new Error(res.message || '请求失败'))
        }
    },
    (error: any) => {
        // Handle auth errors (401)
        if (error.response && error.response.status === 401) {
            const userStore = useUserStore()
            userStore.logout()
            location.reload()
        }
        return Promise.reject(error)
    }
)

export default service
