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
    title: string
    content: string
    isAnonymous: boolean
    category?: string
    price?: number
    location?: string
    lostTime?: string
    status: number
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
}

export interface PostCreateDTO {
    board: string
    title?: string
    content: string
    isAnonymous?: boolean
    category?: string
    price?: number
    location?: string
    lostTime?: string
    fileIds?: number[]
}

export function getPostList(params: PostQueryDTO) {
    return request.get('/posts', { params })
}

export function getPostDetail(id: number) {
    return request.get(`/posts/${id}`)
}

export function createPost(data: PostCreateDTO) {
    return request.post('/posts', data)
}

export function updatePost(id: number, data: any) {
    return request.put(`/posts/${id}`, data)
}

export function deletePost(id: number) {
    return request.delete(`/posts/${id}`)
}

export function likePost(id: number) {
    return request.post(`/posts/${id}/like`)
}

export function unlikePost(id: number) {
    return request.delete(`/posts/${id}/like`)
}

export function bookmarkPost(id: number) {
    return request.post(`/posts/${id}/bookmark`)
}

export function unbookmarkPost(id: number) {
    return request.delete(`/posts/${id}/bookmark`)
}

export function resolvePost(id: number) {
    return request.put(`/posts/${id}/resolve`)
}
