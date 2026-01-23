import request from '@/utils/request'
import type { PostVO } from './post'

export interface UserDetailVO {
    id: number
    username: string
    nickname: string
    avatar?: string
    verifyStatus?: number
    verifyMethod?: string
    sex?: number
    createdAt?: string
}

export function getUserDetail(id: number) {
    return request.get<any, UserDetailVO>(`/api/v1/users/${id}`)
}

export function getUserPosts(userId: number, page = 1, size = 20) {
    return request.get<any, { records: PostVO[]; total: number }>(`/api/v1/users/${userId}/posts`, {
        params: { page, size }
    })
}

export function getMyBookmarks(page = 1, size = 20) {
    return request.get<any, { records: PostVO[]; total: number }>(`/api/v1/users/bookmarks`, {
        params: { page, size }
    })
}
