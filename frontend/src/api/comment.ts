import request from '@/utils/request'
import type { PageResult, UserVO } from './system'

export interface CommentConsoleVO {
  id: number
  postId: number
  content: string
  status: number
  createdAt: string
  author?: UserVO
}

export interface CommentQueryDTO {
  page: number
  size: number
  postId?: number
  status?: number
  keyword?: string
}

export interface CommentUpdateDTO {
  content: string
}

export interface CommentVO {
  id: number
  postId: number
  parentId?: number | null
  content: string
  anonymousId?: string
  isOwner?: boolean
  createdAt: string
  author?: UserVO
  children?: CommentVO[]
}

export interface CommentCreateDTO {
  postId: number
  parentId?: number | null
  content: string
}

export function getConsoleComments(params: CommentQueryDTO) {
  return request.get('/api/v1/console/comments', { params })
}

export function updateConsoleComment(id: number, data: CommentUpdateDTO, reason?: string) {
  return request.put(`/api/v1/console/comments/${id}`, data, {
    params: reason ? { reason } : {}
  })
}

export function deleteConsoleComment(id: number, reason?: string) {
  return request.delete(`/api/v1/console/comments/${id}`, {
    params: reason ? { reason } : {}
  })
}

export function batchDeleteConsoleComments(ids: number[], reason?: string) {
  return request.post('/api/v1/console/comments/batch-delete', {
    ids,
    reason
  })
}

export function getPostCommentsPage(postId: number, params: { page: number; size: number }) {
  return request.get<PageResult<CommentVO>>(`/api/v1/comments/post/${postId}/page`, { params })
}

export function createComment(data: CommentCreateDTO) {
  return request.post('/api/v1/comments', data)
}
