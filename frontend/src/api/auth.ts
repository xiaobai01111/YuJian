import axios from 'axios'

const api = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || '',
    timeout: 5000
})

// Request interceptor to add token
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('token')
        if (token) {
            // Sa-Token expects Bearer prefix
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
    email?: string
    phone?: string
    sex?: number
    verifyStatus?: number
    verifyMethod?: string
    creditScore?: number
    roles?: string[]
    permissions?: string[]
    createdAt?: string
}

export interface RegisterDTO {
    username: string
    password: string
    confirmPassword: string
    nickname?: string
    email: string
    emailCode?: string
}

export const register = (data: RegisterDTO) => {
    return api.post<any, number>('/api/v1/auth/register', data)
}

export const sendRegisterEmailCode = (email: string) => {
    return api.post('/api/v1/auth/register-email-code', { email })
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

export interface UpdatePasswordDTO {
    oldPassword: string
    newPassword: string
    confirmPassword: string
}

export const updatePassword = (data: UpdatePasswordDTO) => {
    return api.put('/api/v1/auth/password', data)
}

export interface UserProfileUpdateDTO {
    nickname: string
    email?: string
    phone?: string
    sex?: number
}

export interface UserProfileVO {
    id: number
    username: string
    nickname: string
    avatar?: string
    email?: string
    phone?: string
    eduEmail?: string
    sex?: number
    verifyStatus?: number
    verifyMethod?: string
    verifyRejectReason?: string
    creditScore?: number
    status?: number
    roleIds?: number[]
    remark?: string
    createdAt?: string
    updatedAt?: string
}

export const getMyProfile = () => {
    return api.get<any, UserProfileVO>('/api/v1/users/me')
}

export const updateProfile = (data: UserProfileUpdateDTO) => {
    return api.post('/api/v1/users/me', data)
}

export const getMyPosts = (page = 1, size = 10) => {
    return api.get<any, any>('/api/v1/users/me/posts', { params: { page, size } })
}

export const getMyBookmarks = (page = 1, size = 10) => {
    return api.get<any, any>('/api/v1/users/bookmarks', { params: { page, size } })
}

export const getMyCreditScore = () => {
    return api.get<any, number>('/api/v1/users/credit')
}

export const verifyEmail = (eduEmail: string) => {
    return api.post('/api/v1/auth/verify-email', { eduEmail })
}

export const confirmEmail = (code: string) => {
    return api.post('/api/v1/auth/confirm-email', { code })
}

export interface SubmitIdCardDTO {
    imageUrl: string
    studentId?: string
}

export const submitIdCard = (data: SubmitIdCardDTO) => {
    return api.post('/api/v1/auth/submit-id-card', data)
}

export interface SubmitStudentIdDTO {
    studentId: string
}

export const submitStudentId = (data: SubmitStudentIdDTO) => {
    return api.post('/api/v1/auth/submit-student-id', data)
}

export const cancelVerification = () => {
    return api.post('/api/v1/auth/verification/cancel')
}

export interface AdminContactVO {
    email?: string
    phone?: string
}

export const getAdminContact = () => {
    return api.get<AdminContactVO>('/api/v1/auth/admin-contact')
}
