import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const usePermissionStore = defineStore('permission', () => {
    const permissions = ref<string[]>([])
    const routes = ref<any[]>([])

    async function fetchPermissions() {
        try {
            const data: any = await request.get('/system/user/permissions')
            permissions.value = data || []
        } catch (e) {
            console.error('Failed to fetch permissions', e)
            // Mock permissions for dev
            permissions.value = ['system:user:list', 'system:user:add', 'system:user:edit', 'system:user:remove', 'system:role:list', 'system:menu:list']
        }
    }

    function hasPermission(value: string | string[]): boolean {
        if (!value) return true
        const required = Array.isArray(value) ? value : [value]
        return required.some(p => permissions.value.includes(p))
    }

    async function generateRoutes() {
        // For now, we return the static routes defined for console to ensure Sidebar works
        // In real app, this would merge static + dynamic routes
        const consoleRoutes = [
            {
                path: '/console/dashboard',
                name: 'ConsoleDashboard',
                meta: { title: '仪表盘', icon: 'dashboard' }
            },
            {
                path: '/console/user',
                name: 'UserManagement',
                meta: { title: '用户管理', icon: 'user' }
            },
            {
                path: '/console/role',
                name: 'RoleManagement',
                meta: { title: '角色管理', icon: 'user' }
            },
            {
                path: '/console/menu',
                name: 'MenuManagement',
                meta: { title: '菜单管理', icon: 'dashboard' }
            }
        ]
        
        // Update the store state
        routes.value = consoleRoutes
        return consoleRoutes
    }

    return {
        permissions,
        routes,
        fetchPermissions,
        hasPermission,
        generateRoutes
    }
})
