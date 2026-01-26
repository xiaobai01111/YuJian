import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'
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
            meta: {
                heroKey: 'CONFESSIONS',
                hero: {
                    titleStart: '勇敢表达',
                    titleHighlight: '爱的声音',
                    description: '暗恋、表白、祝福。在这里，大声说出你的爱。让心意传递，让缘分开始。',
                    theme: 'pink',
                    badge: 'Confessions Wall',
                    primaryBtnText: '发布表白',
                    secondaryBtnText: '最新表白',
                    floatCardLabel: '今日表白',
                    floatCardValue: '99+ 条'
                }
            }
        },
        {
            path: '/treehole',
            name: 'TreeHole',
            component: () => import('@/views/treehole/index.vue'),
            meta: {
                heroKey: 'TREEHOLE',
                hero: {
                    titleStart: '倾听内心',
                    titleHighlight: '真实树洞',
                    description: '匿名倾诉，释放压力。在这里，做最真实的自己。我们是你忠实的倾听者。',
                    theme: 'emerald',
                    badge: 'Anonymous Treehole',
                    primaryBtnText: '发布心声',
                    secondaryBtnText: '查看树洞',
                    floatCardLabel: '新收录',
                    floatCardValue: '58 个秘密'
                }
            }
        },
        {
            path: '/help',
            name: 'Help',
            component: () => import('@/views/help/index.vue'),
            meta: {
                heroKey: 'HELP',
                hero: {
                    titleStart: '互帮互助',
                    titleHighlight: '共同成长',
                    description: '学业困惑、生活难题、求职经验。在这里，寻找答案，分享经验，温暖彼此。',
                    theme: 'blue',
                    badge: 'Q&A Help',
                    primaryBtnText: '发起求助',
                    secondaryBtnText: '我来解答',
                    floatCardLabel: '已解决',
                    floatCardValue: '1,203 个问题'
                }
            }
        },
        {
            path: '/market',
            name: 'Market',
            component: () => import('@/views/market/index.vue'),
            meta: {
                heroKey: 'MARKET',
                hero: {
                    titleStart: '旧物新生',
                    titleHighlight: '跳蚤市场',
                    description: '教材书籍、数码电子、生活用品。在这里，让闲置物品流转，发现物美价廉的宝贝。',
                    theme: 'orange',
                    badge: 'Flea Market',
                    primaryBtnText: '发布闲置',
                    secondaryBtnText: '逛逛市场',
                    floatCardLabel: '今日上新',
                    floatCardValue: '45 件好物'
                }
            }
        },
        {
            path: '/lost-found',
            name: 'LostFound',
            component: () => import('@/views/lost-found/index.vue'),
            meta: {
                heroKey: 'LOST_FOUND',
                hero: {
                    titleStart: '寻找失物',
                    titleHighlight: '传递温暖',
                    description: '丢失物品、捡到失物。在这里，发布信息，让物品回归主人，让善意流转。',
                    theme: 'purple',
                    badge: 'Lost & Found',
                    primaryBtnText: '发布信息',
                    secondaryBtnText: '最近信息',
                    floatCardLabel: '寻回率',
                    floatCardValue: '85%'
                }
            }
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
                    path: 'dept',
                    name: 'DeptManagement',
                    component: () => import('@/views/console/dept/index.vue'),
                    meta: { title: '部门管理', icon: 'tree' }
                },
                {
                    path: 'auth-rule',
                    name: 'AuthRuleManagement',
                    component: () => import('@/views/console/auth-rule/index.vue'),
                    meta: { title: '认证规则', icon: 'id-card' }
                },
                {
                    path: 'sensitive-word',
                    name: 'SensitiveWordManagement',
                    component: () => import('@/views/console/sensitive-word/index.vue'),
                    meta: { title: '敏感词管理', icon: 'warning' }
                },
                {
                    path: 'login-log',
                    name: 'LoginLog',
                    component: () => import('@/views/console/login-log/index.vue'),
                    meta: { title: '登录日志', icon: 'logininfor' }
                },
                {
                    path: 'oper-log',
                    name: 'OperLog',
                    component: () => import('@/views/console/oper-log/index.vue'),
                    meta: { title: '操作日志', icon: 'form' }
                },
                {
                    path: 'notice',
                    name: 'NoticeManagement',
                    component: () => import('@/views/console/notice/index.vue'),
                    meta: { title: '公告管理', icon: 'bell' }
                },
                {
                    path: 'post',
                    name: 'PostManagement',
                    component: () => import('@/views/console/post/index.vue'),
                    meta: { title: '帖子管理', icon: 'post' }
                },
                {
                    path: 'comment',
                    name: 'CommentManagement',
                    component: () => import('@/views/console/comment/index.vue'),
                    meta: { title: '评论管理', icon: 'message' }
                },
                {
                    path: 'report',
                    name: 'ReportManagement',
                    component: () => import('@/views/console/report/index.vue'),
                    meta: { title: '举报管理', icon: 'warning' }
                },
                {
                    path: 'recycle/post',
                    name: 'RecyclePost',
                    component: () => import('@/views/console/recycle/post.vue'),
                    meta: { title: '帖子回收站', icon: 'recycle' }
                },
                {
                    path: 'recycle/comment',
                    name: 'RecycleComment',
                    component: () => import('@/views/console/recycle/comment.vue'),
                    meta: { title: '评论回收站', icon: 'recycle' }
                },
                {
                    path: 'recycle/report',
                    name: 'RecycleReport',
                    component: () => import('@/views/console/recycle/report.vue'),
                    meta: { title: '举报回收站', icon: 'recycle' }
                },
                {
                    path: 'announcement',
                    name: 'AnnouncementManagement',
                    component: () => import('@/views/console/notice/index.vue'),
                    meta: { title: '公告管理', icon: 'bell' }
                },
                {
                    path: 'dashboard/notice',
                    name: 'DashboardNoticeManagement',
                    component: () => import('@/views/console/notice/index.vue'),
                    meta: { title: '公告管理', icon: 'bell' }
                },
                {
                    path: 'profile',
                    name: 'ConsoleProfile',
                    component: () => import('@/views/console/profile/index.vue'),
                    meta: { title: '个人中心', icon: 'user' }
                },
                {
                    path: 'monitor/online',
                    name: 'MonitorOnline',
                    component: () => import('@/views/console/monitor/online/index.vue'),
                    meta: { title: '在线用户', icon: 'users' }
                },
                {
                    path: 'monitor/server',
                    name: 'MonitorServer',
                    component: () => import('@/views/console/monitor/server/index.vue'),
                    meta: { title: '服务监控', icon: 'server' }
                },
                {
                    path: 'monitor/redis',
                    name: 'MonitorRedis',
                    component: () => import('@/views/console/monitor/redis/index.vue'),
                    meta: { title: 'Redis监控', icon: 'redis' }
                },
                {
                    path: 'monitor/blocklist',
                    name: 'MonitorBlocklist',
                    component: () => import('@/views/console/monitor/blocklist/index.vue'),
                    meta: { title: '阻止名单', icon: 'block' }
                },
                {
                    path: 'tool/file',
                    name: 'ToolFile',
                    component: () => import('@/views/console/tool/file/index.vue'),
                    meta: { title: '文件管理', icon: 'file' }
                },
                {
                    path: 'tool/gallery',
                    name: 'ToolGallery',
                    component: () => import('@/views/console/tool/gallery/index.vue'),
                    meta: { title: '图库管理', icon: 'image' }
                },
                {
                    path: 'campus/hero',
                    name: 'CampusHeroManagement',
                    component: () => import('@/views/console/campus/hero/index.vue'),
                    meta: { title: 'Hero管理', icon: 'image' }
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

let setupChecked = false
let setupCompleted = true

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
    const isWhitelisted = whiteList.includes(to.path) || to.path.startsWith('/notices/')

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
