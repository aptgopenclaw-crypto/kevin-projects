<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { searchTenderAwards, listSearchKeywords, exportTenderAwards } from '@/api/tender'
import type { TenderAwardResponse, TenderAwardQueryRequest } from '@/types/tender'

// ── 筌選條件 ──────────────────────────────────────────────────────
const { t } = useI18n()
const filter = reactive<TenderAwardQueryRequest>({
  solution: '',
  keyword: '',
  agency: '',
  name: '',
  vendorName: '',
  dateFrom: '',
  dateTo: '',
  page: 0,
  size: 20,
})

const dateRange = ref<[string, string] | null>(null)

function applyDateRange(val: [string, string] | null) {
  if (val) {
    filter.dateFrom = val[0]
    filter.dateTo = val[1]
  } else {
    filter.dateFrom = ''
    filter.dateTo = ''
  }
}

// ── 解決方案下拉選項 ──────────────────────────────────────────
const solutionOptions = ref<string[]>([])

async function loadSolutions() {
  try {
    const res = await listSearchKeywords(false)
    if (res.errorCode === '00000') {
      solutionOptions.value = [...new Set(res.body.map((k) => k.solution))]
    }
  } catch {
    // ignore
  }
}

// ── 列表 ──────────────────────────────────────────────────────
const loading = ref(false)
const tableData = ref<TenderAwardResponse[]>([])
const total = ref(0)

async function fetchList() {
  loading.value = true
  try {
    const params: TenderAwardQueryRequest = {
      solution: filter.solution || undefined,
      keyword: filter.keyword || undefined,
      agency: filter.agency || undefined,
      name: filter.name || undefined,
      vendorName: filter.vendorName || undefined,
      dateFrom: filter.dateFrom || undefined,
      dateTo: filter.dateTo || undefined,
      page: filter.page,
      size: filter.size,
    }
    const res = await searchTenderAwards(params)
    if (res.errorCode === '00000') {
      tableData.value = res.body.content
      total.value = res.body.totalElements
    }
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filter.page = 0
  fetchList()
}

function handleReset() {
  filter.solution = ''
  filter.keyword = ''
  filter.agency = ''
  filter.name = ''
  filter.vendorName = ''
  filter.dateFrom = ''
  filter.dateTo = ''
  dateRange.value = null
  filter.page = 0
  fetchList()
}

function handlePageChange(page: number) {
  filter.page = page - 1
  fetchList()
}

function handleSizeChange(size: number) {
  filter.size = size
  filter.page = 0
  fetchList()
}

// ── 詳情 Drawer ───────────────────────────────────────────────
const detailVisible = ref(false)
const detailRow = ref<TenderAwardResponse | null>(null)

function openDetail(row: TenderAwardResponse) {
  detailRow.value = row
  detailVisible.value = true
}

function formatAmount(val: number | null | undefined) {
  if (val == null) return '—'
  return val.toLocaleString()
}

onMounted(() => {
  loadSolutions()
  fetchList()
})

// ── 匯出 ──────────────────────────────────────────────────────
const exporting = ref(false)

async function handleExport() {
  exporting.value = true
  try {
    const params: TenderAwardQueryRequest = {
      solution: filter.solution || undefined,
      keyword: filter.keyword || undefined,
      agency: filter.agency || undefined,
      name: filter.name || undefined,
      vendorName: filter.vendorName || undefined,
      dateFrom: filter.dateFrom || undefined,
      dateTo: filter.dateTo || undefined,
    }
    const blob = await exportTenderAwards(params)
    const url = URL.createObjectURL(blob as unknown as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'tender-awards.xlsx'
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success(t('common.exportSuccess'))
  } catch {
    ElMessage.error(t('common.exportFailed'))
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <!-- 篩選條件 -->
    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="方案名稱">
          <el-select
            v-model="filter.solution"
            clearable
            placeholder="請選擇"
            style="width: 160px"
          >
            <el-option v-for="s in solutionOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="比對關鍵字">
          <el-input v-model="filter.keyword" clearable placeholder="比對關鍵字" style="width: 130px" />
        </el-form-item>
        <el-form-item label="機關名稱">
          <el-input v-model="filter.agency" clearable placeholder="機關名稱" style="width: 180px" />
        </el-form-item>
        <el-form-item label="標案名稱">
          <el-input v-model="filter.name" clearable placeholder="標案名稱" style="width: 200px" />
        </el-form-item>
        <el-form-item label="得標廠商">
          <el-input v-model="filter.vendorName" clearable placeholder="廠商名稱" style="width: 160px" />
        </el-form-item>
        <el-form-item label="決標公告日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="~"
            start-placeholder="開始日期"
            end-placeholder="結束日期"
            style="width: 260px"
            @change="applyDateRange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit">查詢</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" :loading="exporting" @click="handleExport">
            <el-icon style="margin-right:4px"><Download /></el-icon>
            匯出
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-table
      v-loading="loading"
      :data="tableData"
      border
      stripe
      style="width: 100%; margin-top: 16px"
      @row-click="openDetail"
    >
      <el-table-column prop="awardAnnounceDate" label="決標公告日期" width="120" />
      <el-table-column prop="solution" label="方案名稱" width="130" />
      <el-table-column prop="agencyName" label="機關名稱" width="200" show-overflow-tooltip />
      <el-table-column prop="tenderName" label="標案名稱" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link v-if="row.detailUrl" :href="row.detailUrl" target="_blank" type="primary" @click.stop>
            {{ row.tenderName }}
          </el-link>
          <span v-else>{{ row.tenderName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="procurementType" label="採購性質" width="90" align="center" />
      <el-table-column prop="tenderMethod" label="招標方式" width="110" show-overflow-tooltip />
      <el-table-column label="決標金額" width="130" align="right">
        <template #default="{ row }">
          {{ row.awardAmountRaw || formatAmount(row.awardAmount) || '—' }}
        </template>
      </el-table-column>
      <el-table-column prop="vendorName" label="得標廠商" min-width="180" show-overflow-tooltip />
      <el-table-column label="廠商決標金額" width="130" align="right">
        <template #default="{ row }">
          {{ row.vendorAwardAmountRaw || formatAmount(row.vendorAwardAmount) || '—' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click.stop="openDetail(row)">詳情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分頁 -->
    <el-pagination
      class="pagination"
      :current-page="filter.page! + 1"
      :page-size="filter.size"
      :page-sizes="[20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <!-- 詳情 Drawer -->
    <el-drawer
      v-model="detailVisible"
      title="決標公告詳情"
      direction="rtl"
      size="560px"
      destroy-on-close
    >
      <template v-if="detailRow">
        <!-- 群組 1：基本資訊 -->
        <div class="drawer-section-title">基本資訊</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="方案名稱">{{ detailRow.solution }}</el-descriptions-item>
          <el-descriptions-item label="比對關鍵字">{{ detailRow.matchedKeyword || '—' }}</el-descriptions-item>
          <el-descriptions-item label="標案案號" :span="2">{{ detailRow.tenderNumber || '—' }}</el-descriptions-item>
          <el-descriptions-item label="標案名稱" :span="2">
            <el-link v-if="detailRow.detailUrl" :href="detailRow.detailUrl" target="_blank" type="primary">
              {{ detailRow.tenderName }}
            </el-link>
            <span v-else>{{ detailRow.tenderName }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="採購性質">{{ detailRow.procurementType || '—' }}</el-descriptions-item>
          <el-descriptions-item label="標的分類">{{ detailRow.tenderCategory || '—' }}</el-descriptions-item>
          <el-descriptions-item label="採購金額級距" :span="2">{{ detailRow.procurementAmountRange || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 群組 2：決標資訊 -->
        <div class="drawer-section-title">決標資訊</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="決標公告日期">{{ detailRow.awardAnnounceDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="決標公告序號">{{ detailRow.awardAnnounceSeq || '—' }}</el-descriptions-item>
          <el-descriptions-item label="決標日期">{{ detailRow.awardDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="招標方式">{{ detailRow.tenderMethod || '—' }}</el-descriptions-item>
          <el-descriptions-item label="決標方式">{{ detailRow.awardMethod || '—' }}</el-descriptions-item>
          <el-descriptions-item label="是否訂有底價">
            {{ detailRow.hasBasePrice == null ? '—' : detailRow.hasBasePrice ? '是' : '否' }}
          </el-descriptions-item>
          <el-descriptions-item label="決標金額" :span="2">
            {{ detailRow.awardAmountRaw || formatAmount(detailRow.awardAmount) || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="履約期限" :span="2">{{ detailRow.performancePeriod || '—' }}</el-descriptions-item>
          <el-descriptions-item label="履約地點" :span="2">{{ detailRow.performanceLocation || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 群組 3：得標廠商資料 -->
        <div class="drawer-section-title">得標廠商（第 {{ detailRow.vendorOrderSeq }} 家）</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="廠商名稱" :span="2">{{ detailRow.vendorName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="統一編號">{{ detailRow.vendorTaxId || '—' }}</el-descriptions-item>
          <el-descriptions-item label="廠商電話">{{ detailRow.vendorPhone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="廠商地址" :span="2">{{ detailRow.vendorAddress || '—' }}</el-descriptions-item>
          <el-descriptions-item label="廠商決標金額" :span="2">
            {{ detailRow.vendorAwardAmountRaw || formatAmount(detailRow.vendorAwardAmount) || '—' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 群組 4：機關資訊 -->
        <div class="drawer-section-title">機關資訊</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="機關名稱" :span="2">{{ detailRow.agencyName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="機關代碼">{{ detailRow.agencyCode || '—' }}</el-descriptions-item>
          <el-descriptions-item label="單位名稱">{{ detailRow.unitName || '—' }}</el-descriptions-item>
          <el-descriptions-item label="機關地址" :span="2">{{ detailRow.agencyAddress || '—' }}</el-descriptions-item>
          <el-descriptions-item label="聯絡人">{{ detailRow.contactPerson || '—' }}</el-descriptions-item>
          <el-descriptions-item label="聯絡電話">{{ detailRow.contactPhone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="聯絡信箱" :span="2">{{ detailRow.contactEmail || '—' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.page-container {
  padding: 16px;
}
.filter-card {
  margin-bottom: 0;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.drawer-section-title {
  font-size: 13px;
  font-weight: 600;
  color: #2e6da4;
  background: #f0f7ff;
  padding: 6px 12px;
  margin: 16px 0 8px;
  border-left: 3px solid #2e6da4;
}
.drawer-section-title:first-child {
  margin-top: 0;
}
</style>
