import { defineStore } from 'pinia'
import { getMenuTree, getMyMenus } from '@/api/rbac'
import type { MenuDto, UserMenuDto, BreadcrumbItem } from '@/types/rbac'
import router from '@/router'
import type { RouteRecordRaw } from 'vue-router'

// Dynamic component map — lazy-loaded views
const viewModules = import.meta.glob('@/views/**/*.vue')

function resolveComponent(component: string | null) {
  if (!component) return undefined
  // component stored as e.g. "views/admin/menu/MenuManageView.vue"
  const key = `/src/${component}`
  return viewModules[key]
}

function buildRoutesFromMenus(menus: UserMenuDto[]): RouteRecordRaw[] {
  const routes: RouteRecordRaw[] = []
  for (const menu of menus) {
    if (menu.menuType === 'BUTTON') continue
    if (menu.menuType === 'DIRECTORY') {
      // Recurse into children
      if (menu.children?.length) {
        routes.push(...buildRoutesFromMenus(menu.children))
      }
      continue
    }
    // PAGE type — create route
    if (menu.routeName && menu.routePath) {
      const comp = resolveComponent(menu.component)
      if (comp) {
        routes.push({
          path: menu.routePath,
          name: menu.routeName,
          component: comp,
          meta: {
            menuId: menu.menuId,
            keepAlive: false,
          },
        })
      }
    }
  }
  return routes
}

function flattenMenuNames(menus: UserMenuDto[]): Set<string> {
  const names = new Set<string>()
  for (const menu of menus) {
    if (menu.routeName) names.add(menu.routeName)
    if (menu.children?.length) {
      for (const name of flattenMenuNames(menu.children)) {
        names.add(name)
      }
    }
  }
  return names
}

/** Flatten nested menu tree into a flat array */
function flattenMenuTree(tree: UserMenuDto[]): UserMenuDto[] {
  const result: UserMenuDto[] = []
  for (const menu of tree) {
    result.push(menu)
    if (menu.children?.length) {
      result.push(...flattenMenuTree(menu.children))
    }
  }
  return result
}

function findBreadcrumbItems(
  menus: UserMenuDto[],
  routeName: string,
  path: BreadcrumbItem[] = [],
): BreadcrumbItem[] | null {
  for (const menu of menus) {
    const currentPath: BreadcrumbItem[] = [
      ...path,
      { label: menu.name, path: menu.routePath ?? null },
    ]
    if (menu.routeName === routeName) return currentPath
    if (menu.children?.length) {
      const found = findBreadcrumbItems(menu.children, routeName, currentPath)
      if (found) return found
    }
  }
  return null
}

// Track dynamically added route names so we can remove them on re-fetch
let dynamicRouteNames: string[] = []

export const useMenuStore = defineStore('menu', {
  state: () => ({
    menuTree: [] as MenuDto[],
    userMenus: [] as UserMenuDto[],
    loading: false,
    initialized: false,
  }),
  getters: {
    sidebarMenus: (state) => state.userMenus,
  },
  actions: {
    async fetchMyMenus() {
      this.loading = true
      try {
        const res = await getMyMenus()
        this.userMenus = res.body

        // Remove previously injected dynamic routes
        for (const name of dynamicRouteNames) {
          if (router.hasRoute(name)) {
            router.removeRoute(name)
          }
        }
        dynamicRouteNames = []

        // Dynamically inject routes from menus
        const routes = buildRoutesFromMenus(this.userMenus)
        for (const route of routes) {
          // Only add if route doesn't already exist (static routes take priority)
          if (route.name && !router.hasRoute(route.name as string)) {
            router.addRoute(route)
            dynamicRouteNames.push(route.name as string)
          }
        }
        this.initialized = true
      } catch (e) {
        // Degradation: mark initialized but keep userMenus empty
        this.initialized = true
        throw e
      } finally {
        this.loading = false
      }
    },

    async fetchMenuTree() {
      this.loading = true
      try {
        const res = await getMenuTree()
        this.menuTree = res.body
      } finally {
        this.loading = false
      }
    },

    hasRouteAccess(routeName: string | symbol | null | undefined): boolean {
      if (!routeName || typeof routeName === 'symbol') return false
      const allowedNames = flattenMenuNames(this.userMenus)
      // Allow implicit child routes (e.g. EditUser, CreateUser) when parent is accessible
      const implicitChildren: Record<string, string> = {
        EditUser: 'UserList',
        CreateUser: 'UserList',
        RepairTicketDetail: 'RepairTicket',
        InspectionRecord: 'InspectionTask',
        ReplacementOrderDetail: 'ReplacementOrder',
        ReplacementSelfCheck: 'ReplacementOrder',
      }
      const parent = implicitChildren[routeName]
      if (parent) return allowedNames.has(parent)
      return allowedNames.has(routeName)
    },

    getBreadcrumbs(routeName: string): BreadcrumbItem[] {
      return findBreadcrumbItems(this.userMenus, routeName) ?? []
    },

    flattenMenuTree(tree?: UserMenuDto[]): UserMenuDto[] {
      return flattenMenuTree(tree ?? this.userMenus)
    },

    $reset() {
      // Remove dynamic routes
      for (const name of dynamicRouteNames) {
        if (router.hasRoute(name)) {
          router.removeRoute(name)
        }
      }
      dynamicRouteNames = []
      this.menuTree = []
      this.userMenus = []
      this.loading = false
      this.initialized = false
    },
  },
})
