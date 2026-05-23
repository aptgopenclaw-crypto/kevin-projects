import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type { TenantDto, CreateTenantRequest, UpdateTenantRequest } from '@/types/tenant'

export const listTenants = () =>
  axiosIns.get<unknown, BaseResponse<TenantDto[]>>('/admin/tenants')

export const createTenant = (payload: CreateTenantRequest) =>
  axiosIns.post<unknown, BaseResponse<TenantDto>>('/admin/tenants', payload)

export const updateTenant = (tenantId: string, payload: UpdateTenantRequest) =>
  axiosIns.put<unknown, BaseResponse<TenantDto>>(`/admin/tenants/${encodeURIComponent(tenantId)}`, payload)

export const toggleTenantEnabled = (tenantId: string, enabled: boolean) =>
  axiosIns.patch<unknown, BaseResponse<void>>(
    `/admin/tenants/${encodeURIComponent(tenantId)}/enabled`,
    null,
    { params: { enabled } },
  )
