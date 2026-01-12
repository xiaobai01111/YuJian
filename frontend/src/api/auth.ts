import axios from 'axios'

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
    timeout: 5000
})

// Request interceptor to add token
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// Response interceptor
api.interceptors.response.use(
    (response) => {
        const res = response.data
        if (res.code === 200) {
            return res.data
        } else {
            // Handle business errors
            return Promise.reject(new Error(res.message || 'Error'))
        }
    },
    (error) => {
        return Promise.reject(error)
    }
)

export interface LoginDTO {
    username: string
    password: string
}

export interface LoginVO {
    token: string
    userInfo: UserInfoVO
}

export interface UserInfoVO {
    id: number
    username: string
    nickname: string
    avatar?: string
    role?: string
}

export interface RegisterDTO {
    username: string
    password: string
    confirmPassword: string
    nickname?: string
    email?: string
}

export const register = (data: RegisterDTO) => {
    return api.post<any, number>('/api/v1/auth/register', data)
}

export const login = (data: LoginDTO) => {
    return api.post<any, LoginVO>('/api/v1/auth/login', data)
}

export const logout = () => {
    return api.post('/api/v1/auth/logout')
}

export const getUserInfo = () => {
    return api.get<any, UserInfoVO>('/api/v1/auth/info')
}
