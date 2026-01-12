import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useUserStore } from './user'

export const usePermissionStore = defineStore('permission', () => {
    const permissions = ref<string[]>([])
    const routes = ref<any[]>([])

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
        const required = Array.isArray(value) ? value : [value]
        return required.some(p => permissions.value.includes(p))
    }

    async function generateRoutes() {
        // 控制台路由，包含父子级菜单结构
        const consoleRoutes = [
            {
                path: '/console/dashboard',
                name: 'ConsoleDashboard',
                meta: { title: '仪表盘', icon: 'dashboard' }
            },
            {
                path: '/console/system',
                name: 'SystemManagement',
                meta: { title: '系统管理', icon: 'setting' },
                children: [
                    {
                        path: '/console/user',
                        name: 'UserManagement',
                        meta: { title: '用户管理', icon: 'user' }
                    },
                    {
                        path: '/console/role',
                        name: 'RoleManagement',
                        meta: { title: '角色管理', icon: 'peoples' }
                    },
                    {
                        path: '/console/menu',
                        name: 'MenuManagement',
                        meta: { title: '菜单管理', icon: 'tree-table' }
                    },
                    {
                        path: '/console/dict',
                        name: 'DictManagement',
                        meta: { title: '字典管理', icon: 'dict' }
                    },
                    {
                        path: '/console/dept',
                        name: 'DeptManagement',
                        meta: { title: '部门管理', icon: 'tree' }
                    },
                    {
                        path: '/console/post',
                        name: 'PostManagement',
                        meta: { title: '岗位管理', icon: 'post' }
                    }
                ]
            },
            {
                path: '/console/log',
                name: 'LogManagement',
                meta: { title: '日志管理', icon: 'log' },
                children: [
                    {
                        path: '/console/login-log',
                        name: 'LoginLog',
                        meta: { title: '登录日志', icon: 'logininfor' }
                    },
                    {
                        path: '/console/oper-log',
                        name: 'OperLog',
                        meta: { title: '操作日志', icon: 'form' }
                    }
                ]
            },
            {
                path: '/console/profile',
                name: 'ConsoleProfile',
                meta: { title: '个人中心', icon: 'user' }
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
