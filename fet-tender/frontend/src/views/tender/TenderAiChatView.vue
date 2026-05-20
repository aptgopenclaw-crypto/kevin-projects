<script setup lang="ts">
import { ref, nextTick, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { CopyDocument, Select } from '@element-plus/icons-vue'
import { tenderChat } from '@/api/tender'
import type { TenderChatMessage } from '@/types/tender'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([PieChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const { t } = useI18n()

// ── 對話狀態 ──────────────────────────────────────────────────
interface UiMessage {
  role: 'user' | 'assistant'
  content: string
  functionCalled?: string | null
  loading?: boolean
  data?: any
}

const messages = ref<UiMessage[]>([
  {
    role: 'assistant',
    content: t('tender.aiChat.welcome'),
  },
])

const inputText = ref('')
const sending = ref(false)
const chatBodyRef = ref<HTMLElement | null>(null)

async function scrollToBottom() {
  await nextTick()
  if (chatBodyRef.value) {
    chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
  }
}

// ── 送出訊息 ──────────────────────────────────────────────────
async function handleSend() {
  const text = inputText.value.trim()
  if (!text || sending.value) return

  // 加入使用者訊息
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  await scrollToBottom()

  // 加入 AI loading 佔位
  const loadingIdx = messages.value.length
  messages.value.push({ role: 'assistant', content: '', loading: true })
  sending.value = true
  await scrollToBottom()

  // 建立 history（排除 loading 占位和錯誤訊息）
  const errorText = t('tender.aiChat.errorResponse')
  const history: TenderChatMessage[] = messages.value
    .filter((m) => !m.loading && m.content !== errorText)
    .filter((m) => m.role === 'user' || m.role === 'assistant')
    .slice(0, -1) // 不含此次的 user 訊息
    .slice(-20)   // 最多帶 20 則
    .map((m) => ({ role: m.role, content: m.content }))

  try {
    const res = await tenderChat({ message: text, history })
    messages.value.splice(loadingIdx, 1, {
      role: 'assistant',
      content: res.errorCode === '00000' ? res.body.message : t('tender.aiChat.errorResponse'),
      functionCalled: res.errorCode === '00000' ? res.body.functionCalled : null,
      data: res.errorCode === '00000' ? res.body.data : undefined,
    })
  } catch {
    messages.value.splice(loadingIdx, 1, {
      role: 'assistant',
      content: t('tender.aiChat.errorResponse'),
    })
    ElMessage.error(t('common.operationFailed'))
  } finally {
    sending.value = false
    await scrollToBottom()
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}

function handleClear() {
  messages.value = [{ role: 'assistant', content: t('tender.aiChat.welcome') }]
}

// ── Function label 對應 ───────────────────────────────────────
const functionLabelMap: Record<string, string> = {
  search_tenders: '搜尋招標',
  get_tender_by_id: '查詢單筆',
  get_recent_tenders: '最近公告',
  get_tenders_by_budget: '預算篩選',
  get_tender_stats: '統計分析',
}

function getFunctionLabel(fn: string | null | undefined) {
  if (!fn) return null
  return functionLabelMap[fn] ?? fn
}

// ── 統計圖表選項 ──────────────────────────────────────────────
function buildStatsChartOptions(data: any) {
  if (!data?.stats || !Array.isArray(data.stats) || data.stats.length === 0) return null
  const stats: Array<{ solution: string; count: number; totalBudget: number }> = data.stats
  const pieData = stats.map((s) => ({ name: s.solution, value: s.count }))
  const barCategories = stats.map((s) => s.solution)
  const barValues = stats.map((s) =>
    s.totalBudget ? Math.round((s.totalBudget / 10000) * 10) / 10 : 0
  )

  return {
    pie: {
      tooltip: { trigger: 'item', formatter: '{b}: {c} 件 ({d}%)' },
      legend: { orient: 'vertical', right: 10, top: 'center' },
      series: [{
        type: 'pie',
        radius: ['35%', '60%'],
        center: ['40%', '50%'],
        data: pieData,
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } },
      }],
    },
    bar: {
      tooltip: { trigger: 'axis', formatter: (params: any) => `${params[0].name}<br/>總預算: ${params[0].value} 萬元` },
      grid: { left: 60, right: 20, top: 20, bottom: 60 },
      xAxis: { type: 'category', data: barCategories, axisLabel: { rotate: 20, fontSize: 11 } },
      yAxis: { type: 'value', name: '萬元' },
      series: [{ type: 'bar', data: barValues, itemStyle: { borderRadius: [4, 4, 0, 0] } }],
    },
  }
}

// ── 範例問句 ──────────────────────────────────────────────────
const suggestions = computed(() => [
  t('tender.aiChat.suggestion1'),
  t('tender.aiChat.suggestion2'),
  t('tender.aiChat.suggestion3'),
  t('tender.aiChat.suggestion4'),
])

function useSuggestion(text: string) {
  inputText.value = text
}

// ── 複製功能 ────────────────────────────────────────────────
const copiedIdx = ref<number | null>(null)

function buildCopyText(msg: UiMessage): string {
  let text = msg.content
  if (msg.functionCalled === 'get_tender_stats' && msg.data?.stats?.length) {
    const stats: Array<{ solution: string; count: number; totalBudget: number }> = msg.data.stats
    const dateFrom = msg.data.dateFrom ?? '不限'
    const dateTo = msg.data.dateTo ?? '不限'
    text += `\n\n各標案類型統計（${dateFrom} ~ ${dateTo}）：\n`
    text += stats
      .map((s) => {
        const budget = s.totalBudget
          ? `${(Math.round((s.totalBudget / 10000) * 10) / 10).toLocaleString()} 萬元`
          : '無資料'
        return `• ${s.solution}：${s.count} 件 / 總預算 ${budget}`
      })
      .join('\n')
  }
  return text
}

async function handleCopy(msg: UiMessage, idx: number) {
  try {
    await navigator.clipboard.writeText(buildCopyText(msg))
    copiedIdx.value = idx
    setTimeout(() => { copiedIdx.value = null }, 2000)
  } catch {
    ElMessage.error('複製失敗，請手動選取文字')
  }
}
</script>

<template>
  <div class="chat-page">
    <!-- 標題列 -->
    <div class="chat-header">
      <span class="chat-title">
        <el-icon class="header-icon"><BrainCircuit /></el-icon>
        {{ $t('tender.aiChat.title') }}
      </span>
      <el-button size="small" plain @click="handleClear">{{ $t('tender.aiChat.clear') }}</el-button>
    </div>

    <!-- 對話區 -->
    <div ref="chatBodyRef" class="chat-body">
      <div
        v-for="(msg, idx) in messages"
        :key="idx"
        :class="['msg-row', msg.role]"
      >
        <!-- 頭像 -->
        <div class="avatar">
          <el-avatar v-if="msg.role === 'assistant'" :size="32" class="avatar-ai">
            AI
          </el-avatar>
          <el-avatar v-else :size="32" class="avatar-user">
            <el-icon><User /></el-icon>
          </el-avatar>
        </div>

        <!-- 訊息泡泡 -->
        <div class="bubble-wrap">
          <div :class="['bubble', msg.role]">
            <template v-if="msg.loading">
              <span class="typing-dot" /><span class="typing-dot" /><span class="typing-dot" />
            </template>
            <template v-else>
              <!-- 保留換行，支援 markdown 風格輸出 -->
              <span style="white-space: pre-wrap">{{ msg.content }}</span>
            </template>
          </div>
          <!-- function called 標籤 -->
          <el-tag
            v-if="msg.role === 'assistant' && getFunctionLabel(msg.functionCalled)"
            size="small"
            type="info"
            class="fn-tag"
          >
            {{ getFunctionLabel(msg.functionCalled) }}
          </el-tag>

          <!-- 統計圖表（get_tender_stats） -->
          <template v-if="msg.functionCalled === 'get_tender_stats' && msg.data?.stats?.length">
            <div class="chart-wrap">
              <div class="chart-label">各標案類型件數（圓餅圖）</div>
              <v-chart
                class="chart-pie"
                :option="buildStatsChartOptions(msg.data)?.pie"
                autoresize
              />
              <div class="chart-label">各標案類型總預算（萬元）</div>
              <v-chart
                class="chart-bar"
                :option="buildStatsChartOptions(msg.data)?.bar"
                autoresize
              />
            </div>
          </template>

          <!-- 複製按鈕（hover 時顯示） -->
          <div v-if="!msg.loading" class="copy-row">
            <el-tooltip
              :content="copiedIdx === idx ? '已複製！' : '複製回覆'"
              placement="top"
              :hide-after="copiedIdx === idx ? 1500 : 200"
            >
              <el-button
                :icon="copiedIdx === idx ? Select : CopyDocument"
                size="small"
                text
                :class="['copy-btn', { copied: copiedIdx === idx }]"
                @click="handleCopy(msg, idx)"
              />
            </el-tooltip>
          </div>
        </div>
      </div>
    </div>

    <!-- 範例問句（初始或空白時顯示） -->
    <div v-if="messages.length <= 1" class="suggestions">
      <el-button
        v-for="s in suggestions"
        :key="s"
        size="small"
        round
        plain
        class="suggestion-btn"
        @click="useSuggestion(s)"
      >
        {{ s }}
      </el-button>
    </div>

    <!-- 輸入列 -->
    <div class="chat-input-bar">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        :placeholder="$t('tender.aiChat.placeholder')"
        resize="none"
        :disabled="sending"
        @keydown="handleKeydown"
        class="chat-textarea"
      />
      <el-button
        type="primary"
        :loading="sending"
        :disabled="!inputText.trim()"
        class="send-btn"
        @click="handleSend"
      >
        {{ $t('tender.aiChat.send') }}
      </el-button>
    </div>
    <div class="input-hint">{{ $t('tender.aiChat.hint') }}</div>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 120px);
  padding: 16px 20px 0;
  box-sizing: border-box;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.chat-title {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
}

.header-icon {
  color: var(--el-color-primary);
}

/* 對話區 */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.msg-row {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.msg-row.user {
  flex-direction: row-reverse;
}

.bubble-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 72%;
}

.msg-row.user .bubble-wrap {
  align-items: flex-end;
}

.bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.bubble.assistant {
  background: var(--el-fill-color-light);
  border-top-left-radius: 2px;
}

.bubble.user {
  background: var(--el-color-primary);
  color: #fff;
  border-top-right-radius: 2px;
}

.fn-tag {
  align-self: flex-start;
  opacity: 0.75;
}

.msg-row.user .fn-tag {
  align-self: flex-end;
}

.avatar-ai {
  background: var(--el-color-primary-light-3);
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 700;
}

.avatar-user {
  background: var(--el-fill-color);
}

/* 打字動畫 */
.typing-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--el-text-color-secondary);
  margin: 0 2px;
  animation: blink 1.2s infinite;
}

.typing-dot:nth-child(2) { animation-delay: 0.2s; }
.typing-dot:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 80%, 100% { opacity: 0.2; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

/* 範例問句 */
.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 8px 0;
}

.suggestion-btn {
  font-size: 12px;
}

/* 輸入列 */
.chat-input-bar {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  margin-top: 8px;
  padding-bottom: 0;
}

.chat-textarea {
  flex: 1;
}

.send-btn {
  height: 58px;
  min-width: 72px;
}

.input-hint {
  font-size: 11px;
  color: var(--el-text-color-placeholder);
  text-align: right;
  margin-top: 2px;
  margin-bottom: 4px;
}

/* 複製按鈕 */
.copy-row {
  display: flex;
  opacity: 0;
  transition: opacity 0.15s;
  margin-top: 2px;
}

.bubble-wrap:hover .copy-row {
  opacity: 1;
}

.copy-btn {
  padding: 2px 4px;
  color: var(--el-text-color-placeholder);
}

.copy-btn.copied {
  color: var(--el-color-success);
}

/* 統計圖表 */
.chart-wrap {
  margin-top: 8px;
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chart-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.chart-pie {
  height: 220px;
}

.chart-bar {
  height: 200px;
}
</style>
