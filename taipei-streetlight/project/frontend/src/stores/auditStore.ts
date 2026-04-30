import { defineStore } from 'pinia'
import { getUserUsageHistory, getMyLoginLog } from '@/api/audit'
import type { UserEventLogDto, PaginationState, AuditFilterModel } from '@/types/audit'

export const useAuditStore = defineStore('audit', {
  state: () => ({
    usageLogs: [] as UserEventLogDto[],
    myLogs: [] as UserEventLogDto[],
    pagination: {
      page: 0,
      size: 20,
      total: 0,
    } as PaginationState,
    myPagination: {
      page: 0,
      size: 20,
      total: 0,
    } as PaginationState,
    loading: false,
  }),
  actions: {
    async fetchUsageLogs(filters: AuditFilterModel & { sortBy?: string; sort?: string }) {
      this.loading = true
      try {
        const res = await getUserUsageHistory({
          userName: filters.userName,
          eventDesc: filters.eventDesc,
          startTimestamp: filters.startTimestamp,
          endTimestamp: filters.endTimestamp,
          sortBy: filters.sortBy,
          sort: filters.sort ?? 'DESC',
          page: this.pagination.page,
          pageSize: this.pagination.size,
        })
        this.usageLogs = res.body.content
        this.pagination.total = res.body.totalElements
      } finally {
        this.loading = false
      }
    },

    async fetchMyLogs(filters: { eventType?: string; startTimestamp?: string; endTimestamp?: string; sort?: string }) {
      this.loading = true
      try {
        const res = await getMyLoginLog({
          eventType: filters.eventType,
          startTimestamp: filters.startTimestamp,
          endTimestamp: filters.endTimestamp,
          sort: filters.sort ?? 'DESC',
          page: this.myPagination.page,
          pageSize: this.myPagination.size,
        })
        this.myLogs = res.body.content
        this.myPagination.total = res.body.totalElements
      } finally {
        this.loading = false
      }
    },
  },
})
