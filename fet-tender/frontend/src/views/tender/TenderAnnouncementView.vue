<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import { searchTenderAnnouncements, listSearchKeywords, exportTenderAnnouncements } from '@/api/tender'
import type { TenderAnnouncementResponse, TenderAnnouncementQueryRequest } from '@/types/tender'

const { t } = useI18n()

// ── 篩選條件 ──────────────────────────────────────────────────
const filter = reactive<TenderAnnouncementQueryRequest>({
  solution: '',
  keyword: '',
  agency: '',
  name: '',
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

// ── 解決方案下拉選項（從關鍵字 API 取得去重） ──────────────────
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
const tableData = ref<TenderAnnouncementResponse[]>([])
const total = ref(0)

async function fetchList() {
  loading.value = true
  try {
    const params: TenderAnnouncementQueryRequest = {
      solution: filter.solution || undefined,
      keyword: filter.keyword || undefined,
      agency: filter.agency || undefined,
      name: filter.name || undefined,
      dateFrom: filter.dateFrom || undefined,
      dateTo: filter.dateTo || undefined,
      page: filter.page,
      size: filter.size,
    }
    const res = await searchTenderAnnouncements(params)
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

// ── 詳情 Dialog ───────────────────────────────────────────────
const detailVisible = ref(false)
const detailRow = ref<TenderAnnouncementResponse | null>(null)

function openDetail(row: TenderAnnouncementResponse) {
  detailRow.value = row
  detailVisible.value = true
}

function formatDateTime(val: string | null | undefined) {
  if (!val) return '—'
  return val.slice(0, 16).replace('T', ' ')
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
    const params: TenderAnnouncementQueryRequest = {
      solution: filter.solution || undefined,
      keyword: filter.keyword || undefined,
      agency: filter.agency || undefined,
      name: filter.name || undefined,
      dateFrom: filter.dateFrom || undefined,
      dateTo: filter.dateTo || undefined,
    }
    const blob = await exportTenderAnnouncements(params)
    const url = URL.createObjectURL(blob as unknown as Blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'tender-announcements.xlsx'
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
        <el-form-item :label="$t('tender.keyword.solution')">
          <el-select
            v-model="filter.solution"
            clearable
            :placeholder="$t('common.search')"
            style="width: 160px"
          >
            <el-option
              v-for="s in solutionOptions"
              :key="s"
              :label="s"
              :value="s"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('tender.announcement.matchedKeyword')">
          <el-input
            v-model="filter.keyword"
            clearable
            :placeholder="$t('tender.announcement.matchedKeyword')"
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item :label="$t('tender.announcement.agencyName')">
          <el-input
            v-model="filter.agency"
            clearable
            :placeholder="$t('tender.announcement.agencyName')"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item :label="$t('tender.announcement.tenderName')">
          <el-input
            v-model="filter.name"
            clearable
            :placeholder="$t('tender.announcement.tenderName')"
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item :label="$t('tender.announcement.announcementDate')">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="~"
            :start-placeholder="$t('tender.announcement.dateFrom')"
            :end-placeholder="$t('tender.announcement.dateTo')"
            style="width: 260px"
            @change="applyDateRange"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" native-type="submit">{{ $t('common.query') }}</el-button>
          <el-button @click="handleReset">{{ $t('common.reset') }}</el-button>
          <el-button type="success" :loading="exporting" @click="handleExport">
            <el-icon style="margin-right:4px"><Download /></el-icon>
            {{ $t('common.export') }}
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
      <el-table-column prop="announcementDate" :label="$t('tender.announcement.announcementDate')" width="110" />
      <el-table-column prop="solution" :label="$t('tender.keyword.solution')" width="130" />
      <el-table-column prop="agencyName" :label="$t('tender.announcement.agencyName')" width="200" show-overflow-tooltip />
      <el-table-column prop="tenderName" :label="$t('tender.announcement.tenderName')" min-width="280" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link v-if="row.detailUrl" :href="row.detailUrl" target="_blank" type="primary" @click.stop>
            {{ row.tenderName }}
          </el-link>
          <span v-else>{{ row.tenderName }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="procurementType" :label="$t('tender.announcement.procurementType')" width="90" align="center" />
      <el-table-column prop="tenderMethod" :label="$t('tender.announcement.tenderMethod')" width="110" show-overflow-tooltip />
      <el-table-column prop="budgetAmountRaw" :label="$t('tender.announcement.budgetAmount')" width="130" align="right" />
      <el-table-column prop="deadline" :label="$t('tender.announcement.deadline')" width="150">
        <template #default="{ row }">{{ formatDateTime(row.deadline) }}</template>
      </el-table-column>

      <!-- 詳情操作欄 -->
      <el-table-column :label="$t('common.actions')" width="80" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click.stop="openDetail(row)">
            {{ $t('tender.announcement.detail') }}
          </el-button>
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
      :title="$t('tender.announcement.detail')"
      direction="rtl"
      size="520px"
      destroy-on-close
    >
      <template v-if="detailRow">
        <!-- 群組 1：基本資訊 -->
        <div class="drawer-section-title">基本資訊</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="$t('tender.keyword.solution')">{{ detailRow.solution }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.matchedKeyword')">{{ detailRow.matchedKeyword || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.tenderNumber')" :span="2">{{ detailRow.tenderNumber || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.tenderName')" :span="2">
            <el-link v-if="detailRow.detailUrl" :href="detailRow.detailUrl" target="_blank" type="primary">
              {{ detailRow.tenderName }}
            </el-link>
            <span v-else>{{ detailRow.tenderName }}</span>
          </el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.announcementDate')">{{ detailRow.announcementDate || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.tenderStatus')">{{ detailRow.tenderStatus || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.budgetAmount')" :span="2">
            {{ detailRow.budgetAmountRaw || formatAmount(detailRow.budgetAmount) }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.transmissionCount')">{{ detailRow.transmissionCount ?? '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.tenderCategory')">{{ detailRow.tenderCategory || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.procurementAmountRange')" :span="2">{{ detailRow.procurementAmountRange || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 群組 2：招標資訊 -->
        <div class="drawer-section-title">招標資訊</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="$t('tender.announcement.procurementType')">{{ detailRow.procurementType || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.tenderMethod')">{{ detailRow.tenderMethod || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.handlingMethod')">{{ detailRow.handlingMethod || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.awardMethod')">{{ detailRow.awardMethod || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.hasBasePrice')">
            {{ detailRow.hasBasePrice == null ? '—' : detailRow.hasBasePrice ? '是' : '否' }}
          </el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.deadline')">{{ formatDateTime(detailRow.deadline) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.performanceLocation')" :span="2">{{ detailRow.performanceLocation || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 群組 3：機關資訊 -->
        <div class="drawer-section-title">機關資訊</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="$t('tender.announcement.agencyName')" :span="2">{{ detailRow.agencyName || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.agencyCode')">{{ detailRow.agencyCode || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.unitName')">{{ detailRow.unitName || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.agencyAddress')" :span="2">{{ detailRow.agencyAddress || '—' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 群組 4：聯絡與開標 -->
        <div class="drawer-section-title">聯絡與開標</div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item :label="$t('tender.announcement.contactPerson')">{{ detailRow.contactPerson || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.contactPhone')">{{ detailRow.contactPhone || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.contactEmail')" :span="2">{{ detailRow.contactEmail || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.openingTime')">{{ formatDateTime(detailRow.openingTime) }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.openingLocation')">{{ detailRow.openingLocation || '—' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('tender.announcement.scrapedAt')" :span="2">{{ formatDateTime(detailRow.scrapedAt) }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.page-container { padding: 20px; }
.filter-card { margin-bottom: 0; }
.pagination { margin-top: 16px; justify-content: flex-end; }

.drawer-section-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-light);
  padding: 6px 12px;
  margin: 16px 0 8px;
  border-left: 3px solid var(--el-color-primary);
  border-radius: 2px;
}

.drawer-section-title:first-child {
  margin-top: 0;
}
</style>
