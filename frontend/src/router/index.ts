import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
import type { RouteItem } from '@/stores/permission'
import { getSetupStatus } from '@/api/setup'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'Home',
            component: () => import('@/views/home/index.vue'),
            meta: { heroKey: 'HOME' }
        },
        {
            path: '/setup',
            name: 'Setup',
            component: () => import('@/views/setup/index.vue'),
            meta: { layout: 'div' }
        },
        {
            path: '/confessions',
            name: 'Confessions',
            component: () => import('@/views/confessions/index.vue'),
            meta: { heroKey: 'CONFESSIONS' }
        },
        {
            path: '/treehole',
            name: 'TreeHole',
            component: () => import('@/views/treehole/index.vue'),
            meta: { heroKey: 'TREEHOLE' }
        },
        {
            path: '/help',
            name: 'Help',
            component: () => import('@/views/help/index.vue'),
            meta: { heroKey: 'HELP' }
        },
        {
            path: '/market',
            name: 'Market',
            component: () => import('@/views/market/index.vue'),
            meta: { heroKey: 'MARKET' }
        },
        {
            path: '/lost-found',
            name: 'LostFound',
            component: () => import('@/views/lost-found/index.vue'),
            meta: { heroKey: 'LOST_FOUND' }
        },
        {
            path: '/notices',
            name: 'NoticeList',
            component: () => import('@/views/notice/index.vue')
        },
        {
            path: '/notices/:id',
            name: 'NoticeDetail',
            component: () => import('@/views/notice/detail.vue')
        },
        {
            path: '/posts/:id',
            name: 'PostDetail',
            component: () => import('@/views/post/detail.vue')
        },
        {
            path: '/publish',
            name: 'PublishPost',
            component: () => import('@/views/post/publish.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/search',
            name: 'Search',
            component: () => import('@/views/search/index.vue')
        },
        {
            path: '/user/:id',
            name: 'UserProfile',
            component: () => import('@/views/user/profile.vue'),
            meta: { requiresAuth: true }
        },
        {
            path: '/console',
            name: 'Console',
            component: () => import('@/views/console/layout/ConsoleLayout.vue'),
            redirect: '/console/dashboard',
            meta: { layout: 'div' },
            children: [
                // 动态路由将通过 router.addRoute 添加
                // 基础路由保留，确保刷新时不会404
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
        },
        {
            path: '/feed',
            name: 'Feed',
            component: () => import('@/views/home/index.vue')
        },
        {
            path: '/403',
            name: 'Forbidden',
            component: () => import('@/views/console/no-permission/index.vue'),
            meta: { layout: 'div', public: true }
        },
        {
            path: '/404',
            name: 'NotFound',
            component: () => import('@/views/error/404.vue'),
            meta: { layout: 'div', public: true }
        },
        {
            path: '/:pathMatch(.*)*',
            name: 'CatchAllNotFound',
            component: () => import('@/views/error/404.vue'),
            meta: { layout: 'div', public: true }
        }
    ]
})

let setupChecked = false
let setupCompleted = true

function resolveConsolePath(path: string, parentPath: string): string {
    if (!path) return parentPath
    if (path.startsWith('/')) return path
    if (!parentPath) return `/console/${path}`
    return parentPath.endsWith('/') ? `${parentPath}${path}` : `${parentPath}/${path}`
}

function hasRouteAccess(targetPath: string, routes: RouteItem[], parentPath = ''): boolean {
    for (const route of routes) {
        const currentPath = resolveConsolePath(route.path, parentPath)
        if (currentPath === targetPath) {
            return true
        }
        if (route.children && route.children.length > 0 && hasRouteAccess(targetPath, route.children, currentPath)) {
            return true
        }
    }
    return false
}

router.beforeEach(async (to, _from, next) => {
    const userStore = useUserStore()
    const permissionStore = usePermissionStore()

    if (!setupChecked) {
        try {
            const res = await getSetupStatus()
            setupCompleted = Boolean(res?.setupCompleted)
        } catch (error) {
            setupCompleted = true
        } finally {
            setupChecked = true
        }
    }
    if (!setupCompleted && to.path !== '/setup') {
        next('/setup')
        return
    }
    if (setupCompleted && to.path === '/setup') {
        next('/')
        return
    }

    // 未登录允许访问的路径
    const whiteList = ['/', '/notices', '/setup']
    const isPublicErrorRoute = to.matched.some(record => record.meta?.public === true)
    const isWhitelisted = whiteList.includes(to.path) || to.path.startsWith('/notices/') || isPublicErrorRoute

    if (userStore.token) {
         // 动态路由未加载时，获取权限并注册路由
         if (!permissionStore.dynamicRoutesAdded) {
            await permissionStore.fetchPermissions()
            await permissionStore.generateRoutes(router)
            // 重新导航以确保动态路由生效
            next({ ...to, replace: true })
         } else {
             // 检查后台权限
             if (to.path.startsWith('/console')) {
                 // 检查用户是否有任何后台菜单权限
                 if (!permissionStore.hasConsoleMenus) {
                     next('/403')
                     return
                 }
                 // 检查具体路由权限（排除基础路由 dashboard 和 profile）
                 const basePaths = ['/console/dashboard', '/console/profile', '/console']
                 if (!basePaths.includes(to.path)) {
                     if (!hasRouteAccess(to.path, permissionStore.routes)) {
                         // 路由不在用户权限范围内，重定向到仪表盘
                         next('/console/dashboard')
                         return
                     }
                 }
             }
             next()
         }
    } else {
        if (permissionStore.dynamicRoutesAdded) {
            permissionStore.clearPermissions()
        }
        if (isWhitelisted) {
             next()
        } else {
            // If trying to access protected route (like /console), user needs to login. 
            // Since we use modal, we might redirect to home or show alert. 
            // For now, redirect to home.
            next('/')
        }
    }
})

export default router
