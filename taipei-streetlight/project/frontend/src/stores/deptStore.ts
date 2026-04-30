import { defineStore } from 'pinia'
import { getDeptOptions } from '@/api/dept'
import type { DeptOptionVO } from '@/types/dept'

function flattenToMap(nodes: DeptOptionVO[]): Map<number, string> {
  const map = new Map<number, string>()
  for (const node of nodes) {
    map.set(node.value, node.label)
    if (node.children?.length) {
      for (const [k, v] of flattenToMap(node.children)) {
        map.set(k, v)
      }
    }
  }
  return map
}

export const useDeptStore = defineStore('dept', {
  state: () => ({
    deptOptions: [] as DeptOptionVO[],
    deptFlatMap: new Map<number, string>(),
    initialized: false,
    loading: false,
  }),
  actions: {
    async fetchDeptOptions() {
      this.loading = true
      try {
        const res = await getDeptOptions()
        this.deptOptions = res.body
        this.deptFlatMap = flattenToMap(res.body)
        this.initialized = true
      } finally {
        this.loading = false
      }
    },

    getDeptName(deptId: number | string | null | undefined): string {
      if (deptId == null) return ''
      const numId = typeof deptId === 'string' ? Number(deptId) : deptId
      return this.deptFlatMap.get(numId) ?? '未知部門'
    },
  },
})
