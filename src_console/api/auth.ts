import request from '@/utils/request'
import type { PostVO } from './post'
import { readRefreshToken } from '@/utils/refreshTokenStorage'

export interface LoginDTO {
    username: string
    password: string
    captchaId?: string
    captchaCode?: string
}

export interface LoginVO {
    token: string
    tokenExpiresIn?: number
    refreshToken?: string
    refreshTokenExpiresIn?: number
    userInfo: UserInfoVO
}

export interface LoginCaptchaVO {
    captchaId: string
    challenge?: string
    captchaImage?: string
    expireSeconds: number
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
    return request.post<number>('/api/v1/auth/register', data)
}

export const sendRegisterEmailCode = (email: string) => {
    return request.post<void>('/api/v1/auth/register-email-code', { email })
}

export const login = (data: LoginDTO) => {
    return request.post<LoginVO>('/api/v1/auth/login', data)
}

export const getLoginCaptcha = () => {
    return request.get<LoginCaptchaVO>('/api/v1/auth/login-captcha')
}

export const refreshAccessToken = (refreshToken?: string) => {
    const storedToken = readRefreshToken()
    const payload = refreshToken
        ? { refreshToken }
        : storedToken
            ? { refreshToken: storedToken }
            : {}
    return request.post<LoginVO>('/api/v1/auth/refresh', payload)
}

export const logout = (token?: string) => {
    const config = token
        ? { headers: { Authorization: `Bearer ${token}` } }
        : undefined
    return request.post<void>('/api/v1/auth/logout', undefined, config)
}

export const getUserInfo = () => {
    return request.get<UserInfoVO>('/api/v1/auth/info')
}

export interface UpdatePasswordDTO {
    oldPassword: string
    newPassword: string
    confirmPassword: string
}

export const updatePassword = (data: UpdatePasswordDTO) => {
    return request.put<void>('/api/v1/auth/password', data)
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
    return request.get<UserProfileVO>('/api/v1/users/me')
}

export const updateProfile = (data: UserProfileUpdateDTO) => {
    return request.post<void>('/api/v1/users/me', data)
}

export interface UserPostsResult {
    records: PostVO[]
    total: number
}

export const getMyPosts = (page = 1, size = 10) => {
    return request.get<UserPostsResult>('/api/v1/users/me/posts', { params: { page, size } })
}

export const getMyBookmarks = (page = 1, size = 10) => {
    return request.get<UserPostsResult>('/api/v1/users/bookmarks', { params: { page, size } })
}

export const getMyCreditScore = () => {
    return request.get<number>('/api/v1/users/credit')
}

export const verifyEmail = (eduEmail: string) => {
    return request.post<void>('/api/v1/auth/verify-email', { eduEmail })
}

export const confirmEmail = (code: string) => {
    return request.post<void>('/api/v1/auth/confirm-email', { code })
}

export interface SubmitIdCardDTO {
    imageUrl: string
    studentId?: string
}

export const submitIdCard = (data: SubmitIdCardDTO) => {
    return request.post<number>('/api/v1/auth/submit-id-card', data)
}

export interface SubmitStudentIdDTO {
    studentId: string
}

export const submitStudentId = (data: SubmitStudentIdDTO) => {
    return request.post<number>('/api/v1/auth/submit-student-id', data)
}

export const cancelVerification = () => {
    return request.post<void>('/api/v1/auth/verification/cancel')
}

export interface AdminContactVO {
    email?: string
    phone?: string
}

export const getAdminContact = () => {
    return request.get<AdminContactVO>('/api/v1/auth/admin-contact')
}
