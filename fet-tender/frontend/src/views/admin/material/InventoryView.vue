<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Archive, Search, AlertTriangle } from 'lucide-vue-next'
import { getInventory, getInventorySummary, getInventoryAlerts, getActiveWarehouses } from '@/api/material'
import type {
  InventoryResponse,
  InventorySummaryResponse,
  WarehouseResponse,
  MaterialCategory,
} from '@/types/material'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const filterWarehouse = ref<number | ''>('')
const filterCategory = ref<MaterialCategory | ''>('')
const filterBelowSafety = ref(false)
const pageSize = ref(15)
const items = ref<InventoryResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

const summary = ref<InventorySummaryResponse[]>([])
const alerts = ref<InventoryResponse[]>([])
const warehouses = ref<WarehouseResponse[]>([])

const activeTab = ref('list')

const categoryOptions: { value: MaterialCategory; label: string }[] = [
  { value: 'LUMINAIRE', label: '燈具' },
  { value: 'CONTROLLER', label: '控制器' },
  { value: 'POLE', label: '燈桿' },
  { value: 'POLE_NUMBER', label: '桿號牌' },
  { value: 'CABLE', label: '電纜' },
  { value: 'OTHER', label: '其他' },
]

const categoryLabel = (c: MaterialCategory) =>
  categoryOptions.find(o => o.value === c)?.label ?? c

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getInventory({
      warehouseId: filterWarehouse.value || undefined,
      category: filterCategory.value || undefined,
      keyword: keyword.value || undefined,
      belowSafetyStock: filterBelowSafety.value || undefined,
      page,
      size: pageSize.value,
    })
    items.value = res.body.content
    pagination.value = {
      page: res.body.page,
      totalElements: res.body.totalElements,
      totalPages: res.body.totalPages,
    }
  } catch {
    ElMessage.error(t('material.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function fetchSummary() {
  try {
    const res = await getInventorySummary()
    summary.value = res.body
  } catch {
    ElMessage.error(t('material.loadFailed'))
  }
}

async function fetchAlerts() {
  try {
    const res = await getInventoryAlerts()
    alerts.value = res.body
  } catch {
    ElMessage.error(t('material.loadFailed'))
  }
}

async function fetchWarehouses() {
  try {
    const res = await getActiveWarehouses()
    warehouses.value = res.body
  } catch {
    // silent
  }
}

function handlePageChange(p: number) {
  fetchData(p - 1)
}

onMounted(async () => {
  await fetchWarehouses()
  fetchData()
  fetchSummary()
  fetchAlerts()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <Archive :size="22" />
        <h2>{{ t('material.inventoryTitle') }}</h2>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <!-- Inventory List -->
      <el-tab-pane :label="t('material.inventoryList')" name="list">
        <div class="filter-bar">
          <el-select v-model="filterWarehouse" :placeholder="t('material.warehouseFilter')" clearable @change="() => fetchData()">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
          <el-select v-model="filterCategory" :placeholder="t('material.categoryFilter')" clearable @change="() => fetchData()">
            <el-option v-for="opt in categoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-checkbox v-model="filterBelowSafety" :label="t('material.belowSafetyOnly')" @change="() => fetchData()" />
          <el-input v-model="keyword" :placeholder="t('common.search')" clearable style="width: 200px" @keyup.enter="() => fetchData()">
            <template #prefix><Search :size="16" /></template>
          </el-input>
          <el-button @click="fetchData()">{{ t('common.query') }}</el-button>
        </div>

        <el-table v-loading="loading" :data="items" stripe>
          <el-table-column prop="warehouseName" :label="t('material.warehouseName')" width="150" />
          <el-table-column prop="specCode" :label="t('material.specCode')" width="140" />
          <el-table-column prop="specName" :label="t('material.specName')" min-width="200" />
          <el-table-column :label="t('material.category')" width="100">
            <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
          </el-table-column>
          <el-table-column prop="quantityOnHand" :label="t('material.quantityOnHand')" width="100" align="right" />
          <el-table-column prop="safetyStock" :label="t('material.safetyStock')" width="100" align="right" />
          <el-table-column :label="t('common.status')" width="100">
            <template #default="{ row }">
              <span v-if="row.belowSafetyStock" class="status-tag status-danger">
                <AlertTriangle :size="12" style="vertical-align: middle" />
                {{ t('material.belowSafety') }}
              </span>
              <span v-else class="status-tag status-success">{{ t('material.normal') }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-bar">
          <el-pagination
            :current-page="pagination.page + 1"
            :page-size="pageSize"
            :total="pagination.totalElements"
            layout="total, prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </el-tab-pane>

      <!-- Summary -->
      <el-tab-pane :label="t('material.inventorySummary')" name="summary">
        <el-table :data="summary" stripe>
          <el-table-column :label="t('material.category')" width="150">
            <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
          </el-table-column>
          <el-table-column prop="itemCount" :label="t('material.itemCount')" width="120" align="right" />
          <el-table-column prop="totalQuantity" :label="t('material.totalQuantity')" width="120" align="right" />
        </el-table>
      </el-tab-pane>

      <!-- Alerts -->
      <el-tab-pane name="alerts">
        <template #label>
          <span>
            {{ t('material.safetyAlerts') }}
            <el-badge v-if="alerts.length > 0" :value="alerts.length" class="alert-badge" />
          </span>
        </template>
        <el-table :data="alerts" stripe>
          <el-table-column prop="warehouseName" :label="t('material.warehouseName')" width="150" />
          <el-table-column prop="specName" :label="t('material.specName')" min-width="200" />
          <el-table-column prop="quantityOnHand" :label="t('material.quantityOnHand')" width="100" align="right" />
          <el-table-column prop="safetyStock" :label="t('material.safetyStock')" width="100" align="right" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 8px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
.status-tag { padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.status-success { background: #f0f9ff; color: #059669; }
.status-danger { background: #fef2f2; color: #dc2626; }
.alert-badge { margin-left: 4px; }
</style>
