import request from '@/utils/request'
import type { PostQueryDTO, PostVO } from './post'
import type { CommentConsoleVO, CommentQueryDTO } from './comment'
import type { ReportVO } from './report'

export function getRecyclePosts(params: PostQueryDTO) {
  return request.get('/api/v1/console/recycle/posts', { params })
}

export function restoreRecyclePost(id: number, reason?: string) {
  return request.put(`/api/v1/console/recycle/posts/${id}/restore`, null, {
    params: reason ? { reason } : {}
  })
}

export function purgeRecyclePost(id: number, reason?: string) {
  return request.delete(`/api/v1/console/recycle/posts/${id}`, {
    params: reason ? { reason } : {}
  })
}

export function getRecycleComments(params: CommentQueryDTO) {
  return request.get('/api/v1/console/recycle/comments', { params })
}

export function restoreRecycleComment(id: number, reason?: string) {
  return request.put(`/api/v1/console/recycle/comments/${id}/restore`, null, {
    params: reason ? { reason } : {}
  })
}

export function purgeRecycleComment(id: number, reason?: string) {
  return request.delete(`/api/v1/console/recycle/comments/${id}`, {
    params: reason ? { reason } : {}
  })
}

export function getRecycleReports(params: { status?: number; page: number; size: number }) {
  return request.get('/api/v1/console/recycle/reports', { params })
}

export function restoreRecycleReport(id: number, reason?: string) {
  return request.put(`/api/v1/console/recycle/reports/${id}/restore`, null, {
    params: reason ? { reason } : {}
  })
}

export function purgeRecycleReport(id: number, reason?: string) {
  return request.delete(`/api/v1/console/recycle/reports/${id}`, {
    params: reason ? { reason } : {}
  })
}

export type { PostVO, CommentConsoleVO, ReportVO }
