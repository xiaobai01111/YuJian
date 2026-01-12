import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useUserStore } from './user'
import { getRoutes } from '@/api/system'

export interface RouteItem {
    name: string
    path: string
    component?: string
    meta?: {
        title: string
        icon?: string
        hidden?: boolean
        perms?: string
        keepAlive?: boolean
    }
    children?: RouteItem[]
}

export const usePermissionStore = defineStore('permission', () => {
    const permissions = ref<string[]>([])
    const routes = ref<RouteItem[]>([])

    function fetchPermissions() {
        // 从用户信息中获取权限（登录时已返回）
        const userStore = useUserStore()
        if (userStore.userInfo?.permissions) {
            permissions.value = userStore.userInfo.permissions
        } else {
            permissions.value = []
        }
    }

    function hasPermission(value: string | string[]): boolean {
        if (!value) return true
        // 超级管理员拥有所有权限
        const userStore = useUserStore()
        if (userStore.userInfo?.roles?.includes('admin')) {
            return true
        }
        const required = Array.isArray(value) ? value : [value]
        return required.some(p => permissions.value.includes(p))
    }

    async function generateRoutes() {
        try {
            // 从后端获取用户可访问的路由
            const backendRoutes: any = await getRoutes()
            
            if (backendRoutes && backendRoutes.length > 0) {
                // 转换后端路由格式为前端路由格式
                routes.value = transformRoutes(backendRoutes)
            } else {
                // 如果后端没有返回路由，使用默认路由（仅仪表盘和个人中心）
                routes.value = getDefaultRoutes()
            }
        } catch (error) {
            console.error('Failed to fetch routes:', error)
            // 出错时使用默认路由
            routes.value = getDefaultRoutes()
        }
        
        return routes.value
    }

    // 转换后端路由格式
    function transformRoutes(backendRoutes: any[]): RouteItem[] {
        return backendRoutes.map(route => {
            const item: RouteItem = {
                name: route.name,
                path: route.path || '',
                component: route.component,
                meta: {
                    title: route.meta?.title || route.name,
                    icon: route.meta?.icon,
                    hidden: route.meta?.hidden,
                    perms: route.meta?.perms,
                    keepAlive: route.meta?.keepAlive
                }
            }
            if (route.children && route.children.length > 0) {
                item.children = transformRoutes(route.children)
            }
            return item
        })
    }

    // 默认路由（所有用户都能访问）
    function getDefaultRoutes(): RouteItem[] {
        return [
            {
                path: '/console/dashboard',
                name: 'ConsoleDashboard',
                meta: { title: '仪表盘', icon: 'dashboard' }
            },
            {
                path: '/console/profile',
                name: 'ConsoleProfile',
                meta: { title: '个人中心', icon: 'user' }
            }
        ]
    }

    function clearPermissions() {
        permissions.value = []
        routes.value = []
    }

    return {
        permissions,
        routes,
        fetchPermissions,
        hasPermission,
        generateRoutes,
        clearPermissions
    }
})
