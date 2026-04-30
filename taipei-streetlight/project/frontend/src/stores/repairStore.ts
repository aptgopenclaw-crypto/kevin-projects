import { defineStore } from 'pinia'
import { getRepairTickets, getRepairTicketById } from '@/api/repair'
import type {
  RepairTicketResponse,
  RepairTicketQueryParams,
} from '@/types/repair'

export const useRepairStore = defineStore('repair', {
  state: () => ({
    tickets: [] as RepairTicketResponse[],
    pagination: {
      page: 0,
      size: 15,
      totalElements: 0,
      totalPages: 0,
    },
    loading: false,
    currentTicket: null as RepairTicketResponse | null,
    // 統計快取
    stats: {
      pending: 0,
      inProgress: 0,
      pendingReview: 0,
    },
  }),

  actions: {
    async fetchTickets(params: RepairTicketQueryParams & { page?: number; size?: number } = {}) {
      this.loading = true
      try {
        const res = await getRepairTickets({
          ...params,
          page: params.page ?? this.pagination.page,
          size: params.size ?? this.pagination.size,
        })
        this.tickets = res.body.content
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

    async fetchTicketDetail(id: number) {
      this.loading = true
      try {
        const res = await getRepairTicketById(id)
        this.currentTicket = res.body
        return res.body
      } finally {
        this.loading = false
      }
    },

    async refreshStats() {
      try {
        const [pending, inProgress, pendingReview] = await Promise.all([
          getRepairTickets({ status: 'PENDING', page: 0, size: 1 }),
          getRepairTickets({ status: 'IN_PROGRESS', page: 0, size: 1 }),
          getRepairTickets({ status: 'PENDING_REVIEW', page: 0, size: 1 }),
        ])
        this.stats = {
          pending: pending.body.totalElements,
          inProgress: inProgress.body.totalElements,
          pendingReview: pendingReview.body.totalElements,
        }
      } catch {
        // ignore stats failures
      }
    },
  },
})
