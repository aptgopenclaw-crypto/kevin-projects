import type { BaseResponse } from '@/types/auth'

// ===== Request Types =====

export interface UpdateOwnProfileRequest {
  displayName?: string
  phone?: string
  notifySmsFlag?: boolean
  notifyEmailFlag?: boolean
}

export interface UserChangePasswordRequest {
  newPassword: string
}

export interface UserListQuery {
  page?: number
  size?: number
  keyword?: string
}

export interface CreateUserRequest {
  email: string
  displayName: string
  phone?: string
  initialPassword: string
  tenantId?: string | null
  roleId: string
  deptId?: number | null
}

export interface UpdateUserRequest {
  displayName?: string
  phone?: string
  roleId?: string
  deptId?: number | null
}

export interface AddTenantRoleRequest {
  tenantId?: string | null
  roleId: string
  deptId?: number | null
}

// ===== Response Types =====

export interface UserListItemDto {
  userId: string
  email: string
  displayName: string
  phone: string | null
  enabled: boolean
  locked: boolean
  roleId: string
  roleCode: string
  roleName: string
  deptId: number | null
  deptName: string | null
  mappingId: number
  mappingEnabled: boolean
}

export interface UserTenantMappingDto {
  mappingId: number
  tenantId: string
  tenantName: string
  roleId: string
  roleCode: string
  roleName: string
  deptId: number | null
  deptName: string | null
  enabled: boolean
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

// Re-export for convenience
export type { BaseResponse }
