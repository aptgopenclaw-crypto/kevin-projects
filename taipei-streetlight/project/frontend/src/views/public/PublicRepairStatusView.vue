<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { queryRepairStatus, type PublicRepairStatusResult } from '@/api/repair'

const { t } = useI18n()

const formRef = ref<FormInstance>()
const loading = ref(false)
const result = ref<PublicRepairStatusResult | null>(null)

const form = reactive({
  ticketNo: '',
  phone: '',
})

const rules = {
  ticketNo: [{ required: true, message: t('publicRepair.validation.ticketNoRequired'), trigger: 'blur' }],
  phone: [
    { required: true, message: t('publicRepair.validation.phoneRequired'), trigger: 'blur' },
    { pattern: /^09\d{8}$/, message: t('publicRepair.validation.phoneFormat'), trigger: 'blur' },
  ],
}

async function handleQuery() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  result.value = null
  try {
    const res = await queryRepairStatus(form.ticketNo, form.phone)
    result.value = res.body
  } catch {
    ElMessage.error(t('publicRepair.statusNotFound'))
  } finally {
    loading.value = false
  }
}

function formatDateTime(dt: string | null) {
  if (!dt) return '-'
  return new Date(dt).toLocaleString('zh-TW')
}
</script>

<template>
  <div class="public-page">
    <div class="public-card">
      <h1 class="public-title">{{ t('publicRepair.statusTitle') }}</h1>
      <p class="public-subtitle">{{ t('publicRepair.statusSubtitle') }}</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" size="large">
        <el-form-item :label="t('publicRepair.ticketNo')" prop="ticketNo">
          <el-input v-model="form.ticketNo" :placeholder="t('publicRepair.placeholder.ticketNo')" />
        </el-form-item>

        <el-form-item :label="t('publicRepair.reporterPhone')" prop="phone">
          <el-input v-model="form.phone" :placeholder="t('publicRepair.placeholder.phone')" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleQuery">
            {{ t('publicRepair.queryStatus') }}
          </el-button>
        </el-form-item>
      </el-form>

      <!-- Result -->
      <div v-if="result" class="status-result">
        <div class="result-row">
          <span class="result-label">{{ t('publicRepair.ticketNo') }}</span>
          <span class="result-value ticket-no">{{ result.ticketNumber }}</span>
        </div>
        <div class="result-row">
          <span class="result-label">{{ t('publicRepair.currentStatus') }}</span>
          <span class="result-value status-badge">{{ result.statusLabel }}</span>
        </div>
        <div class="result-row">
          <span class="result-label">{{ t('publicRepair.createdAt') }}</span>
          <span class="result-value">{{ formatDateTime(result.createdAt) }}</span>
        </div>
        <div class="result-row">
          <span class="result-label">{{ t('publicRepair.updatedAt') }}</span>
          <span class="result-value">{{ formatDateTime(result.updatedAt) }}</span>
        </div>
      </div>

      <div class="query-link">
        <router-link to="/public/repair">{{ t('publicRepair.goSubmitRepair') }}</router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.public-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  display: flex;
  justify-content: center;
  padding: 24px 16px;
}
.public-card {
  width: 100%;
  max-width: 520px;
  background: var(--bg-surface, #1e1e2e);
  border-radius: 16px;
  padding: 32px 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  height: fit-content;
}
.public-title {
  font-size: 22px;
  font-weight: 700;
  text-align: center;
  color: var(--text-heading, #e0e0e0);
  margin: 0 0 4px;
}
.public-subtitle {
  font-size: 13px;
  text-align: center;
  color: var(--text-secondary, #999);
  margin: 0 0 24px;
}
.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 10px;
}
.status-result {
  margin-top: 24px;
  background: var(--bg-base, #141422);
  border-radius: 12px;
  padding: 20px;
}
.result-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid var(--bg-active, #2a2a3e);
}
.result-row:last-child { border-bottom: none; }
.result-label {
  color: var(--text-secondary, #999);
  font-size: 14px;
}
.result-value {
  color: var(--text-heading, #e0e0e0);
  font-size: 14px;
  font-weight: 600;
}
.ticket-no { color: #409eff; }
.status-badge {
  background: rgba(64, 158, 255, 0.15);
  color: #409eff;
  padding: 2px 12px;
  border-radius: 6px;
  font-size: 13px;
}
.query-link {
  text-align: center;
  margin-top: 20px;
}
.query-link a {
  color: #409eff;
  font-size: 14px;
  text-decoration: none;
}
</style>
