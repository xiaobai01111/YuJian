import request from '@/utils/request'
import type { UserVO } from './system'

export interface ReportPostVO {
  id: number
  title?: string
}

export interface ReportVO {
  id: number
  reason: string
  status: number
  result?: string
  createdAt?: string
  handledAt?: string
  reporter?: UserVO
  post?: ReportPostVO
  handler?: UserVO
}

export interface ReportHandleDTO {
  result: string
  remark?: string
}

export interface ReportQueryDTO {
  status?: number
  page: number
  size: number
}

export function getConsoleReports(params: ReportQueryDTO) {
  return request.get('/api/v1/console/reports', { params })
}

export function getConsoleReportDetail(id: number) {
  return request.get(`/api/v1/console/reports/${id}`)
}

export function handleConsoleReport(id: number, data: ReportHandleDTO) {
  return request.put(`/api/v1/console/reports/${id}/handle`, data)
}

export function batchHandleConsoleReports(ids: number[], result: string, remark?: string) {
  return request.post('/api/v1/console/reports/batch-handle', {
    ids,
    result,
    remark
  })
}

export function deleteConsoleReport(id: number, reason?: string) {
  return request.delete(`/api/v1/console/reports/${id}`, {
    params: reason ? { reason } : {}
  })
}
