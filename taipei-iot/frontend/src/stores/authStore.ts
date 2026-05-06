import { defineStore } from 'pinia'
import { getCaptcha, getUserInfo, login, logout, refreshTokenApi, selectTenant, switchTenant } from '@/api/auth'
import { setAxiosToken, setAxiosRefreshHandler, setAxiosLogoutHandler } from '@/api/axios/axiosIns'
import { useTenantStore } from '@/stores/tenantStore'
import { useMenuStore } from '@/stores/menuStore'
import { useDeptStore } from '@/stores/deptStore'
import router from '@/router'
import type { LoginRequest, UserInfoDto, TenantOption } from '@/types/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: null as string | null,
    userInfo: null as UserInfoDto | null,
    temporaryToken: null as string | null,
  }),
  getters: {
    isAuthenticated: (_state) => !!sessionStorage.getItem('passExam'),
  },
  actions: {
    _initAxiosCallbacks() {
      setAxiosRefreshHandler(async () => {
        const res = await refreshTokenApi()
        const newToken = res.body.accessToken
        this.accessToken = newToken
        setAxiosToken(newToken)
        return newToken
      })
      setAxiosLogoutHandler(() => {
        this.clearAuth()
        router.push('/login')
      })
      if (this.accessToken) {
        setAxiosToken(this.accessToken)
      }
    },
    async doGetCaptcha() {
      const res = await getCaptcha()
      return res.body
    },
    async doLogin(payload: LoginRequest) {
      const res = await login(payload)
      const data = res.body
      if (!data.needsSelection) {
        this.accessToken = data.accessToken
        setAxiosToken(data.accessToken)
        sessionStorage.setItem('passExam', 'true')
        await router.push('/')
      } else {
        this.temporaryToken = data.accessToken
        setAxiosToken(data.accessToken)
        const tenantStore = useTenantStore()
        tenantStore.setTenantList(data.tenants as TenantOption[])
        await router.push('/select-tenant')
      }
    },
    async doSelectTenant(tenantId: string) {
      const res = await selectTenant({ tenantId })
      const data = res.body
      this.accessToken = data.accessToken
      setAxiosToken(data.accessToken)
      this.temporaryToken = null
      sessionStorage.setItem('passExam', 'true')
      await router.push('/')
    },
    async doSwitchTenant(tenantId: string) {
      const res = await switchTenant({ tenantId })
      const data = res.body
      this.accessToken = data.accessToken
      setAxiosToken(data.accessToken)
      window.location.reload()
    },
    async getPermission() {
      const res = await getUserInfo()
      this.userInfo = res.body
      const tenantStore = useTenantStore()
      tenantStore.setTenantList(this.userInfo.availableTenants)
    },
    /** Restore session from refresh token cookie (e.g., after page reload) */
    async restoreSession() {
      if (this.accessToken) return
      const res = await refreshTokenApi()
      const newToken = res.body.accessToken
      this.accessToken = newToken
      setAxiosToken(newToken)
    },
    async doLogout() {
      try { await logout() } catch { /* ignore */ }
      this.clearAuth()
      await router.push('/login')
    },
    clearAuth() {
      this.accessToken = null
      this.userInfo = null
      this.temporaryToken = null
      setAxiosToken(null)
      sessionStorage.removeItem('passExam')

      // 清除其他 store，避免切換帳號時殘留前一位使用者的狀態
      useMenuStore().$reset()

      const deptStore = useDeptStore()
      deptStore.deptOptions = []
      deptStore.deptFlatMap = new Map()
      deptStore.initialized = false
    },
  },
})
