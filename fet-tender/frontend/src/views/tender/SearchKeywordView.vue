<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  listSearchKeywords,
  createSearchKeyword,
  updateSearchKeyword,
  deleteSearchKeyword,
  listConfigSolutions,
} from '@/api/tender'
import type { SearchKeywordResponse, SearchKeywordRequest } from '@/types/tender'

const { t } = useI18n()

// ── 方案選項 ──────────────────────────────────────────────────
const solutionOptions = ref<string[]>([])

async function fetchSolutions() {
  try {
    const res = await listConfigSolutions()
    if (res.errorCode === '00000') solutionOptions.value = res.body
  } catch { /* ignore */ }
}

// ── 列表 ──────────────────────────────────────────────────────
const loading = ref(false)
const tableData = ref<SearchKeywordResponse[]>([])
const includeInactive = ref(false)
const solutionFilter = ref('')

async function fetchList() {
  loading.value = true
  try {
    const res = await listSearchKeywords(
      includeInactive.value,
      solutionFilter.value || undefined,
    )
    if (res.errorCode === '00000') tableData.value = res.body
  } finally {
    loading.value = false
  }
}

// ── 分頁 ──────────────────────────────────────────────────────
const currentPage = ref(1)
const pageSize = ref(20)
const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return tableData.value.slice(start, start + pageSize.value)
})

onMounted(() => {
  fetchList()
  fetchSolutions()
})

// ── Dialog ────────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<SearchKeywordRequest>({
  solution: '',
  keyword: '',
  isActive: true,
})

const formRules = reactive<FormRules>({
  solution: [
    { required: true, message: () => t('tender.keyword.validationRequired'), trigger: 'change' },
    { max: 100, message: () => t('common.maxLength', { max: 100 }), trigger: 'blur' },
  ],
  keyword: [
    { required: true, message: () => t('tender.keyword.validationRequired'), trigger: 'blur' },
    { max: 200, message: () => t('common.maxLength', { max: 200 }), trigger: 'blur' },
  ],
})

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  form.solution = ''
  form.keyword = ''
  form.isActive = true
  dialogVisible.value = true
}

function openEdit(row: SearchKeywordResponse) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.solution = row.solution
  form.keyword = row.keyword
  form.isActive = row.isActive
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      await createSearchKeyword({ ...form })
      ElMessage.success(t('common.save') + t('common.confirm'))
    } else {
      await updateSearchKeyword(editingId.value!, { ...form })
      ElMessage.success(t('common.save') + t('common.confirm'))
    }
    dialogVisible.value = false
    fetchList()
    fetchSolutions()
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: SearchKeywordResponse) {
  await ElMessageBox.confirm(
    t('tender.keyword.confirmDelete', { keyword: row.keyword }),
    t('common.confirmDelete'),
    { type: 'warning' },
  )
  try {
    await deleteSearchKeyword(row.id)
    ElMessage.success(t('common.delete') + t('common.confirm'))
    fetchList()
  } catch {
    ElMessage.error(t('common.operationFailed'))
  }
}
</script>

<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">
        <el-icon class="mr-1"><Plus /></el-icon>{{ $t('common.add') }}
      </el-button>
      <el-select
        v-model="solutionFilter"
        filterable
        clearable
        :placeholder="$t('tender.keyword.solutionPlaceholder')"
        style="width: 240px; margin-left: 16px;"
        @change="fetchList"
      >
        <el-option
          v-for="sol in solutionOptions"
          :key="sol"
          :label="sol"
          :value="sol"
        />
      </el-select>
      <el-checkbox v-model="includeInactive" class="ml-4" @change="fetchList">
        {{ $t('tender.keyword.showInactive') }}
      </el-checkbox>
    </div>

    <el-table v-loading="loading" :data="pagedData" border stripe style="width: 100%">
      <el-table-column prop="solution" :label="$t('tender.keyword.solution')" width="200" />
      <el-table-column prop="keyword" :label="$t('tender.keyword.keyword')" />
      <el-table-column prop="isActive" :label="$t('common.status')" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'" size="small">
            {{ row.isActive ? $t('common.enabled') : $t('common.disabled') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" :label="$t('common.createTime')" width="180">
        <template #default="{ row }">{{ row.createdAt?.slice(0, 19).replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column :label="$t('common.actions')" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">
            {{ $t('common.delete') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="tableData.length > pageSize"
      class="mt-4"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :page-sizes="[10, 20, 50, 100]"
      :total="tableData.length"
      layout="total, sizes, prev, pager, next"
    />

    <!-- Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? $t('tender.keyword.createTitle') : $t('tender.keyword.editTitle')"
      width="480px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="120px" @submit.prevent>
        <el-form-item :label="$t('tender.keyword.solution')" prop="solution">
          <el-select
            v-model="form.solution"
            filterable
            allow-create
            default-first-option
            :placeholder="$t('tender.keyword.solutionPlaceholder')"
            style="width: 100%"
          >
            <el-option
              v-for="sol in solutionOptions"
              :key="sol"
              :label="sol"
              :value="sol"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('tender.keyword.keyword')" prop="keyword">
          <el-input v-model="form.keyword" :placeholder="$t('tender.keyword.keywordPlaceholder')" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item :label="$t('common.status')">
          <el-switch v-model="form.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">
          {{ $t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 20px; }
.toolbar { margin-bottom: 16px; display: flex; align-items: center; }
.mr-1 { margin-right: 4px; }
.ml-4 { margin-left: 16px; }
.mt-4 { margin-top: 16px; }
</style>
