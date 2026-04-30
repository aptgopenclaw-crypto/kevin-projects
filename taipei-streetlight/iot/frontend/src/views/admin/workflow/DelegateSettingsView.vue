<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Users, Plus, Trash2 } from 'lucide-vue-next'
import { getDelegates, createDelegate, deleteDelegate, getDelegateCandidates } from '@/api/workflow'
import type { DelegateSettingResponse, DelegateSettingRequest, DelegateCandidateDto } from '@/types/workflow'

const { t } = useI18n()

const loading = ref(false)
const items = ref<DelegateSettingResponse[]>([])

// Dialog
const dialogVisible = ref(false)
const dialogLoading = ref(false)
const dialogFormRef = ref<FormInstance>()
const candidates = ref<DelegateCandidateDto[]>([])
const candidatesLoading = ref(false)
const dialogForm = ref<DelegateSettingRequest>({
  delegateId: '',
  startDate: '',
  endDate: '',
  reason: '',
})

const dialogRules = computed<FormRules>(() => ({
  delegateId: [{ required: true, message: t('delegate.errors.delegateIdRequired'), trigger: 'change' }],
  startDate: [{ required: true, message: t('delegate.errors.startDateRequired'), trigger: 'change' }],
  endDate: [{ required: true, message: t('delegate.errors.endDateRequired'), trigger: 'change' }],
}))

async function loadData() {
  loading.value = true
  try {
    const res = await getDelegates()
    items.value = res.body
  } catch {
    ElMessage.error(t('delegate.loadFailed'))
  } finally {
    loading.value = false
  }
}

async function openCreateDialog() {
  dialogForm.value = { delegateId: '', startDate: '', endDate: '', reason: '' }
  dialogVisible.value = true
  candidatesLoading.value = true
  try {
    const res = await getDelegateCandidates()
    candidates.value = res.body
  } catch {
    ElMessage.error(t('delegate.loadFailed'))
  } finally {
    candidatesLoading.value = false
  }
}

async function handleCreate() {
  const form = dialogFormRef.value
  if (!form) return
  const valid = await form.validate().catch(() => false)
  if (!valid) return
  dialogLoading.value = true
  try {
    await createDelegate(dialogForm.value)
    ElMessage.success(t('delegate.createdSuccess'))
    dialogVisible.value = false
    loadData()
  } catch {
    ElMessage.error(t('delegate.loadFailed'))
  } finally {
    dialogLoading.value = false
  }
}

async function handleDeactivate(row: DelegateSettingResponse) {
  try {
    await ElMessageBox.confirm(t('delegate.deactivateConfirm'), { type: 'warning' })
    await deleteDelegate(row.id)
    ElMessage.success(t('delegate.deactivatedSuccess'))
    loadData()
  } catch { /* cancelled */ }
}

onMounted(() => loadData())
</script>

<template>
  <div class="page-container">
    <div class="page-content">
      <div class="page-header">
        <div class="header-left">
          <div class="header-icon"><Users :size="20" /></div>
          <div>
            <h2 class="header-title">{{ t('delegate.title') }}</h2>
            <p class="header-subtitle">{{ t('delegate.subtitle') }}</p>
          </div>
        </div>
        <el-button class="create-btn" @click="openCreateDialog">
          <Plus :size="16" style="margin-right: 6px" /> {{ t('delegate.addBtn') }}
        </el-button>
      </div>

      <div class="table-card" v-loading="loading">
        <el-table :data="items" stripe>
          <el-table-column prop="delegatorName" :label="t('delegate.colDelegator')" width="150" />
          <el-table-column prop="delegateName" :label="t('delegate.colDelegate')" width="150" />
          <el-table-column prop="startDate" :label="t('delegate.colStartDate')" width="130" />
          <el-table-column prop="endDate" :label="t('delegate.colEndDate')" width="130" />
          <el-table-column prop="reason" :label="t('delegate.colReason')" min-width="180" show-overflow-tooltip />
          <el-table-column :label="t('delegate.colActive')" width="100">
            <template #default="{ row }">
              <span class="status-badge" :class="row.isActive ? 'status-success' : 'status-muted'">
                {{ row.isActive ? t('delegate.activeYes') : t('delegate.activeNo') }}
              </span>
            </template>
          </el-table-column>
          <el-table-column :label="t('common.actions')" width="80" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.isActive" class="action-btn action-delete" size="small" @click="handleDeactivate(row)">
                <Trash2 :size="14" />
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- Create Dialog -->
    <el-dialog v-model="dialogVisible" :title="t('delegate.dialogCreateTitle')" width="480px" class="dark-dialog">
      <el-form ref="dialogFormRef" :model="dialogForm" :rules="dialogRules" label-position="top" v-loading="dialogLoading">
        <el-form-item :label="t('delegate.delegateIdLabel')" prop="delegateId">
          <el-select
            v-model="dialogForm.delegateId"
            :placeholder="t('delegate.delegateIdPlaceholder')"
            filterable
            :loading="candidatesLoading"
            style="width: 100%"
          >
            <el-option
              v-for="c in candidates"
              :key="c.userId"
              :value="c.userId"
              :label="`${c.displayName} (${c.userId})`"
            >
              <span>{{ c.displayName }}</span>
              <span style="float: right; color: var(--text-muted); font-size: 12px">{{ c.deptName }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 0 16px">
          <el-form-item :label="t('delegate.startDateLabel')" prop="startDate">
            <el-date-picker v-model="dialogForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('delegate.endDateLabel')" prop="endDate">
            <el-date-picker v-model="dialogForm.endDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </div>
        <el-form-item :label="t('delegate.reasonLabel')">
          <el-input v-model="dialogForm.reason" :placeholder="t('delegate.reasonPlaceholder')" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="cancel-btn" @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button class="submit-btn" @click="handleCreate" :loading="dialogLoading">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }
.page-content { max-width: 1400px; margin: 0 auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 12px; }
.header-icon {
  width: 40px; height: 40px;
  background: rgba(255, 188, 51, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #ffbc33;
}
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); font-family: 'Inter', sans-serif; margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); font-family: 'Inter', sans-serif; margin: 2px 0 0; }
.create-btn { background: var(--btn-primary-bg); color: var(--btn-primary-text); border: none; border-radius: 86px; padding: 10px 20px; font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 600; }
.create-btn:hover { background: var(--btn-primary-hover); color: var(--btn-primary-text); }
.table-card { background: var(--bg-surface); border: 1px solid var(--bg-active); border-radius: 12px; overflow: hidden; }

.status-badge { display: inline-block; padding: 2px 10px; border-radius: 6px; font-size: 12px; font-weight: 600; }
.status-success { background: rgba(95, 201, 146, 0.15); color: #5fc992; }
.status-muted { background: rgba(139, 143, 163, 0.15); color: #8b8fa3; }

.action-btn { padding: 4px 8px; min-width: auto; background: transparent; border-radius: 6px; }
.action-delete { color: #FF6363; border: 1px solid rgba(255, 99, 99, 0.2); }
.action-delete:hover { background: rgba(255, 99, 99, 0.15); color: #FF6363; }

.cancel-btn { background: transparent; color: var(--text-secondary); border: none; border-radius: 6px; padding: 8px 16px; font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 600; }
.cancel-btn:hover { opacity: 0.6; color: var(--text-primary); }
.submit-btn { background: var(--btn-primary-bg); color: var(--btn-primary-text); border: none; border-radius: 86px; padding: 8px 24px; font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 600; }
.submit-btn:hover { background: var(--btn-primary-hover); color: var(--btn-primary-text); }

:deep(.el-table) {
  --el-table-bg-color: var(--bg-surface); --el-table-tr-bg-color: var(--bg-surface);
  --el-table-header-bg-color: var(--bg-surface); --el-table-row-hover-bg-color: var(--bg-hover-subtle);
  --el-table-text-color: var(--text-primary); --el-table-header-text-color: var(--text-secondary);
  --el-table-border-color: var(--bg-active); font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500;
}
:deep(.el-table th.el-table__cell) { font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.3px; }
:deep(.el-input__wrapper) { background-color: var(--bg-base); border: 1px solid var(--border-medium); border-radius: 8px; box-shadow: none; }
:deep(.el-input__wrapper:hover) { border-color: var(--border-strong); }
:deep(.el-input__wrapper.is-focus) { border-color: rgba(85, 179, 255, 0.5); box-shadow: 0 0 0 3px rgba(85, 179, 255, 0.15); }
:deep(.el-input__inner) { color: var(--text-primary); font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500; }
:deep(.el-input__inner::placeholder) { color: var(--text-muted); }
:deep(.el-form-item__label) { color: var(--text-label); font-family: 'Inter', sans-serif; font-size: 14px; font-weight: 500; }
:deep(.el-select .el-input__wrapper) { background-color: var(--bg-base); border: 1px solid var(--border-medium); border-radius: 8px; box-shadow: none; }
</style>
