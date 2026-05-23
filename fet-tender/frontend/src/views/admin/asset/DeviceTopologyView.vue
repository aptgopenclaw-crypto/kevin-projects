<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { Network, Search } from 'lucide-vue-next'
import { getDevices } from '@/api/device'
import type { DeviceResponse } from '@/types/device'

const { t } = useI18n()

const loading = ref(false)
const keyword = ref('')
const devices = ref<DeviceResponse[]>([])

interface TreeNode extends Omit<DeviceResponse, 'children'> {
  children: TreeNode[] | null
}

const treeData = ref<TreeNode[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await getDevices({ page: 0, size: 9999 })
    devices.value = res.body.content
    buildTree()
  } catch {
    ElMessage.error(t('topology.loadFailed'))
  } finally {
    loading.value = false
  }
}

function buildTree() {
  const map = new Map<number, TreeNode>()
  const roots: TreeNode[] = []
  for (const d of devices.value) {
    map.set(d.id, { ...d, children: [] })
  }
  for (const d of devices.value) {
    const node = map.get(d.id)!
    if (d.parentDeviceId && map.has(d.parentDeviceId)) {
      map.get(d.parentDeviceId)!.children!.push(node)
    } else {
      roots.push(node)
    }
  }
  treeData.value = roots
}

const treeProps = { children: 'children', label: 'deviceCode' }

function handleSearch() {
  // Re-filter on client side for simplicity
  if (!keyword.value) {
    buildTree()
    return
  }
  const kw = keyword.value.toLowerCase()
  treeData.value = treeData.value.filter(
    (n) => n.deviceCode.toLowerCase().includes(kw) || n.deviceName?.toLowerCase().includes(kw),
  )
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <div class="page-content">
      <div class="page-header">
        <div class="header-left">
          <div class="header-icon"><Network :size="20" /></div>
          <div>
            <h2 class="header-title">{{ t('topology.title') }}</h2>
            <p class="header-subtitle">{{ t('topology.subtitle') }}</p>
          </div>
        </div>
      </div>

      <div class="filter-bar">
        <el-input v-model="keyword" :placeholder="t('topology.searchPlaceholder')" clearable style="width: 280px" @keyup.enter="handleSearch" />
        <el-button class="search-btn" @click="handleSearch"><Search :size="16" /></el-button>
      </div>

      <div class="tree-card" v-loading="loading">
        <el-tree
          v-if="treeData.length"
          :data="treeData"
          :props="treeProps"
          node-key="id"
          default-expand-all
        >
          <template #default="{ data }">
            <div class="tree-node">
              <span class="tree-code">{{ data.deviceCode }}</span>
              <span class="tree-name">{{ data.deviceName }}</span>
              <span v-if="data.children?.length" class="tree-count">
                {{ t('topology.childCount', { count: data.children.length }) }}
              </span>
            </div>
          </template>
        </el-tree>
        <div v-else class="no-data">{{ t('topology.noData') }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }
.page-content {}
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon {
  width: 40px; height: 40px;
  background: rgba(95, 201, 146, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #5fc992;
}
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); font-family: 'Inter', sans-serif; margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); font-family: 'Inter', sans-serif; margin: 2px 0 0; }
.filter-bar { display: flex; gap: 10px; align-items: center; margin-bottom: 16px; }
.search-btn { background: var(--btn-primary-bg); color: var(--btn-primary-text); border: none; border-radius: 8px; padding: 8px 14px; }
.search-btn:hover { background: var(--btn-primary-hover); color: var(--btn-primary-text); }
.tree-card {
  background: var(--bg-surface);
  border: 1px solid var(--bg-active);
  border-radius: 12px;
  padding: 20px;
  min-height: 300px;
}
.no-data { text-align: center; color: var(--text-muted); padding: 60px 0; font-size: 14px; }

.tree-node { display: flex; align-items: center; gap: 10px; padding: 4px 0; }
.tree-code { color: #55b3ff; font-weight: 600; font-family: 'JetBrains Mono', monospace; font-size: 13px; }
.tree-name { color: var(--text-primary); font-size: 13px; }
.tree-count { color: var(--text-muted); font-size: 12px; }

:deep(.el-tree) {
  background: transparent;
  --el-tree-node-hover-bg-color: var(--bg-hover-subtle);
  color: var(--text-primary);
}
:deep(.el-tree-node__content) { height: 36px; }
:deep(.el-input__wrapper) { background-color: var(--bg-base); border: 1px solid var(--border-medium); border-radius: 8px; box-shadow: none; }
:deep(.el-input__wrapper:hover) { border-color: var(--border-strong); }
:deep(.el-input__wrapper.is-focus) { border-color: rgba(85, 179, 255, 0.5); box-shadow: 0 0 0 3px rgba(85, 179, 255, 0.15); }
:deep(.el-input__inner) { color: var(--text-primary); font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500; }
:deep(.el-input__inner::placeholder) { color: var(--text-muted); }
</style>
