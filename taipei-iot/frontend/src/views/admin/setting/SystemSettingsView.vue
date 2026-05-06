<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { listSettings, updateSetting, type SystemSettingDto } from '@/api/setting'
import { Pencil } from 'lucide-vue-next'

const { t } = useI18n()

const settings = ref<SystemSettingDto[]>([])
const loading = ref(false)

// ---- Edit dialog ----
const editDialogVisible = ref(false)
const editingRow = ref<SystemSettingDto | null>(null)
const editValue = ref('')
const saving = ref(false)

onMounted(async () => {
  await loadSettings()
})

async function loadSettings() {
  loading.value = true
  try {
    const res = await listSettings()
    settings.value = res.body
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    loading.value = false
  }
}

function openEdit(row: SystemSettingDto) {
  editingRow.value = row
  editValue.value = row.settingValue
  editDialogVisible.value = true
}

async function handleSaveEdit() {
  if (!editingRow.value) return
  saving.value = true
  try {
    const res = await updateSetting(editingRow.value.settingKey, editValue.value)
    const idx = settings.value.findIndex(s => s.settingKey === res.body.settingKey)
    if (idx >= 0) settings.value[idx] = res.body
    editDialogVisible.value = false
    ElMessage.success(t('setting.saveSuccess'))
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <div class="page-content">
      <div class="page-header">
        <div>
          <h1 class="page-title">{{ t('setting.title') }}</h1>
          <p class="page-subtitle">{{ t('setting.subtitle') }}</p>
        </div>
      </div>

      <div class="table-card" v-loading="loading">
        <el-table :data="settings" style="width: 100%" row-class-name="table-row">
          <el-table-column prop="description" :label="t('setting.colDescription')" min-width="280" />
          <el-table-column prop="settingValue" :label="t('setting.colValue')" width="180" />
          <el-table-column :label="t('setting.colAction')" width="100" align="center" fixed="right">
            <template #default="{ row }">
              <div class="action-group">
                <el-button class="action-btn" size="small" @click="openEdit(row)">
                  <Pencil :size="14" />
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- Edit Dialog -->
    <el-dialog
      v-model="editDialogVisible"
      :title="t('setting.editDialogTitle')"
      width="420px"
      :close-on-click-modal="false"
    >
      <div class="edit-form">
        <div class="form-row">
          <label class="form-label">{{ t('setting.colDescription') }}</label>
          <div class="form-value-readonly">{{ editingRow?.description }}</div>
        </div>
        <div class="form-row">
          <label class="form-label">{{ t('setting.colValue') }}</label>
          <el-input v-model="editValue" />
        </div>
      </div>
      <template #footer>
        <el-button @click="editDialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveEdit">
          {{ t('common.save') }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding: 32px 24px;
  min-height: 100vh;
  background-color: var(--bg-base);
}

.page-content {
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-title {
  font-family: 'Inter', sans-serif;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.15;
  color: var(--text-heading);
  margin: 0 0 8px 0;
}

.page-subtitle {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.6;
  letter-spacing: 0.2px;
  color: var(--text-secondary);
  margin: 0;
}

.table-card {
  background-color: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 0;
  box-shadow: var(--shadow-card);
  overflow: hidden;
}

/* Action button — same as UserListView */
.action-group {
  display: flex;
  gap: 6px;
  justify-content: center;
}

.action-btn {
  border: 1px solid var(--border-light);
  background: transparent;
  color: var(--text-secondary);
  padding: 4px 8px;
  border-radius: 6px;
}
.action-btn:hover {
  background: var(--bg-hover);
  color: #55b3ff;
  border-color: #55b3ff;
}

/* Edit dialog form */
.edit-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-family: 'Inter', sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.form-value-readonly {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  color: var(--text-primary);
  padding: 8px 0;
}
</style>
