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
    dataScope?: number
    remark?: string
    menuIds?: number[]
    deptIds?: number[]
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
    dataScope?: number
    remark?: string
    menuIds?: number[]
    deptIds?: number[]
}

// --- Menu & Router ---
export function getRoutes() {
    return request.get('/v1/system/menu/routes')
}

export function getMenuTree() {
    return request.get('/v1/system/menu/list')
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
}

export function getDeptList() {
    return request.get('/v1/system/dept/list')
}

export function getDeptTree() {
    return request.get('/v1/system/dept/tree')
}

export function createDept(data: DeptDTO) {
    return request.post('/v1/system/dept', data)
}

export function updateDept(id: number, data: DeptDTO) {
    return request.put(`/v1/system/dept/${id}`, data)
}

export function deleteDept(id: number) {
    return request.delete(`/v1/system/dept/${id}`)
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
    return request.get('/v1/console/users', { params })
}

export function createUser(data: UserCreateDTO) {
    return request.post('/v1/console/users', data)
}

export function editUser(userId: number, data: UserEditDTO) {
    return request.put(`/v1/console/users/${userId}`, data)
}

export function deleteUsers(ids: number[]) {
    return request.delete('/v1/console/users', { data: ids })
}

export function updateUserRole(userId: number, roleIds: number[]) {
    return request.put(`/v1/console/users/${userId}/role`, { roleIds })
}

export function batchUpdateUserRole(userIds: number[], roleIds: number[]) {
    return request.put('/v1/console/users/batch-role', { userIds, roleIds })
}

export function banUser(userId: number, status: number) {
    return request.put(`/v1/console/users/${userId}/ban`, { status })
}

export function exportUsers(params?: any) {
    return request.get('/v1/console/users/export', { params, responseType: 'blob' })
}

export function importUsers(file: File, updateExisting: boolean = false) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('updateExisting', String(updateExisting))
    return request.post('/v1/console/users/import', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

export function downloadUserTemplate() {
    return request.get('/v1/console/users/template', { responseType: 'blob' })
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

export function deleteRoles(roleIds: number[]) {
    return request.delete('/v1/system/role', { data: roleIds })
}

export function assignRoleMenus(roleId: number, menuIds: number[]) {
    return request.put(`/v1/system/role/${roleId}/menus`, menuIds)
}

export function assignRoleDepts(roleId: number, deptIds: number[], dataScope: number) {
    return request.put(`/v1/system/role/${roleId}/depts`, { deptIds, dataScope })
}
