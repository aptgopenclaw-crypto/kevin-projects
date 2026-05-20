import { defineStore } from 'pinia'
import type { TenantOption } from '@/types/auth'

export const useTenantStore = defineStore('tenant', {
  state: () => ({
    currentTenantId: null as string | null,
    tenantList: [] as TenantOption[],
    needsSelection: false,
  }),
  actions: {
    setTenant(tenantId: string) {
      this.currentTenantId = tenantId
    },
    setTenantList(list: TenantOption[]) {
      this.tenantList = list
      this.needsSelection = list.length > 1
    },
  },
})
