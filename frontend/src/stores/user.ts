import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi, type LoginDTO, type RegisterDTO, type UserInfoVO } from '@/api/auth'
export const useUserStore = defineStore('user', () => {
    const token = ref<string | null>(localStorage.getItem('token'))
    const userInfo = ref<UserInfoVO | null>(null)
    
    // Try to restore user info from local storage if available
    const savedUserInfo = localStorage.getItem('userInfo')
    if (savedUserInfo) {
        try {
            userInfo.value = JSON.parse(savedUserInfo)
        } catch (e) {
            console.error('Failed to parse saved user info', e)
        }
    }

    function setToken(newToken: string) {
        token.value = newToken
        localStorage.setItem('token', newToken)
    }

    function setUserInfo(info: UserInfoVO) {
        userInfo.value = info
        localStorage.setItem('userInfo', JSON.stringify(info))
    }

    function clearToken() {
        token.value = null
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    async function login(loginForm: LoginDTO) {
        try {
            const data = await loginApi(loginForm)
            setToken(data.token)
            setUserInfo(data.userInfo)
            return data
        } catch (error) {
            throw error
        }
    }

    async function register(registerForm: RegisterDTO) {
        try {
            return await registerApi(registerForm)
        } catch (error) {
            throw error
        }
    }

    async function logout() {
        clearToken()
        try { await logout() } catch {}
        clearToken()
    }

    // 检查用户是否拥有指定权限
    function hasPermission(perm: string): boolean {
        if (!userInfo.value?.permissions) return false
        // 超级管理员拥有所有权限
        if (userInfo.value.permissions.includes('*')) return true
        return userInfo.value.permissions.includes(perm)
    }

    return {
        token,
        userInfo,
        setToken,
        logout,
        login,
        register,
        hasPermission
    }
})
