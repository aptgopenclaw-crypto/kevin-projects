<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { getCaptcha } from '@/api/auth'
import { submitPublicRepair } from '@/api/repair'

const { t } = useI18n()
const route = useRoute()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaImage = ref('')
const submitted = ref(false)
const resultTicketNo = ref('')
const resultMessage = ref('')

const form = reactive({
  reporterName: '',
  reporterPhone: '',
  reporterEmail: '',
  reportDescription: '',
  reportAddress: '',
  poleNumber: (route.query.pole as string) || '',
  captchaKey: '',
  captchaValue: '',
  privacyAgreed: false,
})

const rules = {
  reporterName: [{ required: true, message: t('publicRepair.validation.nameRequired'), trigger: 'blur' }],
  reporterPhone: [
    { required: true, message: t('publicRepair.validation.phoneRequired'), trigger: 'blur' },
    { pattern: /^09\d{8}$/, message: t('publicRepair.validation.phoneFormat'), trigger: 'blur' },
  ],
  reportDescription: [{ required: true, message: t('publicRepair.validation.descriptionRequired'), trigger: 'blur' }],
  reportAddress: [{ required: true, message: t('publicRepair.validation.addressRequired'), trigger: 'blur' }],
  captchaValue: [{ required: true, message: t('publicRepair.validation.captchaRequired'), trigger: 'blur' }],
}

async function refreshCaptcha() {
  try {
    const res = await getCaptcha()
    captchaImage.value = res.body.captchaImage
    form.captchaKey = res.body.captchaKey
    form.captchaValue = ''
  } catch {
    ElMessage.error(t('publicRepair.captchaFailed'))
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!form.privacyAgreed) {
    ElMessage.warning(t('publicRepair.validation.privacyRequired'))
    return
  }

  loading.value = true
  try {
    const res = await submitPublicRepair({
      reporterName: form.reporterName,
      reporterPhone: form.reporterPhone,
      reporterEmail: form.reporterEmail || undefined,
      reportDescription: form.reportDescription,
      reportAddress: form.reportAddress,
      poleNumber: form.poleNumber || undefined,
      captchaKey: form.captchaKey,
      captchaValue: form.captchaValue,
      privacyAgreed: form.privacyAgreed,
    })
    resultTicketNo.value = res.body.ticketNumber
    resultMessage.value = res.body.message
    submitted.value = true
  } catch {
    ElMessage.error(t('publicRepair.submitFailed'))
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<template>
  <div class="public-page">
    <div class="public-card">
      <h1 class="public-title">{{ t('publicRepair.title') }}</h1>
      <p class="public-subtitle">{{ t('publicRepair.subtitle') }}</p>

      <!-- Success State -->
      <div v-if="submitted" class="success-panel">
        <div class="success-icon">✓</div>
        <h2>{{ t('publicRepair.submitSuccess') }}</h2>
        <div class="ticket-number">{{ resultTicketNo }}</div>
        <p class="success-message">{{ resultMessage }}</p>
        <router-link to="/public/repair/status" class="status-link">
          {{ t('publicRepair.goQueryStatus') }}
        </router-link>
      </div>

      <!-- Form -->
      <el-form v-else ref="formRef" :model="form" :rules="rules" label-position="top" size="large">

        <!-- Pole number (from QR code) -->
        <el-form-item v-if="form.poleNumber" :label="t('publicRepair.poleNumber')">
          <el-input v-model="form.poleNumber" disabled />
        </el-form-item>

        <el-divider>{{ t('publicRepair.sectionReporter') }}</el-divider>

        <el-form-item :label="t('publicRepair.reporterName')" prop="reporterName">
          <el-input v-model="form.reporterName" :placeholder="t('publicRepair.placeholder.name')" />
        </el-form-item>

        <el-form-item :label="t('publicRepair.reporterPhone')" prop="reporterPhone">
          <el-input v-model="form.reporterPhone" :placeholder="t('publicRepair.placeholder.phone')" />
        </el-form-item>

        <el-form-item :label="t('publicRepair.reporterEmail')">
          <el-input v-model="form.reporterEmail" :placeholder="t('publicRepair.placeholder.email')" />
        </el-form-item>

        <el-divider>{{ t('publicRepair.sectionContent') }}</el-divider>

        <el-form-item :label="t('publicRepair.reportAddress')" prop="reportAddress">
          <el-input v-model="form.reportAddress" :placeholder="t('publicRepair.placeholder.address')" />
        </el-form-item>

        <el-form-item :label="t('publicRepair.reportDescription')" prop="reportDescription">
          <el-input v-model="form.reportDescription" type="textarea" :rows="4" :placeholder="t('publicRepair.placeholder.description')" />
        </el-form-item>

        <el-divider />

        <!-- CAPTCHA -->
        <el-form-item :label="t('publicRepair.captcha')" prop="captchaValue">
          <div class="captcha-row">
            <el-input v-model="form.captchaValue" :placeholder="t('publicRepair.placeholder.captcha')" style="flex: 1" />
            <img v-if="captchaImage" :src="captchaImage" class="captcha-img" @click="refreshCaptcha" :title="t('publicRepair.refreshCaptcha')" />
          </div>
        </el-form-item>

        <!-- Privacy Agreement -->
        <el-form-item>
          <el-checkbox v-model="form.privacyAgreed">
            {{ t('publicRepair.privacyAgreement') }}
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="submit-btn" :loading="loading" @click="handleSubmit" :disabled="!form.privacyAgreed">
            {{ t('publicRepair.submit') }}
          </el-button>
        </el-form-item>

        <div class="query-link">
          <router-link to="/public/repair/status">{{ t('publicRepair.goQueryStatus') }}</router-link>
        </div>
      </el-form>
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
.captcha-row {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
}
.captcha-img {
  height: 40px;
  border-radius: 6px;
  cursor: pointer;
  border: 1px solid var(--bg-active, #333);
}
.submit-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 10px;
}
.query-link {
  text-align: center;
  margin-top: 12px;
}
.query-link a {
  color: #409eff;
  font-size: 14px;
  text-decoration: none;
}
.success-panel {
  text-align: center;
  padding: 32px 0;
}
.success-icon {
  width: 60px;
  height: 60px;
  background: rgba(95, 201, 146, 0.15);
  color: #5fc992;
  font-size: 32px;
  font-weight: 700;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}
.success-panel h2 {
  color: #5fc992;
  font-size: 20px;
  margin: 0 0 12px;
}
.ticket-number {
  font-size: 24px;
  font-weight: 700;
  color: #409eff;
  background: rgba(64, 158, 255, 0.1);
  padding: 12px 24px;
  border-radius: 10px;
  display: inline-block;
  margin-bottom: 12px;
}
.success-message {
  color: var(--text-secondary, #999);
  font-size: 14px;
  margin-bottom: 20px;
}
.status-link {
  color: #409eff;
  font-size: 14px;
  text-decoration: none;
}
</style>
