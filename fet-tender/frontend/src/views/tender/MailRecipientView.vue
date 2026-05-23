<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listMailRecipients,
  createMailRecipient,
  updateMailRecipient,
  deleteMailRecipient,
  batchImportMailRecipients,
} from '@/api/tender'
import type { MailRecipientResponse, MailRecipientRequest, MailRecipientBatchResult } from '@/types/tender'

const { t } = useI18n()

// ── 列表 ──────────────────────────────────────────────────────
const loading = ref(false)
const tableData = ref<MailRecipientResponse[]>([])
const includeInactive = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const res = await listMailRecipients(includeInactive.value)
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
})

// ── Dialog ────────────────────────────────────────────────────
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const saving = ref(false)

const form = reactive<MailRecipientRequest>({
  email: '',
  name: null,
  isActive: true,
})

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = null
  form.email = ''
  form.name = null
  form.isActive = true
  dialogVisible.value = true
}

function openEdit(row: MailRecipientResponse) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  form.email = row.email
  form.name = row.name
  form.isActive = row.isActive
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.email.trim() || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    ElMessage.warning(t('tender.mailRecipient.validationRequired'))
    return
  }
  saving.value = true
  try {
    if (dialogMode.value === 'create') {
      await createMailRecipient({ ...form })
      ElMessage.success(t('common.save') + t('common.confirm'))
    } else {
      await updateMailRecipient(editingId.value!, { ...form })
      ElMessage.success(t('common.save') + t('common.confirm'))
    }
    dialogVisible.value = false
    fetchList()
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    saving.value = false
  }
}

async function handleDelete(row: MailRecipientResponse) {
  await ElMessageBox.confirm(
    t('tender.mailRecipient.confirmDelete', { email: row.email }),
    t('common.confirmDelete'),
    { type: 'warning' },
  )
  try {
    await deleteMailRecipient(row.id)
    ElMessage.success(t('common.delete') + t('common.confirm'))
    fetchList()
  } catch {
    ElMessage.error(t('common.operationFailed'))
  }
}

// ── Batch Import ─────────────────────────────────────────────
const batchDialogVisible = ref(false)
const batchText = ref('')
const batchImporting = ref(false)
const batchResult = ref<MailRecipientBatchResult | null>(null)

function openBatchImport() {
  batchText.value = ''
  batchResult.value = null
  batchDialogVisible.value = true
}

async function handleBatchImport() {
  const lines = batchText.value
    .split(/[\n,;]+/)
    .map((s) => s.trim())
    .filter((s) => s.length > 0)

  if (lines.length === 0) {
    ElMessage.warning(t('tender.mailRecipient.batchEmpty'))
    return
  }
  if (lines.length > 100) {
    ElMessage.warning(t('tender.mailRecipient.batchMax'))
    return
  }

  batchImporting.value = true
  try {
    const res = await batchImportMailRecipients(lines)
    if (res.errorCode === '00000') {
      batchResult.value = res.body
      if (res.body.successCount > 0) {
        ElMessage.success(t('tender.mailRecipient.batchSuccess', { count: res.body.successCount }))
        fetchList()
      }
    }
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    batchImporting.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">
        <el-icon class="mr-1"><Plus /></el-icon>{{ $t('common.add') }}
      </el-button>
      <el-button @click="openBatchImport">
        {{ $t('tender.mailRecipient.batchImport') }}
      </el-button>
      <el-checkbox v-model="includeInactive" class="ml-4" @change="fetchList">
        {{ $t('tender.mailRecipient.showInactive') }}
      </el-checkbox>
    </div>

    <el-table v-loading="loading" :data="pagedData" border stripe style="width: 100%">
      <el-table-column prop="email" :label="$t('tender.mailRecipient.email')" min-width="250" />
      <el-table-column prop="name" :label="$t('tender.mailRecipient.name')" min-width="150">
        <template #default="{ row }">{{ row.name || '-' }}</template>
      </el-table-column>
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
      :title="dialogMode === 'create' ? $t('tender.mailRecipient.createTitle') : $t('tender.mailRecipient.editTitle')"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="120px" @submit.prevent>
        <el-form-item :label="$t('tender.mailRecipient.email')" required>
          <el-input
            v-model="form.email"
            :placeholder="$t('tender.mailRecipient.emailPlaceholder')"
            type="email"
          />
        </el-form-item>
        <el-form-item :label="$t('tender.mailRecipient.name')">
          <el-input
            v-model="form.name"
            :placeholder="$t('tender.mailRecipient.namePlaceholder')"
          />
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

    <!-- Batch Import Dialog -->
    <el-dialog
      v-model="batchDialogVisible"
      :title="$t('tender.mailRecipient.batchImport')"
      width="540px"
      destroy-on-close
    >
      <p class="batch-hint">{{ $t('tender.mailRecipient.batchHint') }}</p>
      <el-input
        v-model="batchText"
        type="textarea"
        :rows="8"
        :placeholder="$t('tender.mailRecipient.batchPlaceholder')"
        :disabled="batchImporting"
      />
      <!-- 匯入結果 -->
      <div v-if="batchResult" class="batch-result">
        <el-alert
          :title="`成功匯入 ${batchResult.successCount} 筆，略過 ${batchResult.skippedCount} 筆`"
          :type="batchResult.skippedCount === 0 ? 'success' : 'warning'"
          show-icon
          :closable="false"
        />
        <ul v-if="batchResult.skippedItems.length > 0" class="skipped-list">
          <li v-for="item in batchResult.skippedItems" :key="item.email">
            <code>{{ item.email }}</code> — {{ item.reason }}
          </li>
        </ul>
      </div>
      <template #footer>
        <el-button @click="batchDialogVisible = false">{{ $t('common.close') }}</el-button>
        <el-button
          type="primary"
          :loading="batchImporting"
          :disabled="!batchText.trim()"
          @click="handleBatchImport"
        >
          {{ $t('tender.mailRecipient.batchImport') }}
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
.batch-hint { margin: 0 0 12px; color: var(--el-text-color-secondary); font-size: 13px; }
.batch-result { margin-top: 12px; }
.skipped-list { margin: 8px 0 0; padding-left: 20px; font-size: 13px; }
.skipped-list li { margin-bottom: 4px; }
</style>
