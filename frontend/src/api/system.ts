import request from '@/utils/request'

// --- Interfaces ---

export interface MenuVO {
    id: number
    name: string
    parentId: number
    order?: number
    sortOrder?: number
    path: string
    component: string
    query?: string
    isFrame?: boolean
    isCache?: boolean
    type?: number
    menuType: 'M' | 'C' | 'F'
    visible: boolean
    status: number  // 0=启用, 1=禁用
    perms?: string
    icon?: string
    createdAt?: string
    children?: MenuVO[]
}

export interface UserVO {
    id: number
    username: string
    nickname: string
    email?: string
    phone?: string
    avatar?: string
    deptId?: number
    deptName?: string
    postId?: number
    postName?: string
    userType?: number
    sex?: number
    verifyStatus?: number
    status: number
    creditScore?: number
    loginDate?: string
    createdAt: string
    roles: string[]
    roleIds?: number[]
    remark?: string
}

export interface RoleVO {
    id: number
    roleName: string
    roleKey: string
    status: number
    sortOrder: number
    remark?: string
    menuIds?: number[]
    createdAt: string
}

export interface MenuDTO {
    parentId?: number
    menuType: 'M' | 'C' | 'F'
    icon?: string
    menuName: string
    orderNum: number
    isFrame?: boolean
    isCache?: boolean
    visible?: boolean
    status?: number  // 0=启用, 1=禁用
    path?: string
    component?: string
    query?: string
    perms?: string
}

export interface RoleDTO {
    roleName: string
    roleKey: string
    sortOrder: number
    status: number
    remark?: string
    menuIds?: number[]
}

// --- Menu & Router ---
export function getRoutes() {
    return request.get('/api/v1/system/menu/routes')
}

export function getMenuTree() {
    return request.get('/api/v1/system/menu/list')
}

// --- Dept ---
export interface DeptVO {
    id: number
    parentId: number
    deptName: string
    sortOrder: number
    leader?: string
    phone?: string
    email?: string
    status: number
    dataScope?: number
    createdAt?: string
    children?: DeptVO[]
}

export interface DeptDTO {
    parentId?: number
    deptName: string
    sortOrder?: number
    leader?: string
    phone?: string
    email?: string
    status?: number
    dataScope?: number
}

export function getDeptList() {
    return request.get('/api/v1/system/dept/list')
}

export function getDeptTree() {
    return request.get('/api/v1/system/dept/tree')
}

export function createDept(data: DeptDTO) {
    return request.post('/api/v1/system/dept', data)
}

export function updateDept(id: number, data: DeptDTO) {
    return request.put(`/api/v1/system/dept/${id}`, data)
}

export function deleteDept(id: number) {
    return request.delete(`/api/v1/system/dept/${id}`)
}

export type DeptUserStrategy = 'TRANSFER_PARENT' | 'UNASSIGN' | 'DELETE'

export interface DeptDeleteDTO {
    userStrategy?: DeptUserStrategy
    reason?: string
}

export function deleteDeptWithStrategy(id: number, data: DeptDeleteDTO) {
    return request.post(`/api/v1/system/dept/${id}/delete`, data)
}

export function updateDeptStatus(id: number, status: number) {
    return request.put(`/api/v1/system/dept/${id}/status`, { status })
}

export function getDeptUsers(deptId: number) {
    return request.get(`/api/v1/system/dept/${deptId}/users`)
}

export function getDeptUserCount(deptId: number) {
    return request.get(`/api/v1/system/dept/${deptId}/user-count`)
}

// --- User ---
export interface UserCreateDTO {
    username: string
    password: string
    nickname: string
    email?: string
    phone?: string
    deptId?: number
    roleId?: number
    sex?: number
    status?: number
    avatar?: string
    remark?: string
}

export interface UserEditDTO {
    nickname: string
    email?: string
    phone?: string
    deptId?: number
    roleId?: number
    sex?: number
    status?: number
    avatar?: string
    remark?: string
}

export function getUserList(params?: any) {
    return request.get('/api/v1/console/users', { params })
}

export function createUser(data: UserCreateDTO) {
    return request.post('/api/v1/console/users', data)
}

export function editUser(userId: number, data: UserEditDTO) {
    return request.put(`/api/v1/console/users/${userId}`, data)
}

export function deleteUsers(ids: number[], reason?: string) {
    return request.delete('/api/v1/console/users', { data: { ids, reason } })
}

export function updateUserRole(userId: number, roleIds: number[]) {
    return request.put(`/api/v1/console/users/${userId}/role`, { roleIds })
}

export function batchUpdateUserRole(userIds: number[], roleIds: number[]) {
    return request.put('/api/v1/console/users/batch-role', { userIds, roleIds })
}

export function batchAssignUsersByQuery(data: any) {
    return request.post('/api/v1/console/users/batch-assign', data)
}

export function banUser(userId: number, status: number, reason?: string) {
    return request.put(`/api/v1/console/users/${userId}/ban`, { status, reason })
}

export function exportUsers(params?: any) {
    return request.get('/api/v1/console/users/export', { params, responseType: 'blob' })
}

export function importUsers(file: File, updateExisting: boolean = false) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('updateExisting', String(updateExisting))
    return request.post('/api/v1/console/users/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

export function downloadUserTemplate() {
    return request.get('/api/v1/console/users/template', { responseType: 'blob' })
}

// --- Login Log ---
export interface LoginLogVO {
    id: number
    userId?: number
    username?: string
    ipaddr?: string
    loginLocation?: string
    browser?: string
    os?: string
    status: number
    msg?: string
    userAgent?: string
    loginTime?: string
}

export interface LoginLogQuery {
    page?: number
    size?: number
    username?: string
    ipaddr?: string
    status?: number
    loginTimeStart?: string
    loginTimeEnd?: string
}

export function getLoginLogList(params?: LoginLogQuery) {
    return request.get('/api/v1/console/login-logs', { params })
}

export function deleteLoginLog(id: number) {
    return request.delete(`/api/v1/console/login-logs/${id}`)
}

export function clearLoginLogs() {
    return request.delete('/api/v1/console/login-logs/clear')
}

export function exportLoginLogs(params?: LoginLogQuery) {
    return request.get('/api/v1/console/login-logs/export', { params, responseType: 'blob' })
}

// --- Online Users ---
export interface OnlineUserVO {
    token: string
    userId: number
    username?: string
    nickname?: string
    ipaddr?: string
    userAgent?: string
    loginTime?: string
    lastActiveTime?: number
    tokenTimeout?: number
    tokenActiveTimeout?: number
}

export interface OnlineUserQuery {
    page?: number
    size?: number
    keyword?: string
    ipaddr?: string
}

export interface PageResult<T> {
    records: T[]
    total: number
    size: number
    current: number
    pages: number
}

export function getOnlineUserList(params?: OnlineUserQuery) {
    return request.get<PageResult<OnlineUserVO>>('/api/v1/console/online-users', { params })
}

export function kickoutOnlineUser(token: string) {
    return request.post('/api/v1/console/online-users/kickout', { token })
}

// --- Server Monitor ---
export interface ServerMonitorVO {
    cpu: {
        coreCount: number
        userUsage: number
        systemUsage: number
        totalUsage: number
        idleUsage: number
    }
    memory: {
        total: string
        used: string
        free: string
        usage: number
    }
    jvm: {
        total: string
        used: string
        free: string
        max: string
        usage: number
    }
    server: {
        hostName: string
        hostIp: string
        osName: string
        osArch: string
    }
    javaInfo: {
        javaName: string
        javaVersion: string
        startTime: string
        runTime: string
        javaHome: string
        projectDir: string
        inputArgs: string
    }
    disks: Array<{
        mount: string
        fileSystem: string
        diskType: string
        total: string
        free: string
        used: string
        usage: number
    }>
}

export function getServerMonitor() {
    return request.get('/api/v1/console/monitor/server')
}

// --- Redis Monitor ---
export interface RedisMonitorVO {
    basic: {
        version: string
        runMode: string
        port: string
        connectedClients: string
        uptimeDays: string
        usedMemory: string
        usedCpu: string
        maxMemory: string
        aofEnabled: string
        rdbStatus: string
        keyCount: string
        networkInput: string
        networkOutput: string
    }
    memory: {
        used: string
        usedPeak: string
        usedRss: string
        usedLua: string
        fragmentationRatio: string
        max: string
    }
    commandStats: Array<{
        command: string
        calls: number
        usec: number
        usecPerCall: number | null
    }>
}

export function getRedisMonitor() {
    return request.get('/api/v1/console/monitor/redis')
}

// --- Oper Log ---
export interface OperLogVO {
    id: number
    operatorId?: number
    operatorName?: string
    targetType?: string
    targetId?: number
    action?: string
    reason?: string
    beforeValue?: any
    afterValue?: any
    ipAddress?: string
    createdAt?: string
}

export interface OperLogQuery {
    page?: number
    size?: number
    operatorName?: string
    targetType?: string
    action?: string
    startTime?: string
    endTime?: string
}

export function getOperLogList(params?: OperLogQuery) {
    return request.get('/api/v1/console/oper-logs', { params })
}

export function deleteOperLog(id: number) {
    return request.delete(`/api/v1/console/oper-logs/${id}`)
}

export function clearOperLogs() {
    return request.delete('/api/v1/console/oper-logs/clear')
}

export function exportOperLogs(params?: OperLogQuery) {
    return request.get('/api/v1/console/oper-logs/export', { params, responseType: 'blob' })
}

// --- Role ---
export function getRoleList(params?: any) {
    return request.get('/api/v1/system/roles/list', { params })
}

export function createRole(data: RoleDTO) {
    return request.post('/api/v1/system/roles', data)
}

export function updateRole(id: number, data: RoleDTO) {
    return request.put(`/api/v1/system/roles/${id}`, data)
}

export function getRoleMenuIds(roleId: number) {
    return request.get(`/api/v1/system/roles/${roleId}/menus`)
}

export function getRoleUsers(roleId: number) {
    return request.get(`/api/v1/system/roles/${roleId}/users`)
}

export function deleteRole(roleId: number, data?: { deleteUsers?: boolean; reason?: string }) {
    const payload = data ?? { deleteUsers: false }
    return request.post(`/api/v1/system/roles/${roleId}/delete`, payload)
}

export function deleteRoles(roleIds: number[]) {
    return request.delete('/api/v1/system/roles', { data: roleIds })
}

export function assignRoleMenus(roleId: number, menuIds: number[]) {
    return request.put(`/api/v1/system/roles/${roleId}/menus`, menuIds)
}

export function assignRoleDepts(roleId: number, deptIds: number[]) {
    return request.put(`/api/v1/system/roles/${roleId}/depts`, { deptIds })
}

export function getRoleDeptIds(roleId: number) {
    return request.get(`/api/v1/system/roles/${roleId}/depts`)
}

// --- Notice (公告) ---
export interface NoticeVO {
    id: number
    title: string
    content: string
    status: number  // 0草稿 1已发布 2已下线
    statusText: string
    scopeType: 'ALL' | 'DEPT' | 'USERS'
    scopeTypeText: string
    scopeIds?: number[]
    isPinned: boolean
    startAt?: string
    endAt?: string
    publishedAt?: string
    createdBy: number
    createdByName?: string
    createdAt: string
    updatedAt?: string
}

export interface NoticeDTO {
    title: string
    content: string
    scopeType?: 'ALL' | 'DEPT' | 'USERS'
    scopeIds?: number[]
    isPinned?: boolean
    startAt?: string
    endAt?: string
}

// 公开接口（未登录可访问）
export function getPublicNotices(limit: number = 10) {
    return request.get('/api/v1/notices/public', { params: { limit } })
}

export function getPublicNoticeDetail(id: number) {
    return request.get(`/api/v1/notices/public/${id}`)
}

// 登录用户接口
export function getVisibleNotices(page: number = 1, size: number = 10) {
    return request.get('/api/v1/notices', { params: { page, size } })
}

export function getVisibleNoticeDetail(id: number) {
    return request.get(`/api/v1/notices/${id}`)
}

// 后台管理接口
export function queryNotices(params: { page?: number; size?: number; status?: number; keyword?: string }) {
    return request.get('/api/v1/console/notices', { params })
}

export function getNoticeDetail(id: number) {
    return request.get(`/api/v1/console/notices/${id}`)
}

export function createNotice(data: NoticeDTO) {
    return request.post('/api/v1/console/notices', data)
}

export function updateNotice(id: number, data: NoticeDTO) {
    return request.put(`/api/v1/console/notices/${id}`, data)
}

export function publishNotice(id: number) {
    return request.put(`/api/v1/console/notices/${id}/publish`)
}

export function offlineNotice(id: number) {
    return request.put(`/api/v1/console/notices/${id}/offline`)
}

export function deleteNotice(id: number) {
    return request.delete(`/api/v1/console/notices/${id}`)
}

// --- Sensitive Words (敏感词) ---
export interface SensitiveWordVO {
    id: number
    word: string
    level: number
    createdAt?: string
}

export interface SensitiveWordDTO {
    word: string
    level?: number
}

export function querySensitiveWords(params: { page?: number; size?: number; level?: number; keyword?: string }) {
    return request.get('/api/v1/system/sensitive-words', { params })
}

export function createSensitiveWord(data: SensitiveWordDTO) {
    return request.post('/api/v1/system/sensitive-words', data)
}

export function createSensitiveWordsBatch(data: { words: string[]; level?: number }) {
    return request.post('/api/v1/system/sensitive-words/batch', data)
}

export function deleteSensitiveWord(id: number) {
    return request.delete(`/api/v1/system/sensitive-words/${id}`)
}

export function deleteSensitiveWords(ids: number[]) {
    return request.delete('/api/v1/system/sensitive-words', { data: ids })
}

// --- Auth Rules (认证规则) ---
export interface AuthRuleVO {
    id: number
    name: string
    enabled: boolean
    triggerType: string
    verifyMethod?: string
    matchType?: string
    matchValue?: string
    roleIds?: number[]
    roleNames?: string[]
    deptId?: number
    deptName?: string
    priority?: number
    remark?: string
    createdAt?: string
    updatedAt?: string
}

export interface AuthRuleDTO {
    name: string
    enabled?: boolean
    triggerType: string
    verifyMethod?: string
    matchType?: string
    matchValue?: string
    roleIds?: number[]
    deptId?: number
    priority?: number
    remark?: string
}

export function queryAuthRules(params: { page?: number; size?: number; triggerType?: string; verifyMethod?: string; enabled?: boolean }) {
    return request.get('/api/v1/system/auth-rules', { params })
}

export function createAuthRule(data: AuthRuleDTO) {
    return request.post('/api/v1/system/auth-rules', data)
}

export function updateAuthRule(id: number, data: AuthRuleDTO) {
    return request.put(`/api/v1/system/auth-rules/${id}`, data)
}

export function deleteAuthRule(id: number) {
    return request.delete(`/api/v1/system/auth-rules/${id}`)
}

// --- Statistics (统计) ---
export interface DashboardStats {
    totalUsers: number
    todayNewUsers: number
    totalPosts: number
    todayPosts: number
    yesterdayPosts: number
    postGrowth: number
    userGrowth: number
    pendingVerifications?: number
    pendingReports?: number
    noticeTotal?: number
    noticePublished?: number
    noticeDraft?: number
    noticeOffline?: number
    noticePinned?: number
    noticeExpiringSoon?: number
    sensitiveWords?: number
    loginSuccessToday?: number
    loginFailToday?: number
    loginTotalToday?: number
}

export function getDashboardStats() {
    return request.get<DashboardStats>('/api/v1/console/statistics/dashboard')
}

export interface RecentActivity {
    userId: number
    nickname: string
    avatar: string | null
    action: string
    time: string
    status: number
    postId: number
}

export function getRecentActivities() {
    return request.get<RecentActivity[]>('/api/v1/console/statistics/recent-activities')
}

export interface RecentNotice {
    id: number
    title: string
    isPinned: boolean
    publishedAt: string
    status: number
}

export function getRecentNotices() {
    return request.get<RecentNotice[]>('/api/v1/console/statistics/recent-notices')
}

export interface RecentReport {
    id: number
    reason: string
    status: number
    createdAt: string
    reporterName: string
    postId: number
}

export function getRecentReports() {
    return request.get<RecentReport[]>('/api/v1/console/statistics/recent-reports')
}

export interface RecentVerification {
    id: number
    status: number
    createdAt: string
    userId: number
    nickname: string
}

export function getRecentVerifications() {
    return request.get<RecentVerification[]>('/api/v1/console/statistics/recent-verifications')
}

export interface RecentOperLog {
    id: number
    operatorName: string | null
    action: string
    targetType: string
    ipAddress: string | null
    createdAt: string
}

export function getRecentOperLogs() {
    return request.get<RecentOperLog[]>('/api/v1/console/statistics/recent-oper-logs')
}

export interface LoginLogTrendItem {
    date: string
    success: number
    fail: number
}

export function getLoginLogTrend() {
    return request.get<LoginLogTrendItem[]>('/api/v1/console/statistics/login-log-trend')
}
