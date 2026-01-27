import request from '@/utils/request'
import type { FileVO } from './post'

export function uploadFile(file: File, type: string) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('type', type)
  return request.post<FileVO>('/api/v1/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function uploadPostImage(file: File) {
  return uploadFile(file, 'post')
}

export function uploadIdCardImage(file: File) {
  return uploadFile(file, 'id_card')
}
