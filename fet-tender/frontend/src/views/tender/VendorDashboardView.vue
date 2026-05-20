<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { QuestionFilled } from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { SVGRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart, TreemapChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'
import {
  suggestVendors,
  getVendorOverview,
  getVendorTrend,
  getVendorSolutionBreakdown,
  getVendorTopAgencies,
  getVendorProcurementProfile,
  getVendorCoVendors,
  searchTenderAwards,
} from '@/api/tender'
import type {
  VendorSuggestResponse,
  VendorOverviewResponse,
  VendorTrendResponse,
  VendorSolutionNode,
  VendorTopAgencyResponse,
  VendorProcurementProfileResponse,
  VendorCoVendorResponse,
  TenderAwardResponse,
} from '@/types/tender'

use([
  SVGRenderer,
  BarChart,
  LineChart,
  PieChart,
  TreemapChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
  DataZoomComponent,
])

// ── 廠商選擇 ─────────────────────────────────────────────────────────────────
const selectedVendor = ref<VendorSuggestResponse | null>(null)
const searchInput = ref('')

async function handleSuggest(q: string, cb: (suggestions: any[]) => void) {
  try {
    const res = await suggestVendors(q)
    if (res.errorCode === '00000') {
      cb(res.body.map((v) => ({ value: v.vendorName, ...v })))
    } else {
      cb([])
    }
  } catch {
    cb([])
  }
}

async function handleSelect(item: VendorSuggestResponse & { value: string }) {
  selectedVendor.value = item
  await loadAll()
}

// 使用者直接輸入後按 Enter：以輸入文字作為廠商名稱查詢（不依賴下拉選單）
async function handleEnterSearch() {
  const name = searchInput.value.trim()
  if (!name) return
  // 優先從建議清單比對精確名稱以取得 taxId
  try {
    const res = await suggestVendors(name)
    if (res.errorCode === '00000' && res.body.length > 0) {
      // 完全符合優先，否則取第一筆
      const exact = res.body.find(
        (v) => v.vendorName === name,
      ) ?? res.body[0]
      selectedVendor.value = exact
      searchInput.value = exact.vendorName
    } else {
      // 沒有建議時仍以輸入值直接查詢
      selectedVendor.value = { vendorName: name, vendorTaxId: null, winCount: 0 }
    }
  } catch {
    selectedVendor.value = { vendorName: name, vendorTaxId: null, winCount: 0 }
  }
  await loadAll()
}

function handleClear() {
  selectedVendor.value = null
  overview.value = null
  trend.value = null
  solutionBreakdown.value = []
  topAgencies.value = []
  procurementProfile.value = null
  coVendors.value = []
  tableData.value = []
  tableTotal.value = 0
}

// ── 資料狀態 ──────────────────────────────────────────────────────────────────
const loading = ref(false)
const overview = ref<VendorOverviewResponse | null>(null)
const trend = ref<VendorTrendResponse | null>(null)
const solutionBreakdown = ref<VendorSolutionNode[]>([])
const topAgencies = ref<VendorTopAgencyResponse[]>([])
const procurementProfile = ref<VendorProcurementProfileResponse | null>(null)
const coVendors = ref<VendorCoVendorResponse[]>([])
const tableData = ref<TenderAwardResponse[]>([])
const tableTotal = ref(0)
const tablePage = ref(0)

async function loadAll() {
  if (!selectedVendor.value) return
  const { vendorTaxId, vendorName } = selectedVendor.value
  loading.value = true
  try {
    const [ov, tr, sol, ag, pp, co, tbl] = await Promise.all([
      getVendorOverview(vendorTaxId, vendorName),
      getVendorTrend(vendorTaxId, vendorName),
      getVendorSolutionBreakdown(vendorTaxId, vendorName),
      getVendorTopAgencies(vendorTaxId, vendorName, 10),
      getVendorProcurementProfile(vendorTaxId, vendorName),
      getVendorCoVendors(vendorTaxId, vendorName),
      searchTenderAwards({ vendorName, page: 0, size: 20 }),
    ])
    if (ov.errorCode === '00000') overview.value = ov.body
    if (tr.errorCode === '00000') trend.value = tr.body
    if (sol.errorCode === '00000') solutionBreakdown.value = sol.body
    if (ag.errorCode === '00000') topAgencies.value = ag.body
    if (pp.errorCode === '00000') procurementProfile.value = pp.body
    if (co.errorCode === '00000') coVendors.value = co.body
    if (tbl.errorCode === '00000') {
      tableData.value = tbl.body.content
      tableTotal.value = tbl.body.totalElements
    }
  } finally {
    loading.value = false
  }
}

async function handleTablePageChange(page: number) {
  if (!selectedVendor.value) return
  tablePage.value = page - 1
  const res = await searchTenderAwards({
    vendorName: selectedVendor.value.vendorName,
    page: tablePage.value,
    size: 20,
  })
  if (res.errorCode === '00000') {
    tableData.value = res.body.content
    tableTotal.value = res.body.totalElements
  }
}

// ── 初始化：預設載入得標件數最多的廠商 ────────────────────────────────────────
onMounted(async () => {
  try {
    const res = await suggestVendors('')
    if (res.errorCode === '00000' && res.body.length > 0) {
      const top = res.body[0]
      selectedVendor.value = top
      searchInput.value = top.vendorName
      await loadAll()
    }
  } catch {
    // 無資料時靜默失敗
  }
})

// ── KPI 卡片資料 ──────────────────────────────────────────────────────────────
const kpiCards = computed(() => {
  if (!overview.value) return []
  const ov = overview.value
  return [
    {
      label: '總得標件數',
      value: ov.totalWins.toLocaleString(),
      unit: '件',
      color: '#409EFF',
    },
    {
      label: '總得標金額',
      value: formatAmount(ov.totalAmount),
      unit: '元',
      color: '#67C23A',
    },
    {
      label: '服務機關數',
      value: ov.agencyCount.toLocaleString(),
      unit: '個',
      color: '#E6A23C',
    },
    {
      label: '業務領域數',
      value: ov.solutionCount.toLocaleString(),
      unit: '類',
      color: '#909399',
    },
    {
      label: '最近得標',
      value: ov.latestAwardDate ?? '-',
      unit: '',
      color: '#F56C6C',
    },
  ]
})

function formatAmount(val: number | null): string {
  if (val == null) return '-'
  if (val >= 100_000_000) return (val / 100_000_000).toFixed(1) + ' 億'
  if (val >= 10_000) return (val / 10_000).toFixed(0) + ' 萬'
  return val.toLocaleString()
}

// ── 趨勢圖 ─────────────────────────────────────────────────────────────────────
const granularityLabel = computed(() => {
  const g = trend.value?.granularity
  if (g === 'DAY') return '每日'
  if (g === 'QUARTER') return '每季'
  return '每月'
})

const trendOption = computed(() => {
  const points = trend.value?.points ?? []
  const categories = points.map((p) => p.period)
  const counts = points.map((p) => p.count)
  const amounts = points.map((p) => +(p.totalAmount / 10000).toFixed(0))

  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'cross' } },
    legend: { data: ['得標件數', '得標金額（萬）'] },
    dataZoom: [{ type: 'inside' }, { type: 'slider', bottom: 0 }],
    grid: { left: 60, right: 70, top: 40, bottom: 60 },
    xAxis: { type: 'category', data: categories, axisLabel: { rotate: 30 } },
    yAxis: [
      { type: 'value', name: '件數', nameTextStyle: { align: 'right' } },
      { type: 'value', name: '萬元', nameTextStyle: { align: 'left' } },
    ],
    series: [
      {
        name: '得標件數',
        type: 'bar',
        data: counts,
        itemStyle: { color: '#409EFF' },
      },
      {
        name: '得標金額（萬）',
        type: 'line',
        yAxisIndex: 1,
        data: amounts,
        smooth: true,
        itemStyle: { color: '#67C23A' },
        lineStyle: { width: 2 },
        symbol: 'circle',
        symbolSize: 4,
      },
    ],
  }
})

// ── Solution Treemap ──────────────────────────────────────────────────────────
const treemapOption = computed(() => {
  const data = solutionBreakdown.value.map((s) => ({
    name: s.solution,
    value: +(s.totalAmount / 10000).toFixed(0),
    children: s.keywords.map((k) => ({
      name: k.keyword,
      value: +(k.totalAmount / 10000).toFixed(0),
    })),
  }))

  return {
    tooltip: {
      formatter: (info: any) => {
        const val = info.value
        return `${info.name}<br/>金額：${val.toLocaleString()} 萬`
      },
    },
    series: [
      {
        type: 'treemap',
        data,
        leafDepth: 2,
        label: { show: true, formatter: '{b}\n{c} 萬' },
        levels: [
          { itemStyle: { borderWidth: 3, borderColor: '#fff', gapWidth: 3 } },
          { itemStyle: { borderWidth: 1, borderColor: '#ccc', gapWidth: 1 } },
        ],
      },
    ],
  }
})

// ── 機關排行（水平長條） ───────────────────────────────────────────────────────
const agencyOption = computed(() => {
  const reversed = [...topAgencies.value].reverse()
  const names = reversed.map((a) => a.agencyName)
  const amounts = reversed.map((a) => +(a.totalAmount / 10000).toFixed(0))
  const counts = reversed.map((a) => a.count)

  return {
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) => {
        const name = params[0].name
        const amtItem = params.find((p: any) => p.seriesName === '金額（萬）')
        const cntItem = params.find((p: any) => p.seriesName === '件數')
        return `${name}<br/>金額：${amtItem?.value?.toLocaleString()} 萬<br/>件數：${cntItem?.value} 件`
      },
    },
    legend: { data: ['金額（萬）', '件數'], top: 0 },
    grid: { left: 140, right: 60, top: 30, bottom: 20 },
    xAxis: [
      { type: 'value', name: '萬元' },
      { type: 'value', name: '件數' },
    ],
    yAxis: { type: 'category', data: names, axisLabel: { width: 120, overflow: 'truncate' } },
    series: [
      {
        name: '金額（萬）',
        type: 'bar',
        data: amounts,
        itemStyle: { color: '#409EFF' },
      },
      {
        name: '件數',
        type: 'bar',
        xAxisIndex: 1,
        data: counts,
        itemStyle: { color: '#E6A23C' },
      },
    ],
  }
})

// ── 採購屬性三圓餅 ─────────────────────────────────────────────────────────────
function makePieOption(title: string, data: { name: string; count: number }[]) {
  return {
    title: { text: title, left: 'center', top: 0, textStyle: { fontSize: 13 } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} 件 ({d}%)' },
    legend: { orient: 'vertical', right: 10, top: 'middle', type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['35%', '65%'],
        center: ['40%', '55%'],
        data: data.map((d) => ({ name: d.name, value: d.count })),
        label: { show: false },
        emphasis: { label: { show: true, fontWeight: 'bold' } },
      },
    ],
  }
}

const tenderMethodOption = computed(() =>
  makePieOption('招標方式', procurementProfile.value?.tenderMethods ?? []),
)
const procurementTypeOption = computed(() =>
  makePieOption('採購類型', procurementProfile.value?.procurementTypes ?? []),
)
const awardMethodOption = computed(() =>
  makePieOption('決標方式', procurementProfile.value?.awardMethods ?? []),
)

// ── 共同得標廠商橫條圖 ────────────────────────────────────────────────────────
const coVendorOption = computed(() => {
  const reversed = [...coVendors.value].reverse()
  const names = reversed.map((v) => v.vendorName)
  const counts = reversed.map((v) => v.coCount)

  return {
    tooltip: { trigger: 'axis', formatter: '{b}: {c} 次' },
    grid: { left: 150, right: 40, top: 10, bottom: 20 },
    xAxis: { type: 'value', name: '共同得標次數' },
    yAxis: { type: 'category', data: names, axisLabel: { width: 130, overflow: 'truncate' } },
    series: [
      {
        type: 'bar',
        data: counts,
        itemStyle: { color: '#909399' },
        label: { show: true, position: 'right' },
      },
    ],
  }
})

// ── 明細表格 ──────────────────────────────────────────────────────────────────
function openDetail(url: string | null) {
  if (url) window.open(url, '_blank', 'noopener,noreferrer')
}

function formatDate(val: string | null) {
  return val ? val.slice(0, 10) : '-'
}

function formatMoney(val: number | null) {
  if (val == null) return '-'
  return val.toLocaleString()
}
</script>

<template>
  <div class="vendor-dashboard">
    <!-- ── 頁首搜尋 ────────────────────────────────────────────── -->
    <div class="dashboard-header">
      <h2 class="dashboard-title">廠商得標分析</h2>
      <div class="search-wrapper">
        <el-autocomplete
          v-model="searchInput"
          :fetch-suggestions="handleSuggest"
          placeholder="輸入廠商名稱後按 Enter，或從下拉選單選擇"
          clearable
          style="width: 360px"
          @select="handleSelect"
          @keyup.enter="handleEnterSearch"
          @clear="handleClear"
        >
          <template #default="{ item }">
            <div class="suggest-item">
              <span class="suggest-name">{{ item.vendorName }}</span>
              <el-tag size="small" type="info" class="suggest-count">
                {{ item.winCount }} 件
              </el-tag>
              <span v-if="item.vendorTaxId" class="suggest-taxid">{{ item.vendorTaxId }}</span>
            </div>
          </template>
        </el-autocomplete>
      </div>
    </div>

    <!-- ── 廠商未選定提示 ──────────────────────────────────────── -->
    <el-empty v-if="!selectedVendor" description="請輸入廠商名稱搜尋" />

    <!-- ── Dashboard 主體 ─────────────────────────────────────── -->
    <div v-else v-loading="loading" class="dashboard-body">

      <!-- 廠商標題列 -->
      <div class="vendor-title-row">
        <span class="vendor-display-name">{{ selectedVendor.vendorName }}</span>
        <el-tag v-if="selectedVendor.vendorTaxId" type="info" size="small">
          統編 {{ selectedVendor.vendorTaxId }}
        </el-tag>
        <span class="date-range" v-if="overview?.firstAwardDate">
          {{ overview.firstAwardDate }} ～ {{ overview.latestAwardDate }}
        </span>
      </div>

      <!-- KPI 卡片 -->
      <el-row :gutter="12" class="kpi-row">
        <el-col :xs="12" :sm="8" :md="4" v-for="kpi in kpiCards" :key="kpi.label">
          <el-card class="kpi-card" shadow="never">
            <div class="kpi-label">{{ kpi.label }}</div>
            <div class="kpi-value" :style="{ color: kpi.color }">
              {{ kpi.value }}
              <span class="kpi-unit">{{ kpi.unit }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 時間趨勢 -->
      <el-card class="chart-card" shadow="never">
        <template #header>
          <span>得標趨勢（{{ granularityLabel }}）</span>
        </template>
        <VChart class="chart-lg" :option="trendOption" autoresize />
      </el-card>

      <!-- Solution Treemap + 機關排行 -->
      <el-row :gutter="12" class="row-gap two-col-row">
        <el-col :xs="24" :md="12">
          <el-card class="chart-card" shadow="never">
            <template #header><span>業務版圖（Solution × Keyword）</span></template>
            <VChart class="chart-md" :option="treemapOption" autoresize />
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card class="chart-card" shadow="never">
            <template #header><span>機關排行 TOP 10（依金額）</span></template>
            <VChart class="chart-md" :option="agencyOption" autoresize />
          </el-card>
        </el-col>
      </el-row>

      <!-- 採購屬性三圓餅 -->
      <el-row :gutter="12" class="row-gap three-col-row">
        <el-col :xs="24" :sm="8">
          <el-card class="chart-card" shadow="never">
            <VChart class="chart-sm" :option="tenderMethodOption" autoresize />
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-card class="chart-card" shadow="never">
            <VChart class="chart-sm" :option="procurementTypeOption" autoresize />
          </el-card>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-card class="chart-card" shadow="never">
            <VChart class="chart-sm" :option="awardMethodOption" autoresize />
          </el-card>
        </el-col>
      </el-row>

      <!-- 共同得標廠商 -->
      <el-card v-if="coVendors.length > 0" class="chart-card row-gap" shadow="never">
        <template #header>
          <span>共同得標廠商（出現在同一標案的其他廠商）</span>
          <el-tooltip content="同一 tender_number + 公告序號下出現的其他廠商，次數越高代表越常共同承攬。" placement="top">
            <el-icon style="margin-left: 4px; cursor: help"><QuestionFilled /></el-icon>
          </el-tooltip>
        </template>
        <VChart class="chart-co" :option="coVendorOption" autoresize />
      </el-card>

      <!-- 得標明細 -->
      <el-card class="chart-card row-gap" shadow="never">
        <template #header><span>得標明細（共 {{ tableTotal }} 筆）</span></template>
        <el-table :data="tableData" size="small" border stripe style="width: 100%">
          <el-table-column prop="awardAnnounceDate" label="決標日" width="100" :formatter="(r: TenderAwardResponse) => formatDate(r.awardAnnounceDate)" />
          <el-table-column prop="solution" label="Solution" width="110" show-overflow-tooltip />
          <el-table-column prop="matchedKeyword" label="關鍵字" width="110" show-overflow-tooltip />
          <el-table-column prop="agencyName" label="機關" min-width="160" show-overflow-tooltip />
          <el-table-column prop="tenderName" label="標案名稱" min-width="200" show-overflow-tooltip />
          <el-table-column label="得標金額" width="130" align="right">
            <template #default="{ row }">
              {{ formatMoney(row.vendorAwardAmount) }}
            </template>
          </el-table-column>
          <el-table-column prop="tenderMethod" label="招標方式" width="100" show-overflow-tooltip />
          <el-table-column label="詳情" width="70" align="center">
            <template #default="{ row }">
              <el-button
                v-if="row.detailUrl"
                link
                type="primary"
                size="small"
                @click="openDetail(row.detailUrl)"
              >開啟</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-row">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="tableTotal"
            :page-size="20"
            @current-change="handleTablePageChange"
          />
        </div>
      </el-card>

    </div>
  </div>
</template>

<style scoped>
.vendor-dashboard {
  padding: 16px;
}

.dashboard-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.dashboard-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  white-space: nowrap;
}

.vendor-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.vendor-display-name {
  font-size: 18px;
  font-weight: 600;
}

.date-range {
  color: #909399;
  font-size: 13px;
}

/* KPI */
.kpi-row {
  margin-bottom: 12px;
}

.kpi-card {
  text-align: center;
  padding: 8px 0;
}

.kpi-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.kpi-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.kpi-unit {
  font-size: 12px;
  font-weight: 400;
  margin-left: 2px;
  color: #606266;
}

/* Charts */
.chart-card {
  width: 100%;
}

.chart-lg {
  height: 320px;
}

.chart-md {
  height: 300px;
}

.chart-sm {
  height: 240px;
}

.chart-co {
  height: 220px;
}

.row-gap {
  margin-top: 12px;
}

/* Suggest dropdown */
.suggest-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.suggest-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.suggest-count {
  flex-shrink: 0;
}

.suggest-taxid {
  font-size: 11px;
  color: #c0c4cc;
  flex-shrink: 0;
}

.pagination-row {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

/* ── 列印樣式 ───────────────────────────────────────────────── */
@media print {
  @page {
    size: A4 landscape;
    margin: 10mm 12mm;
  }

  /* 隱藏搜尋列、分頁、開啟按鈕等互動元件 */
  .search-wrapper,
  .pagination-row,
  .el-pagination {
    display: none !important;
  }

  .vendor-dashboard {
    padding: 6px;
  }

  /* KPI：5 張卡片同一行，各佔 20% */
  .kpi-row :deep(.el-col) {
    flex: 0 0 20% !important;
    max-width: 20% !important;
  }

  /* 兩欄區塊（業務版圖 + 機關排行）：各 50% */
  .two-col-row :deep(.el-col) {
    flex: 0 0 50% !important;
    max-width: 50% !important;
  }

  /* 三欄區塊（三圓餅）：各 33.333% */
  .three-col-row :deep(.el-col) {
    flex: 0 0 33.333% !important;
    max-width: 33.333% !important;
  }

  /* 避免卡片在頁面中間斷行 */
  .chart-card,
  .kpi-card {
    break-inside: avoid;
  }

  /* 趨勢圖列印後略縮，讓整體能塞進一頁 */
  .chart-lg {
    height: 240px !important;
  }

  .chart-md {
    height: 220px !important;
  }

  .chart-sm {
    height: 180px !important;
  }

  .chart-co {
    height: 160px !important;
  }

  /* 確保 el-row 保持 flex 橫排 */
  .kpi-row :deep(.el-row),
  .two-col-row :deep(.el-row),
  .three-col-row :deep(.el-row) {
    display: flex !important;
    flex-wrap: wrap !important;
  }
}
</style>
