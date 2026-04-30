import { defineStore } from 'pinia'
import {
  getMaterialSpecs,
  getActiveWarehouses,
  getActiveSuppliers,
  getInventory,
  getInventorySummary,
  getInventoryAlerts,
  getPurchaseOrders,
  getApprovedMaterials,
} from '@/api/material'
import type {
  MaterialSpecResponse,
  WarehouseResponse,
  SupplierResponse,
  InventoryResponse,
  InventorySummaryResponse,
  PurchaseOrderResponse,
  ApprovedMaterialResponse,
  MaterialCategory,
  PurchaseOrderStatus,
} from '@/types/material'

export const useMaterialStore = defineStore('material', {
  state: () => ({
    // Shared dropdown options (cached)
    warehouses: [] as WarehouseResponse[],
    suppliers: [] as SupplierResponse[],

    // Material specs
    specs: [] as MaterialSpecResponse[],
    specsPagination: { page: 0, size: 15, totalElements: 0, totalPages: 0 },

    // Inventory
    inventoryItems: [] as InventoryResponse[],
    inventoryPagination: { page: 0, size: 15, totalElements: 0, totalPages: 0 },
    inventorySummary: [] as InventorySummaryResponse[],
    inventoryAlerts: [] as InventoryResponse[],

    // Purchase orders
    purchaseOrders: [] as PurchaseOrderResponse[],
    purchaseOrderPagination: { page: 0, size: 15, totalElements: 0, totalPages: 0 },

    // Approved materials
    approvedMaterials: [] as ApprovedMaterialResponse[],
    approvedMaterialPagination: { page: 0, size: 15, totalElements: 0, totalPages: 0 },

    loading: false,
  }),

  actions: {
    // ── Cached dropdown options ──

    async fetchWarehouses() {
      if (this.warehouses.length > 0) return
      const res = await getActiveWarehouses()
      this.warehouses = res.body
    },

    async fetchSuppliers() {
      if (this.suppliers.length > 0) return
      const res = await getActiveSuppliers()
      this.suppliers = res.body
    },

    // ── Material Specs ──

    async fetchSpecs(params: { category?: MaterialCategory; keyword?: string; page?: number; size?: number } = {}) {
      this.loading = true
      try {
        const res = await getMaterialSpecs({
          ...params,
          page: params.page ?? this.specsPagination.page,
          size: params.size ?? this.specsPagination.size,
        })
        this.specs = res.body.content
        this.specsPagination = {
          page: res.body.page,
          size: res.body.size,
          totalElements: res.body.totalElements,
          totalPages: res.body.totalPages,
        }
      } finally {
        this.loading = false
      }
    },

    // ── Inventory ──

    async fetchInventory(params: {
      warehouseId?: number
      category?: MaterialCategory
      keyword?: string
      belowSafetyStock?: boolean
      page?: number
      size?: number
    } = {}) {
      this.loading = true
      try {
        const res = await getInventory({
          ...params,
          page: params.page ?? this.inventoryPagination.page,
          size: params.size ?? this.inventoryPagination.size,
        })
        this.inventoryItems = res.body.content
        this.inventoryPagination = {
          page: res.body.page,
          size: res.body.size,
          totalElements: res.body.totalElements,
          totalPages: res.body.totalPages,
        }
      } finally {
        this.loading = false
      }
    },

    async fetchInventorySummary() {
      const res = await getInventorySummary()
      this.inventorySummary = res.body
    },

    async fetchInventoryAlerts() {
      const res = await getInventoryAlerts()
      this.inventoryAlerts = res.body
    },

    // ── Purchase Orders ──

    async fetchPurchaseOrders(params: { status?: PurchaseOrderStatus; keyword?: string; page?: number; size?: number } = {}) {
      this.loading = true
      try {
        const res = await getPurchaseOrders({
          ...params,
          page: params.page ?? this.purchaseOrderPagination.page,
          size: params.size ?? this.purchaseOrderPagination.size,
        })
        this.purchaseOrders = res.body.content
        this.purchaseOrderPagination = {
          page: res.body.page,
          size: res.body.size,
          totalElements: res.body.totalElements,
          totalPages: res.body.totalPages,
        }
      } finally {
        this.loading = false
      }
    },

    // ── Approved Materials ──

    async fetchApprovedMaterials(params: { keyword?: string; page?: number; size?: number } = {}) {
      this.loading = true
      try {
        const res = await getApprovedMaterials({
          ...params,
          page: params.page ?? this.approvedMaterialPagination.page,
          size: params.size ?? this.approvedMaterialPagination.size,
        })
        this.approvedMaterials = res.body.content
        this.approvedMaterialPagination = {
          page: res.body.page,
          size: res.body.size,
          totalElements: res.body.totalElements,
          totalPages: res.body.totalPages,
        }
      } finally {
        this.loading = false
      }
    },

    // ── Reset ──

    invalidateCache() {
      this.warehouses = []
      this.suppliers = []
    },
  },
})
