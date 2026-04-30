import { defineStore } from 'pinia'
import { getDevices, getDeviceById } from '@/api/device'
import type { DeviceResponse, DeviceType, DeviceStatus } from '@/types/device'

export const useDeviceStore = defineStore('device', {
  state: () => ({
    devices: [] as DeviceResponse[],
    pagination: {
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    },
    loading: false,
    currentDevice: null as DeviceResponse | null,
  }),
  actions: {
    async fetchDevices(params: {
      deviceType?: DeviceType
      status?: DeviceStatus
      keyword?: string
      page?: number
      size?: number
    }) {
      this.loading = true
      try {
        const res = await getDevices(params)
        this.devices = res.body.content
        this.pagination = {
          page: res.body.page,
          size: res.body.size,
          totalElements: res.body.totalElements,
          totalPages: res.body.totalPages,
        }
      } finally {
        this.loading = false
      }
    },
    async fetchDeviceDetail(id: number) {
      this.loading = true
      try {
        const res = await getDeviceById(id)
        this.currentDevice = res.body
        return res.body
      } finally {
        this.loading = false
      }
    },
  },
})
