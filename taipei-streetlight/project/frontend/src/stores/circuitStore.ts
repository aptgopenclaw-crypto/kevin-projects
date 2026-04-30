import { defineStore } from 'pinia'
import { getCircuits } from '@/api/circuit'
import type { CircuitResponse } from '@/types/circuit'

export const useCircuitStore = defineStore('circuit', {
  state: () => ({
    circuits: [] as CircuitResponse[],
    pagination: {
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    },
    loading: false,
  }),
  actions: {
    async fetchCircuits(params: {
      keyword?: string
      status?: string
      page?: number
      size?: number
    }) {
      this.loading = true
      try {
        const res = await getCircuits(params)
        this.circuits = res.body.content
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
  },
})
