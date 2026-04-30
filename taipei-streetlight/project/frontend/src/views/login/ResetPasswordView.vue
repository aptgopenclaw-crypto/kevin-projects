<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Lock, ArrowLeft, Check, X } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { resetPassword } from '@/api/auth'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)
const token = ref('')

onMounted(() => {
  token.value = (route.query.token as string) || ''
  if (!token.value) {
    ElMessage.error(t('resetPassword.errors.missingToken'))
    router.push('/login')
  }
})

const form = reactive({
  newPassword: '',
  confirmPassword: '',
})

const passwordChecks = computed(() => [
  { label: t('resetPassword.rules.minLength'), pass: form.newPassword.length >= 8 },
  { label: t('resetPassword.rules.uppercase'), pass: /[A-Z]/.test(form.newPassword) },
  { label: t('resetPassword.rules.lowercase'), pass: /[a-z]/.test(form.newPassword) },
  { label: t('resetPassword.rules.digit'), pass: /\d/.test(form.newPassword) },
])

const allChecksPassed = computed(() => passwordChecks.value.every(c => c.pass))

const rules = computed<FormRules>(() => ({
  newPassword: [
    { required: true, message: t('resetPassword.errors.passwordRequired'), trigger: 'blur' },
    {
      validator: (_rule: unknown, _value: string, callback: (err?: Error) => void) => {
        if (!allChecksPassed.value) {
          callback(new Error(t('resetPassword.errors.passwordWeak')))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: t('resetPassword.errors.confirmRequired'), trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
        if (value !== form.newPassword) {
          callback(new Error(t('resetPassword.errors.passwordMismatch')))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}))

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await resetPassword({
      token: token.value,
      newPassword: form.newPassword,
    })
    ElMessage.success(t('resetPassword.success'))
    router.push('/login')
  } catch (err: unknown) {
    const error = err as { response?: { data?: { errorCode?: string } } }
    const errorCode = error?.response?.data?.errorCode
    const msg = errorCode
      ? t(`resetPassword.errors.${errorCode}`, t('resetPassword.errors.resetFailed'))
      : t('resetPassword.errors.resetFailed')
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <h1 class="login-title">{{ t('resetPassword.title') }}</h1>
        <p class="login-subtitle">{{ t('resetPassword.subtitle') }}</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        size="large"
        @submit.prevent="handleSubmit"
      >
        <el-form-item prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            :placeholder="t('resetPassword.newPasswordPlaceholder')"
            show-password
          >
            <template #prefix>
              <Lock :size="18" class="input-icon" />
            </template>
          </el-input>
        </el-form-item>

        <!-- Password strength indicators -->
        <div v-if="form.newPassword" class="password-checks">
          <div
            v-for="check in passwordChecks"
            :key="check.label"
            class="check-item"
            :class="{ pass: check.pass }"
          >
            <component :is="check.pass ? Check : X" :size="14" />
            <span>{{ check.label }}</span>
          </div>
        </div>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            :placeholder="t('resetPassword.confirmPasswordPlaceholder')"
            show-password
          >
            <template #prefix>
              <Lock :size="18" class="input-icon" />
            </template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button
            class="submit-btn"
            :loading="loading"
            native-type="submit"
          >
            {{ t('resetPassword.submit') }}
          </el-button>
        </el-form-item>

        <div class="back-row">
          <a class="back-link" @click="router.push('/login')">
            <ArrowLeft :size="14" style="margin-right: 4px; vertical-align: middle;" />
            {{ t('resetPassword.backToLogin') }}
          </a>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: var(--bg-base);
}

.login-card {
  width: 420px;
  background-color: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 40px 32px;
  box-shadow: var(--shadow-card);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-title {
  font-family: 'Inter', sans-serif;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.15;
  color: var(--text-heading);
  margin: 0 0 8px 0;
}

.login-subtitle {
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
  line-height: 1.6;
  letter-spacing: 0.2px;
  color: var(--text-secondary);
  margin: 0;
}

.input-icon {
  color: var(--text-muted);
}

.password-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 16px;
  margin-bottom: 16px;
  padding: 0 2px;
}

.check-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--text-muted);
}

.check-item.pass {
  color: #67c23a;
}

.submit-btn {
  width: 100%;
  background: var(--btn-primary-bg);
  color: var(--btn-primary-text);
  border: none;
  border-radius: 86px;
  padding: 8px 24px;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.3px;
  transition: opacity 150ms ease;
}

.submit-btn:hover {
  background: var(--btn-primary-hover);
  color: var(--btn-primary-text);
}

.back-row {
  text-align: center;
  margin-top: 8px;
}

.back-link {
  font-size: 13px;
  color: var(--text-link);
  cursor: pointer;
  text-decoration: none;
}

.back-link:hover {
  text-decoration: underline;
}

/* Element Plus dark overrides */
:deep(.el-input__wrapper) {
  background-color: var(--bg-base);
  border: 1px solid var(--border-medium);
  border-radius: 8px;
  box-shadow: none;
}

:deep(.el-input__wrapper:hover) {
  border-color: var(--border-strong);
}

:deep(.el-input__wrapper.is-focus) {
  border-color: rgba(85, 179, 255, 0.5);
  box-shadow: 0 0 0 3px rgba(85, 179, 255, 0.15);
}

:deep(.el-input__inner) {
  color: var(--text-primary);
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.2px;
}

:deep(.el-input__inner::placeholder) {
  color: var(--text-muted);
}

:deep(.el-form-item__error) {
  color: #FF6363;
}
</style>
