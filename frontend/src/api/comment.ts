import request from '@/utils/request'
import type { UserVO } from './system'

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
