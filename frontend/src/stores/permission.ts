import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useUserStore } from './user'
import { getRoutes } from '@/api/system'
import { resolveComponent } from '@/router/modules'
import type { RouteRecordRaw } from 'vue-router'

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
    const dynamicRoutesAdded = ref(false)

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
        // 超级管理员拥有所有权限（由后端下发 * 权限）
        const userStore = useUserStore()
        if (userStore.userInfo?.permissions?.includes('*')) {
            return true
        }
        const required = Array.isArray(value) ? value : [value]
        return required.some(p => permissions.value.includes(p))
    }

    async function generateRoutes(router: any) {
        if (dynamicRoutesAdded.value) {
            return routes.value
        }

        try {
            // 从后端获取用户可访问的路由
            const backendRoutes: any = await getRoutes()
            
            if (backendRoutes && backendRoutes.length > 0) {
                // 转换后端路由格式为前端路由格式
                routes.value = transformRoutes(backendRoutes)
                // 动态添加路由到 router
                const dynamicRoutes = buildDynamicRoutes(backendRoutes)
                dynamicRoutes.forEach((route: RouteRecordRaw) => {
                    router.addRoute('Console', route)
                })
            } else {
                // 如果后端没有返回路由，使用默认路由
                routes.value = getDefaultRoutes()
                addDefaultRoutes(router)
            }
            
            dynamicRoutesAdded.value = true
        } catch (error) {
            console.error('Failed to fetch routes:', error)
            // 出错时使用默认路由
            routes.value = getDefaultRoutes()
            addDefaultRoutes(router)
            dynamicRoutesAdded.value = true
        }
        
        return routes.value
    }

    // 构建动态路由（用于 router.addRoute）
    // 只添加叶子菜单作为路由，父级菜单仅用于侧边栏组织
    function buildDynamicRoutes(backendRoutes: any[]): RouteRecordRaw[] {
        const routes: RouteRecordRaw[] = []
        
        function collectLeafRoutes(menus: any[]) {
            for (const menu of menus) {
                if (menu.children && menu.children.length > 0) {
                    // 递归处理子菜单
                    collectLeafRoutes(menu.children)
                } else if (menu.component && menu.component !== 'Layout') {
                    // 叶子菜单，添加为路由
                    routes.push({
                        path: extractPath(menu.path),
                        name: menu.name || generateRouteName(menu.path),
                        component: resolveComponent(menu.component),
                        meta: {
                            title: menu.meta?.title || menu.name,
                            icon: menu.meta?.icon,
                            hidden: menu.meta?.hidden === true,
                            perms: menu.meta?.perms
                        }
                    } as RouteRecordRaw)
                }
            }
        }
        
        collectLeafRoutes(backendRoutes)
        return routes
    }

    // 从完整路径中提取相对路径（去掉 /console 前缀）
    function extractPath(fullPath: string): string {
        if (!fullPath) return ''
        // 移除开头的 /console/ 或 /console
        return fullPath.replace(/^\/console\/?/, '') || 'dashboard'
    }

    // 生成路由名称
    function generateRouteName(path: string): string {
        if (!path) return 'Unknown'
        const parts = path.split('/').filter(Boolean)
        return parts.map(p => p.charAt(0).toUpperCase() + p.slice(1)).join('')
    }

    // 添加默认路由
    function addDefaultRoutes(router: any) {
        const defaultRoutes: RouteRecordRaw[] = [
            {
                path: 'dashboard',
                name: 'ConsoleDashboard',
                component: () => import('@/views/console/dashboard/index.vue'),
                meta: { title: '仪表盘', icon: 'dashboard' }
            },
            {
                path: 'profile',
                name: 'ConsoleProfile',
                component: () => import('@/views/console/profile/index.vue'),
                meta: { title: '个人中心', icon: 'user' }
            }
        ]
        defaultRoutes.forEach(route => {
            router.addRoute('Console', route)
        })
    }

    // 转换后端路由格式（用于菜单渲染）
    function transformRoutes(backendRoutes: any[]): RouteItem[] {
        return backendRoutes.map(route => {
            const item: RouteItem = {
                name: route.name,
                path: route.path || '',
                component: route.component,
                meta: {
                    title: route.meta?.title || route.name,
                    icon: route.meta?.icon,
                    hidden: route.meta?.hidden === true,
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
        dynamicRoutesAdded.value = false
    }

    return {
        permissions,
        routes,
        dynamicRoutesAdded,
        fetchPermissions,
        hasPermission,
        generateRoutes,
        clearPermissions
    }
})
