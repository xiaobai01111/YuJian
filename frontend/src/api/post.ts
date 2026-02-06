import request from '@/utils/request'
import type { PageResult, UserVO } from './system'

export interface FileVO {
    id: number
    originalName: string
    url: string
    type: string
    size: number
}

export interface PostVO {
    id: number
    board: string
    boards?: string[]
    title: string
    content: string
    isAnonymous: boolean
    category?: string
    price?: number
    originalPrice?: number
    location?: string
    lostTime?: string
    status: number
    showOnHome?: boolean
    likeCount: number
    commentCount: number
    viewCount: number
    createdAt: string
    updatedAt?: string
    author?: UserVO
    isLiked: boolean
    isBookmarked: boolean
    files: FileVO[]
}

export interface PostQueryDTO {
    page: number
    size: number
    board?: string
    category?: string
    status?: number
    userId?: number
    keyword?: string
    orderBy?: string
    lostFoundType?: string
    showOnHome?: boolean
}

export interface PostCreateDTO {
    boards: string[]
    board?: string
    title?: string
    content: string
    isAnonymous?: boolean
    category?: string
    price?: number
    location?: string
    lostTime?: string
    fileIds?: number[]
    showOnHome?: boolean
}

export interface BatchActionResultVO {
    requested: number
    success: number
    skipped: number
}

export function getPostList(params: PostQueryDTO) {
    return request.get<PageResult<PostVO>>('/api/v1/posts', { params })
}

export function getConsolePostList(params: PostQueryDTO) {
    return request.get<PageResult<PostVO>>('/api/v1/console/posts', { params })
}

export function getPostDetail(id: number) {
    return request.get<PostVO>(`/api/v1/posts/${id}`)
}

export function recordPostView(id: number) {
    return request.post<boolean>(`/api/v1/posts/${id}/view`)
}

export function createPost(data: PostCreateDTO) {
    return request.post<number>('/api/v1/posts', data)
}

export function createConsolePost(data: PostCreateDTO) {
    return request.post<number>('/api/v1/console/posts', data)
}

export function updatePost(id: number, data: unknown) {
    return request.put<void>(`/api/v1/posts/${id}`, data)
}

export function deletePost(id: number) {
    return request.delete<void>(`/api/v1/posts/${id}`)
}

export function likePost(id: number) {
    return request.post<void>(`/api/v1/posts/${id}/like`)
}

export function unlikePost(id: number) {
    return request.delete<void>(`/api/v1/posts/${id}/like`)
}

export function bookmarkPost(id: number) {
    return request.post<void>(`/api/v1/posts/${id}/bookmark`)
}

export function batchBookmarkPosts(postIds: number[]) {
    return request.post<BatchActionResultVO>('/api/v1/posts/bookmarks/batch', { postIds })
}

export function unbookmarkPost(id: number) {
    return request.delete<void>(`/api/v1/posts/${id}/bookmark`)
}

export function batchReportPosts(postIds: number[], reason: string) {
    return request.post<BatchActionResultVO>('/api/v1/posts/reports/batch', { postIds, reason })
}

export function resolvePost(id: number) {
    return request.put<void>(`/api/v1/posts/${id}/resolve`)
}

export function soldPost(id: number) {
    return request.put<void>(`/api/v1/posts/${id}/sold`)
}

export function deleteConsolePost(id: number, reason?: string) {
    return request.delete<void>(`/api/v1/console/posts/${id}`, { params: reason ? { reason } : {} })
}

export function resolveConsolePost(id: number, reason?: string) {
    return request.put<void>(`/api/v1/console/posts/${id}/resolve`, null, { params: reason ? { reason } : {} })
}

export function soldConsolePost(id: number, reason?: string) {
    return request.put<void>(`/api/v1/console/posts/${id}/sold`, null, { params: reason ? { reason } : {} })
}

// 帖子状态常量
export const POST_STATUS = {
    NORMAL: 0,
    RESOLVED: 1,
    DELETED: 2,
    PENDING_AUDIT: 3,
    ARCHIVED: 4,
    SOLD: 5
} as const

// 获取状态显示文本
export function getStatusLabel(status: number): string {
    switch (status) {
        case POST_STATUS.RESOLVED: return '已解决'
        case POST_STATUS.SOLD: return '已售出'
        case POST_STATUS.DELETED: return '已删除'
        case POST_STATUS.PENDING_AUDIT: return '待审核'
        case POST_STATUS.ARCHIVED: return '已下架'
        default: return ''
    }
}

// 获取状态样式类
export function getStatusClass(status: number): string {
    switch (status) {
        case POST_STATUS.RESOLVED: return 'badge-success'
        case POST_STATUS.SOLD: return 'badge-warning'
        case POST_STATUS.DELETED: return 'badge-error'
        case POST_STATUS.PENDING_AUDIT: return 'badge-info'
        case POST_STATUS.ARCHIVED: return 'badge-neutral'
        default: return ''
    }
}

export function searchPosts(params: { keyword: string; board?: string; page?: number; size?: number }) {
    return request.get<PageResult<PostVO>>('/api/v1/posts/search', { params })
}
