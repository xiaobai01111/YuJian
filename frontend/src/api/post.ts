import request from '@/utils/request'
import type { UserVO } from './system'

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

export function getPostList(params: PostQueryDTO) {
    return request.get('/api/v1/posts', { params })
}

export function getConsolePostList(params: PostQueryDTO) {
    return request.get('/api/v1/console/posts', { params })
}

export function getPostDetail(id: number) {
    return request.get(`/api/v1/posts/${id}`)
}

export function createPost(data: PostCreateDTO) {
    return request.post('/api/v1/posts', data)
}

export function createConsolePost(data: PostCreateDTO) {
    return request.post('/api/v1/console/posts', data)
}

export function updatePost(id: number, data: any) {
    return request.put(`/api/v1/posts/${id}`, data)
}

export function deletePost(id: number) {
    return request.delete(`/api/v1/posts/${id}`)
}

export function likePost(id: number) {
    return request.post(`/api/v1/posts/${id}/like`)
}

export function unlikePost(id: number) {
    return request.delete(`/api/v1/posts/${id}/like`)
}

export function bookmarkPost(id: number) {
    return request.post(`/api/v1/posts/${id}/bookmark`)
}

export function unbookmarkPost(id: number) {
    return request.delete(`/api/v1/posts/${id}/bookmark`)
}

export function resolvePost(id: number) {
    return request.put(`/api/v1/posts/${id}/resolve`)
}

export function deleteConsolePost(id: number) {
    return request.delete(`/api/v1/console/posts/${id}`)
}

export function resolveConsolePost(id: number) {
    return request.put(`/api/v1/console/posts/${id}/resolve`)
}

export function searchPosts(params: { keyword: string; board?: string; page?: number; size?: number }) {
    return request.get('/api/v1/posts/search', { params })
}
