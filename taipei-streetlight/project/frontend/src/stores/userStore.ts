import { defineStore } from 'pinia'
import { listUsers, getUserTenantRoles } from '@/api/user'
import type { UserListItemDto, UserTenantMappingDto, UserListQuery } from '@/types/user'

export const useUserStore = defineStore('user', {
  state: () => ({
    userList: [] as UserListItemDto[],
    pagination: {
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    },
    tenantRoles: [] as UserTenantMappingDto[],
  }),
  actions: {
    async fetchUserList(params: UserListQuery) {
      const res = await listUsers(params)
      const data = res.body
      this.userList = data.content
      this.pagination = {
        page: data.page,
        size: data.size,
        totalElements: data.totalElements,
        totalPages: data.totalPages,
      }
    },
    async fetchTenantRoles(userId: string) {
      const res = await getUserTenantRoles(userId)
      this.tenantRoles = res.body
    },
  },
})
