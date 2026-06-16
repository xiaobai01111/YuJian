import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
    login as loginApi,
    register as registerApi,
    logout as logoutApi,
    refreshAccessToken,
    type LoginDTO,
    type RegisterDTO,
    type UserInfoVO
} from '@/api/auth'
import { clearRefreshToken, writeRefreshToken } from '@/utils/refreshTokenStorage'

export const useUserStore = defineStore('user', () => {
    const token = ref<string | null>(null)
    const userInfo = ref<UserInfoVO | null>(null)
    const restoreAttempted = ref(false)
    let restorePromise: Promise<boolean> | null = null

    function setToken(newToken?: string | null) {
        token.value = newToken && newToken.trim() ? newToken : null
    }

    function setUserInfo(info?: UserInfoVO | null) {
        userInfo.value = info ?? null
    }

    function clearToken() {
        token.value = null
        userInfo.value = null
        clearRefreshToken()
    }

    function forceLogout() {
        restoreAttempted.value = true
        clearToken()
    }

    async function restoreSession(force = false): Promise<boolean> {
        if (token.value) {
            return true
        }
        if (!force && restoreAttempted.value) {
            return false
        }
        if (restorePromise) {
            return restorePromise
        }
        restoreAttempted.value = true
        restorePromise = (async () => {
            try {
                const data = await refreshAccessToken()
                if (!data?.token) {
                    clearToken()
                    return false
                }
                setToken(data.token)
                setUserInfo(data.userInfo)
                writeRefreshToken(data.refreshToken)
                return true
            } catch {
                clearToken()
                return false
            } finally {
                restorePromise = null
            }
        })()
        return restorePromise
    }

    async function login(loginForm: LoginDTO) {
        const data = await loginApi(loginForm)
        restoreAttempted.value = true
        setToken(data.token)
        setUserInfo(data.userInfo)
        writeRefreshToken(data.refreshToken)
        return data
    }

    async function register(registerForm: RegisterDTO) {
        return registerApi(registerForm)
    }

    async function logout() {
        const currentToken = token.value
        restoreAttempted.value = true
        clearToken()
        if (!currentToken) return
        try {
            await logoutApi(currentToken)
        } catch {
            // ignore
        }
    }

    function hasPermission(perm: string): boolean {
        if (!userInfo.value?.permissions) return false
        if (userInfo.value.permissions.includes('*')) return true
        return userInfo.value.permissions.includes(perm)
    }

    return {
        token,
        userInfo,
        setToken,
        setUserInfo,
        forceLogout,
        clearToken,
        restoreSession,
        logout,
        login,
        register,
        hasPermission
    }
})
