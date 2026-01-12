import request from '@/utils/request'

// --- Interfaces ---

export interface MenuVO {
    id: number
    name: string
    parentId: number
    order: number
    path: string
    component: string
    query?: string
    isFrame: boolean
    isCache: boolean
    menuType: 'M' | 'C' | 'F'
    visible: boolean
    status: boolean
    perms?: string
    icon?: string
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
    userType?: number
    sex?: number
    verifyStatus?: number
    status: number
    creditScore?: number
    loginDate?: string
    createdAt: string
    roles: string[]
}

export interface RoleVO {
    id: number
    roleName: string
    roleKey: string
    status: number
    sortOrder: number
    menuIds?: number[]
    createTime: string
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
    status?: boolean
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
    menuIds?: number[]
}

// --- Menu & Router ---
export function getRoutes() {
    return request.get('/v1/system/menu/routes')
}

export function getMenuList(params?: any) {
    return request.get('/v1/system/menu/list', { params })
}

export function getMenuTree() {
    return request.get('/v1/system/menu/list')
}

export function createMenu(data: MenuDTO) {
    return request.post('/v1/system/menu', data)
}

export function updateMenu(id: number, data: MenuDTO) {
    return request.put(`/v1/system/menu/${id}`, data)
}

export function deleteMenu(id: number) {
    return request.delete(`/v1/system/menu/${id}`)
}

// --- User ---
export function getUserList(params?: any) {
    return request.get('/v1/console/users', { params })
}

export function updateUserRole(userId: number, roleIds: number[]) {
    return request.put(`/v1/console/users/${userId}/role`, { roleIds })
}

export function banUser(userId: number, status: number) {
    // status: 1 = ban, 0 = unban (normal)
    return request.put(`/v1/console/users/${userId}/ban`, { status })
}

// --- Role ---
export function getRoleList(params?: any) {
    return request.get('/v1/system/role/list', { params })
}

export function createRole(data: RoleDTO) {
    return request.post('/v1/system/role', data)
}

export function updateRole(id: number, data: RoleDTO) {
    return request.put(`/v1/system/role/${id}`, data)
}

export function deleteRole(roleId: number) {
    return request.delete(`/v1/system/role/${roleId}`)
}

export function assignRoleMenus(roleId: number, menuIds: number[]) {
    return request.put(`/v1/system/role/${roleId}/menus`, menuIds)
}
