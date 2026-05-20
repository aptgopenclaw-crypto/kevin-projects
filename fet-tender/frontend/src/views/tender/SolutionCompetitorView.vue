<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { use } from 'echarts/core'
import { SVGRenderer } from 'echarts/renderers'
import { BarChart } from 'echarts/charts'
import {
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'
import {
  listSolutionOptions,
  getSolutionCompetitorSummary,
  getSolutionVendorRank,
  getSolutionKeywordSummary,
} from '@/api/tender'
import type {
  SolutionCompetitorSummaryResponse,
  SolutionVendorRankResponse,
  SolutionKeywordSummaryResponse,
} from '@/types/tender'

use([SVGRenderer, BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent])

// ── 篩選狀態 ──────────────────────────────────────────────────────────────────
const solutionOptions = ref<string[]>([])
const selectedSolution = ref<string>('')
const selectedKeyword = ref<string>('')
const dateRange = ref<[string, string] | null>(null)

const keywordOptions = computed<string[]>(() => {
  return keywordSummary.value.map((k) => k.keyword)
})

// ── 資料狀態 ──────────────────────────────────────────────────────────────────
const summary = ref<SolutionCompetitorSummaryResponse>({
  totalTenders: 0,
  totalAmount: 0,
  vendorCount: 0,
  keywordCount: 0,
})

const vendorRankList = ref<SolutionVendorRankResponse[]>([])
const vendorRankTotal = ref(0)
const vendorPage = ref(0)
const vendorPageSize = ref(10)

const keywordSummary = ref<SolutionKeywordSummaryResponse[]>([])

const loading = ref(false)

// ── 圖表配置 ──────────────────────────────────────────────────────────────────
const keywordChartOption = computed(() => {
  const top10 = keywordSummary.value.slice(0, 15)
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: any[]) => {
        const d = params[0]
        const kw = top10[d.dataIndex]
        return `
          <b>${kw.keyword}</b><br/>
          得標件數：${kw.winCount}<br/>
          不重複廠商：${kw.vendorCount}<br/>
          總金額：${formatAmount(kw.totalAmount)}
        `
      },
    },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: top10.map((k) => k.keyword),
      axisLabel: {
        rotate: 35,
        interval: 0,
        formatter: (v: string) => (v.length > 12 ? v.slice(0, 12) + '…' : v),
      },
    },
    yAxis: [
      { type: 'value', name: '件數', position: 'left' },
      { type: 'value', name: '廠商數', position: 'right' },
    ],
    series: [
      {
        name: '得標件數',
        type: 'bar',
        data: top10.map((k) => k.winCount),
        itemStyle: { color: '#409EFF' },
      },
      {
        name: '不重複廠商',
        type: 'bar',
        yAxisIndex: 1,
        data: top10.map((k) => k.vendorCount),
        itemStyle: { color: '#67C23A' },
      },
    ],
    legend: { data: ['得標件數', '不重複廠商'], top: 0 },
  }
})

// ── 工具函式 ──────────────────────────────────────────────────────────────────
function formatAmount(val: number | null | undefined): string {
  if (val == null) return '—'
  if (val >= 1_0000_0000) return (val / 1_0000_0000).toFixed(1) + ' 億'
  if (val >= 1_0000) return (val / 1_0000).toFixed(0) + ' 萬'
  return val.toLocaleString()
}

// ── 資料載入 ──────────────────────────────────────────────────────────────────
async function loadSolutionOptions() {
  const res = await listSolutionOptions()
  if (res.errorCode === '00000') {
    solutionOptions.value = res.body ?? []
    if (solutionOptions.value.length > 0 && !selectedSolution.value) {
      selectedSolution.value = solutionOptions.value[0]
    }
  }
}

async function loadAll() {
  if (!selectedSolution.value) return
  loading.value = true
  try {
    const [from, to] = dateRange.value ?? [undefined, undefined]
    const keyword = selectedKeyword.value || undefined
    await Promise.all([
      loadSummary(from, to, keyword),
      loadVendorRank(0, from, to, keyword),
      loadKeywordSummary(from, to),
    ])
  } finally {
    loading.value = false
  }
}

async function loadSummary(from?: string, to?: string, keyword?: string) {
  const res = await getSolutionCompetitorSummary(selectedSolution.value, keyword, from, to)
  if (res.errorCode === '00000') summary.value = res.body
}

async function loadVendorRank(page = 0, from?: string, to?: string, keyword?: string) {
  vendorPage.value = page
  const res = await getSolutionVendorRank({
    solution: selectedSolution.value,
    keyword,
    dateFrom: from,
    dateTo: to,
    page,
    size: vendorPageSize.value,
  })
  if (res.errorCode === '00000') {
    vendorRankList.value = res.body.content
    vendorRankTotal.value = res.body.totalElements
  }
}

async function loadKeywordSummary(from?: string, to?: string) {
  const res = await getSolutionKeywordSummary(selectedSolution.value, from, to)
  if (res.errorCode === '00000') keywordSummary.value = res.body
}

function handlePageChange(page: number) {
  const [from, to] = dateRange.value ?? [undefined, undefined]
  loadVendorRank(page - 1, from, to, selectedKeyword.value || undefined)
}

function handleSearch() {
  selectedKeyword.value = ''
  loadAll()
}

// ── 生命週期 ──────────────────────────────────────────────────────────────────
onMounted(async () => {
  await loadSolutionOptions()
  await loadAll()
})

watch(selectedSolution, () => {
  selectedKeyword.value = ''
  loadAll()
})
</script>

<template>
  <div class="solution-competitor" v-loading="loading">
    <!-- 頂部篩選 -->
    <el-card class="filter-card" shadow="never">
      <el-form :inline="true" label-width="80px">
        <el-form-item label="Solution">
          <el-select
            v-model="selectedSolution"
            placeholder="選擇 Solution"
            filterable
            style="width: 220px"
          >
            <el-option v-for="s in solutionOptions" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>

        <el-form-item label="關鍵字">
          <el-select
            v-model="selectedKeyword"
            placeholder="全部關鍵字"
            clearable
            filterable
            style="width: 220px"
          >
            <el-option v-for="k in keywordOptions" :key="k" :label="k" :value="k" />
          </el-select>
        </el-form-item>

        <el-form-item label="決標日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="~"
            start-placeholder="開始日期"
            end-placeholder="結束日期"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 260px"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSearch">查詢</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- KPI 摘要卡片 -->
    <el-row :gutter="16" class="kpi-row">
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">總得標件數</div>
          <div class="kpi-value primary">{{ summary.totalTenders.toLocaleString() }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">總得標金額</div>
          <div class="kpi-value success">{{ formatAmount(summary.totalAmount) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">參與廠商數</div>
          <div class="kpi-value warning">{{ summary.vendorCount.toLocaleString() }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="6">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">觸發關鍵字數</div>
          <div class="kpi-value info">{{ summary.keywordCount.toLocaleString() }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="content-row">
      <!-- 廠商排行表格 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never">
          <template #header>
            <span class="card-title">廠商得標排行榜</span>
            <span class="card-subtitle">（依得標件數降冪，{{ selectedKeyword || '全部關鍵字' }}）</span>
          </template>

          <el-table :data="vendorRankList" border stripe size="small" style="width: 100%">
            <el-table-column prop="rank" label="名次" width="60" align="center">
              <template #default="{ row }">
                <el-tag
                  v-if="row.rank <= 3"
                  :type="row.rank === 1 ? 'danger' : row.rank === 2 ? 'warning' : 'success'"
                  size="small"
                >
                  {{ row.rank }}
                </el-tag>
                <span v-else>{{ row.rank }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="vendorName" label="廠商名稱" min-width="160" show-overflow-tooltip />

            <el-table-column prop="vendorTaxId" label="統一編號" width="110" align="center">
              <template #default="{ row }">
                {{ row.vendorTaxId || '—' }}
              </template>
            </el-table-column>

            <el-table-column prop="winCount" label="得標件數" width="90" align="right" sortable />

            <el-table-column prop="totalAmount" label="總得標金額" width="130" align="right" sortable>
              <template #default="{ row }">
                {{ formatAmount(row.totalAmount) }}
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next, sizes"
              :total="vendorRankTotal"
              :current-page="vendorPage + 1"
              :page-size="vendorPageSize"
              :page-sizes="[10, 20, 50]"
              @current-change="handlePageChange"
              @size-change="(s: number) => { vendorPageSize = s; handleSearch() }"
            />
          </div>
        </el-card>
      </el-col>

      <!-- 關鍵字分布圖 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" style="height: 100%">
          <template #header>
            <span class="card-title">Keyword 分布</span>
            <span class="card-subtitle">（前 15 名，依得標件數）</span>
          </template>

          <v-chart
            v-if="keywordSummary.length > 0"
            :option="keywordChartOption"
            style="height: 400px"
            autoresize
          />

          <el-empty v-else description="暫無資料" />

          <!-- 關鍵字明細表 -->
          <el-table
            :data="keywordSummary"
            border
            size="small"
            style="width: 100%; margin-top: 12px"
            max-height="280"
          >
            <el-table-column prop="keyword" label="關鍵字" min-width="140" show-overflow-tooltip />
            <el-table-column prop="winCount" label="件數" width="70" align="right" />
            <el-table-column prop="vendorCount" label="廠商數" width="75" align="right" />
            <el-table-column prop="totalAmount" label="金額" width="110" align="right">
              <template #default="{ row }">{{ formatAmount(row.totalAmount) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.solution-competitor {
  padding: 16px;
}

.filter-card {
  margin-bottom: 16px;
}
.filter-card :deep(.el-card__body) {
  padding-bottom: 0;
}

.kpi-row {
  margin-bottom: 16px;
}
.kpi-card :deep(.el-card__body) {
  padding: 16px;
  text-align: center;
}
.kpi-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.kpi-value {
  font-size: 26px;
  font-weight: 700;
}
.kpi-value.primary  { color: #409eff; }
.kpi-value.success  { color: #67c23a; }
.kpi-value.warning  { color: #e6a23c; }
.kpi-value.info     { color: #909399; }

.content-row {
  align-items: flex-start;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
}
.card-subtitle {
  font-size: 12px;
  color: #909399;
  margin-left: 6px;
}

.pagination-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
