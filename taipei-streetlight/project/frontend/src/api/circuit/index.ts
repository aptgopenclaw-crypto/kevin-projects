import axiosIns from '@/api/axios/axiosIns'
import type { BaseResponse } from '@/types/auth'
import type { CircuitResponse, CircuitRequest, PageResponse } from '@/types/circuit'

export const getCircuits = (params: {
  keyword?: string
  status?: string
  page?: number
  size?: number
}) =>
  axiosIns.get<unknown, BaseResponse<PageResponse<CircuitResponse>>>('/auth/circuits', { params })

export const getCircuitById = (id: number) =>
  axiosIns.get<unknown, BaseResponse<CircuitResponse>>(`/auth/circuits/${id}`)

export const createCircuit = (payload: CircuitRequest) =>
  axiosIns.post<unknown, BaseResponse<CircuitResponse>>('/auth/circuits', payload)

export const updateCircuit = (id: number, payload: CircuitRequest) =>
  axiosIns.put<unknown, BaseResponse<CircuitResponse>>(`/auth/circuits/${id}`, payload)

export const deleteCircuit = (id: number) =>
  axiosIns.delete<unknown, BaseResponse<void>>(`/auth/circuits/${id}`)
