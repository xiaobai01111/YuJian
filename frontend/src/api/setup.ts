import request from '@/utils/request'

export interface SetupStatus {
  setupCompleted: boolean
  siteName?: string
  storageProvider?: string
  localPath?: string
  localPublicEnabled?: boolean
}

export interface SetupInitPayload {
  adminUsername: string
  adminPassword: string
  adminConfirmPassword: string
  adminNickname?: string
  adminEmail?: string
  siteName: string
  logoUrl?: string
  faviconUrl?: string
  theme?: string
  storageProvider?: string
  localPath?: string
  localPublicEnabled?: boolean
}

export const getSetupStatus = () => request.get<SetupStatus>('/api/v1/setup/status')

export const initSetup = (payload: SetupInitPayload) => request.post<void>('/api/v1/setup/init', payload)
