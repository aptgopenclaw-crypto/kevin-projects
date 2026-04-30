<script setup lang="ts">
import { ref, computed, onMounted, provide } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { LayoutDashboard, Settings, RotateCcw, Save, Plus, Sun, Moon, Palette, Upload } from 'lucide-vue-next'
import { GridLayout, GridItem } from 'grid-layout-plus'
import WidgetContainer from '@/views/admin/dashboard/WidgetContainer.vue'
import { getWidgetMeta, buildDefaultLayout, getAllWidgetMetas } from '@/views/admin/dashboard/widgetRegistry'
import { getLayout, saveLayout, resetLayout, saveDefaultLayout } from '@/api/dashboard'
import type { WidgetConfig, WidgetType, DashboardTheme } from '@/types/dashboard'
import { ECHARTS_THEME_KEY } from '@/views/admin/dashboard/composables/useDashboardTheme'
import { useDashboardWebSocket } from '@/views/admin/dashboard/composables/useDashboardWebSocket'
import { DASHBOARD_WS_KEY } from '@/views/admin/dashboard/composables/useDashboardWsInject'
import { useAuthStore } from '@/stores/authStore'

const { t } = useI18n()

// ── WebSocket ──

const { status: wsStatus, onWidgetUpdate } = useDashboardWebSocket()
provide(DASHBOARD_WS_KEY, { status: wsStatus, onWidgetUpdate })

// ── Permission ──

const authStore = useAuthStore()
const canManageDefault = computed(() =>
  authStore.userInfo?.permissions?.includes('DASHBOARD_MANAGE') ?? false,
)

// ── Core state ──

const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const layoutId = ref<number | null>(null)
const isDefault = ref(false)

// ── Page tabs ──

interface PageTab {
  key: string
  name: string
  widgets: WidgetConfig[]
}

let nextPageKey = 0
function createPageTab(name: string, widgets: WidgetConfig[] = []): PageTab {
  return { key: `p${nextPageKey++}`, name, widgets }
}

const pages = ref<PageTab[]>([])
const activeTabKey = ref('')
const currentPage = computed(() =>
  pages.value.find(p => p.key === activeTabKey.value) || pages.value[0],
)

// ── Theme ──

const currentTheme = ref<DashboardTheme>('light')
const customColor = ref('#409eff')

const echartsTheme = computed<string | undefined>(() =>
  currentTheme.value === 'dark' ? 'dark' : undefined,
)
provide(ECHARTS_THEME_KEY, echartsTheme)

const themeIcon = computed(() => {
  switch (currentTheme.value) {
    case 'dark': return Moon
    case 'custom': return Palette
    default: return Sun
  }
})

const themeStyles = computed(() => {
  if (currentTheme.value === 'custom') {
    return { '--dashboard-primary': customColor.value } as Record<string, string>
  }
  return {} as Record<string, string>
})

function handleThemeChange(cmd: string | number | object) {
  currentTheme.value = cmd as DashboardTheme
}

// ── Snapshot for cancel ──

let snapshot = ''

// ── Parse / Serialize ──

function parseDashboardData(json: string) {
  const parsed = JSON.parse(json)
  // v1 format: plain widget array
  if (Array.isArray(parsed)) {
    return {
      pages: [createPageTab(t('dashboard.defaultPage'), parsed)],
      theme: 'light' as DashboardTheme,
      customColor: '#409eff',
      activePageIndex: 0,
    }
  }
  // v2 format
  const pageTabs = ((parsed.pages || []) as { name: string; widgets: WidgetConfig[] }[]).map(
    p => createPageTab(p.name, p.widgets),
  )
  if (pageTabs.length === 0) pageTabs.push(createPageTab(t('dashboard.defaultPage')))
  return {
    pages: pageTabs,
    theme: (parsed.theme || 'light') as DashboardTheme,
    customColor: (parsed.customColor || '#409eff') as string,
    activePageIndex: Math.min(parsed.activePageIndex ?? 0, pageTabs.length - 1),
  }
}

function serializeDashboardData(): string {
  return JSON.stringify({
    version: 2,
    theme: currentTheme.value,
    customColor: customColor.value,
    activePageIndex: pages.value.findIndex(p => p.key === activeTabKey.value),
    pages: pages.value.map(p => ({ name: p.name, widgets: p.widgets })),
  })
}

// ── Data ──

function applyData(json: string) {
  const data = parseDashboardData(json)
  pages.value = data.pages
  activeTabKey.value = pages.value[data.activePageIndex]?.key || pages.value[0]?.key || ''
  currentTheme.value = data.theme
  customColor.value = data.customColor
}

function applyDefaultLayout() {
  pages.value = [createPageTab(t('dashboard.defaultPage'), buildDefaultLayout())]
  activeTabKey.value = pages.value[0].key
  currentTheme.value = 'light'
  customColor.value = '#409eff'
  isDefault.value = true
}

async function fetchLayout() {
  loading.value = true
  try {
    const res = await getLayout()
    if (res.body?.layoutJson) {
      applyData(res.body.layoutJson)
      layoutId.value = res.body.id
      isDefault.value = res.body.isDefault ?? false
    } else {
      applyDefaultLayout()
    }
  } catch {
    applyDefaultLayout()
  } finally {
    loading.value = false
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await saveLayout({ layoutJson: serializeDashboardData() })
    layoutId.value = res.body.id
    isDefault.value = false
    editing.value = false
    ElMessage.success(t('dashboard.saveSuccess'))
  } catch {
    ElMessage.error(t('dashboard.saveFailed'))
  } finally {
    saving.value = false
  }
}

const savingDefault = ref(false)

async function handleSaveAsDefault() {
  try {
    await ElMessageBox.confirm(t('dashboard.saveAsDefaultConfirm'), t('common.confirm'), { type: 'warning' })
  } catch { return }

  savingDefault.value = true
  try {
    await saveDefaultLayout({ layoutJson: serializeDashboardData(), roleType: null })
    ElMessage.success(t('dashboard.saveAsDefaultSuccess'))
  } catch {
    ElMessage.error(t('dashboard.saveAsDefaultFailed'))
  } finally {
    savingDefault.value = false
  }
}

async function handleReset() {
  try {
    await ElMessageBox.confirm(t('dashboard.resetConfirm'), t('common.confirm'), { type: 'warning' })
  } catch { return }

  loading.value = true
  try {
    const res = await resetLayout()
    if (res.body?.layoutJson) {
      applyData(res.body.layoutJson)
    } else {
      applyDefaultLayout()
    }
    isDefault.value = true
    editing.value = false
    ElMessage.success(t('dashboard.resetSuccess'))
  } catch {
    ElMessage.error(t('dashboard.resetFailed'))
  } finally {
    loading.value = false
  }
}

function startEditing() {
  snapshot = serializeDashboardData()
  editing.value = true
}

function cancelEditing() {
  applyData(snapshot)
  editing.value = false
}

function removeWidget(idx: number) {
  currentPage.value.widgets.splice(idx, 1)
}

// ── Tab management ──

function addPage() {
  const page = createPageTab(`${t('dashboard.page')} ${pages.value.length + 1}`)
  pages.value.push(page)
  activeTabKey.value = page.key
}

async function renamePage(page: PageTab) {
  try {
    const { value } = await ElMessageBox.prompt(
      t('dashboard.renamePagePrompt'),
      t('dashboard.renamePage'),
      { inputValue: page.name, inputPattern: /\S+/, inputErrorMessage: t('dashboard.pageNameRequired') },
    )
    page.name = value
  } catch { /* cancelled */ }
}

function handleTabRemove(tabKey: string | number) {
  const key = String(tabKey)
  const idx = pages.value.findIndex(p => p.key === key)
  if (idx === -1 || pages.value.length <= 1) return

  if (key === activeTabKey.value) {
    activeTabKey.value = pages.value[idx > 0 ? idx - 1 : idx + 1].key
  }
  pages.value.splice(idx, 1)
}

// ── Add Widget ──

const showAddDrawer = ref(false)
const availableToAdd = computed(() => {
  if (!currentPage.value) return getAllWidgetMetas()
  const usedTypes = new Set(currentPage.value.widgets.map(w => w.type))
  return getAllWidgetMetas().filter(m => !usedTypes.has(m.type))
})

function addWidget(type: WidgetType) {
  const meta = getWidgetMeta(type)
  const widgets = currentPage.value.widgets
  const maxI = widgets.reduce((max, w) => {
    const num = parseInt(w.i.replace('w', ''), 10)
    return isNaN(num) ? max : Math.max(max, num)
  }, -1)
  const bottomY = widgets.reduce((max, w) => Math.max(max, w.y + w.h), 0)
  widgets.push({
    i: `w${maxI + 1}`,
    x: 0,
    y: bottomY,
    w: meta.defaultW,
    h: meta.defaultH,
    type,
  })
  showAddDrawer.value = false
}

onMounted(() => fetchLayout())
</script>

<template>
  <div class="page-container" :class="`theme-${currentTheme}`" :style="themeStyles">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <LayoutDashboard :size="22" />
        <h2>{{ t('dashboard.title') }}</h2>
        <el-tag v-if="isDefault" size="small" type="info">{{ t('dashboard.defaultLayout') }}</el-tag>
        <el-tooltip :content="t('dashboard.wsStatus.' + wsStatus)" placement="bottom">
          <span class="ws-indicator" :class="'ws-' + wsStatus" />
        </el-tooltip>
      </div>
      <div class="header-right">
        <!-- Theme selector (edit mode) -->
        <el-dropdown v-if="editing" trigger="click" @command="handleThemeChange">
          <el-button>
            <component :is="themeIcon" :size="14" class="btn-icon" />
            {{ t('dashboard.theme') }}
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="light">
                <Sun :size="14" style="margin-right: 6px" /> {{ t('dashboard.themeLight') }}
              </el-dropdown-item>
              <el-dropdown-item command="dark">
                <Moon :size="14" style="margin-right: 6px" /> {{ t('dashboard.themeDark') }}
              </el-dropdown-item>
              <el-dropdown-item command="custom">
                <Palette :size="14" style="margin-right: 6px" /> {{ t('dashboard.themeCustom') }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-color-picker
          v-if="editing && currentTheme === 'custom'"
          v-model="customColor"
          size="small"
        />

        <template v-if="editing">
          <el-button @click="showAddDrawer = true">
            {{ t('dashboard.addWidget') }}
          </el-button>
          <el-button @click="handleReset">
            <RotateCcw :size="14" class="btn-icon" />
            {{ t('dashboard.reset') }}
          </el-button>
          <el-button type="primary" :loading="saving" @click="handleSave">
            <Save :size="14" class="btn-icon" />
            {{ t('dashboard.save') }}
          </el-button>
          <el-button
            v-if="canManageDefault"
            type="warning"
            :loading="savingDefault"
            @click="handleSaveAsDefault"
          >
            <Upload :size="14" class="btn-icon" />
            {{ t('dashboard.saveAsDefault') }}
          </el-button>
          <el-button @click="cancelEditing">{{ t('common.cancel') }}</el-button>
        </template>
        <el-button v-else @click="startEditing">
          <Settings :size="14" class="btn-icon" />
          {{ t('dashboard.customize') }}
        </el-button>
      </div>
    </div>

    <!-- Page tabs -->
    <div v-if="pages.length > 1 || editing" class="page-tabs">
      <el-tabs v-model="activeTabKey" type="card" @tab-remove="handleTabRemove">
        <el-tab-pane
          v-for="page in pages"
          :key="page.key"
          :name="page.key"
          :closable="editing && pages.length > 1"
        >
          <template #label>
            <span @dblclick.stop="editing && renamePage(page)">{{ page.name }}</span>
          </template>
        </el-tab-pane>
      </el-tabs>
      <el-button v-if="editing" text type="primary" size="small" class="add-page-btn" @click="addPage">
        <Plus :size="14" />
      </el-button>
    </div>

    <!-- Grid -->
    <div v-loading="loading" class="grid-wrapper">
      <GridLayout
        v-if="currentPage"
        v-model:layout="currentPage.widgets"
        :col-num="12"
        :row-height="60"
        :margin="[12, 12]"
        :is-draggable="editing"
        :is-resizable="editing"
      >
        <GridItem
          v-for="(widget, idx) in currentPage.widgets"
          :key="widget.i"
          :i="widget.i"
          :x="widget.x"
          :y="widget.y"
          :w="widget.w"
          :h="widget.h"
          :min-w="getWidgetMeta(widget.type).minW"
          :min-h="getWidgetMeta(widget.type).minH"
          drag-allow-from=".widget-header"
        >
          <WidgetContainer
            :widget-type="widget.type"
            :editing="editing"
            @remove="removeWidget(idx)"
          />
        </GridItem>
      </GridLayout>

      <el-empty v-if="!loading && currentPage && currentPage.widgets.length === 0" :description="t('dashboard.noWidgets')" />
    </div>

    <!-- Add Widget Drawer -->
    <el-drawer
      v-model="showAddDrawer"
      :title="t('dashboard.addWidget')"
      direction="rtl"
      size="360px"
    >
      <div v-if="availableToAdd.length === 0" class="drawer-empty">
        {{ t('dashboard.allWidgetsAdded') }}
      </div>
      <div v-else class="widget-list">
        <div
          v-for="meta in availableToAdd"
          :key="meta.type"
          class="widget-list-item"
          :class="{ 'is-unavailable': !meta.available }"
          @click="addWidget(meta.type)"
        >
          <span>{{ t(meta.labelKey) }}</span>
          <el-tag v-if="!meta.available" size="small" type="warning">
            {{ t('dashboard.moduleNotEnabled') }}
          </el-tag>
          <el-tag v-if="meta.realtime" size="small" type="success">
            {{ t('dashboard.realtime') }}
          </el-tag>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.page-container {
  padding: 20px;
  transition: background-color 0.3s, color 0.3s;
}

/* ── Dark Theme ── */
.page-container.theme-dark {
  --el-bg-color: #141414;
  --el-bg-color-overlay: #1d1d1d;
  --el-bg-color-page: #0a0a0a;
  --el-text-color-primary: #e5eaf3;
  --el-text-color-regular: #cfd3dc;
  --el-text-color-secondary: #a3a6ad;
  --el-text-color-placeholder: #8d9095;
  --el-border-color: #4c4d4f;
  --el-border-color-light: #414243;
  --el-border-color-lighter: #363637;
  --el-border-color-extra-light: #2b2b2c;
  --el-fill-color: #303030;
  --el-fill-color-light: #262727;
  --el-fill-color-lighter: #1d1d1d;
  --el-fill-color-extra-light: #191919;
  --el-fill-color-blank: #141414;
  background-color: var(--el-bg-color-page);
  color: var(--el-text-color-primary);
}

/* ── Custom Theme ── */
.page-container.theme-custom {
  --el-color-primary: var(--dashboard-primary);
  --el-color-primary-light-3: color-mix(in srgb, var(--dashboard-primary), white 30%);
  --el-color-primary-light-5: color-mix(in srgb, var(--dashboard-primary), white 50%);
  --el-color-primary-light-7: color-mix(in srgb, var(--dashboard-primary), white 70%);
  --el-color-primary-light-8: color-mix(in srgb, var(--dashboard-primary), white 80%);
  --el-color-primary-light-9: color-mix(in srgb, var(--dashboard-primary), white 90%);
  --el-color-primary-dark-2: color-mix(in srgb, var(--dashboard-primary), black 20%);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 8px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-icon {
  margin-right: 4px;
}

/* ── Page Tabs ── */
.page-tabs {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  gap: 4px;
}

.page-tabs :deep(.el-tabs) {
  flex: 1;
}

.page-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.page-tabs :deep(.el-tabs__content) {
  display: none;
}

.add-page-btn {
  flex-shrink: 0;
}

.grid-wrapper {
  min-height: 400px;
}

.widget-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.widget-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.widget-list-item:hover {
  background: var(--el-fill-color-lighter);
}

.widget-list-item.is-unavailable {
  opacity: 0.7;
}

.drawer-empty {
  text-align: center;
  color: var(--el-text-color-secondary);
  padding: 40px 0;
}

/* ── WebSocket indicator ── */
.ws-indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--el-text-color-placeholder);
  transition: background 0.3s;
}
.ws-connected { background: #67c23a; }
.ws-connecting, .ws-reconnecting { background: #e6a23c; animation: ws-blink 1s infinite; }
.ws-disconnected { background: #f56c6c; }

@keyframes ws-blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}
</style>
