import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type { ContractResponse, ContractRequest, ContractStatus, PageResponse } from '@/types/contract'

export const getContracts = (params: {
  status?: ContractStatus
  keyword?: string
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<ContractResponse>>>('/auth/contracts', { params })

export const getContractById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<ContractResponse>>(`/auth/contracts/${id}`)

export const createContract = (payload: ContractRequest) =>
  axiosIns.post<unknown, BaseResponse<ContractResponse>>('/auth/contracts', payload)

export const updateContract = (id: number, payload: ContractRequest) =>
  axiosIns.put<unknown, BaseResponse<ContractResponse>>(`/auth/contracts/${id}`, payload)

export const deleteContract = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/contracts/${id}`)
