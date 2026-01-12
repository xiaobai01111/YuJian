import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            name: 'Home',
            component: () => import('@/views/home/index.vue')
        },
        {
            path: '/confessions',
            name: 'Confessions',
            component: () => import('@/views/board/index.vue')
        },
        {
            path: '/treehole',
            name: 'TreeHole',
            component: () => import('@/views/board/index.vue')
        },
        {
            path: '/help',
            name: 'Help',
            component: () => import('@/views/board/index.vue')
        },
        {
            path: '/market',
            name: 'Market',
            component: () => import('@/views/board/index.vue')
        },
        {
            path: '/lost-found',
            name: 'LostFound',
            component: () => import('@/views/board/index.vue')
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
            component: () => import('@/views/console/layout/ConsoleLayout.vue'),
            redirect: '/console/dashboard',
            meta: { layout: 'div' },
            children: [
                {
                    path: 'dashboard',
                    name: 'ConsoleDashboard',
                    component: () => import('@/views/console/dashboard/index.vue'),
                    meta: { title: '仪表盘', icon: 'dashboard' }
                },
                {
                    path: 'user',
                    name: 'UserManagement',
                    component: () => import('@/views/console/user/index.vue'),
                    meta: { title: '用户管理', icon: 'user' }
                },
                {
                    path: 'role',
                    name: 'RoleManagement',
                    component: () => import('@/views/console/role/index.vue'),
                    meta: { title: '角色管理', icon: 'role' }
                },
                {
                    path: 'menu',
                    name: 'MenuManagement',
                    component: () => import('@/views/console/menu/index.vue'),
                    meta: { title: '菜单管理', icon: 'menu' }
                }
            ]
        },
        {
            path: '/feed',
            name: 'Feed',
            component: () => import('@/views/home/index.vue')
        }
    ]
})

router.beforeEach(async (to, _from, next) => {
    const userStore = useUserStore()
    const permissionStore = usePermissionStore()

    const whiteList = ['/', '/confessions', '/treehole', '/help', '/market', '/lost-found']

    if (userStore.token) {
         // Check if permissions are loaded (simple check for now)
         if (permissionStore.permissions.length === 0) {
            // Try to fetch permissions
            // In a real app we'd also fetch user info here
            await permissionStore.fetchPermissions()
            await permissionStore.generateRoutes()
            next()
         } else {
             next()
         }
    } else {
        if (whiteList.includes(to.path)) {
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
