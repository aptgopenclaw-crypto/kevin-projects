<script setup lang="ts">
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import { updateUser } from '@/api/user'
import { listRoles } from '@/api/rbac'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Phone, Shield } from 'lucide-vue-next'
import DeptTreeSelector from '@/components/DeptTreeSelector.vue'
import type { UserListItemDto } from '@/types/user'
import type { RoleDto } from '@/types/rbac'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const pageLoading = ref(true)
const userId = route.params.userId as string

const roles = ref<RoleDto[]>([])

const form = reactive({
  displayName: '',
  phone: '',
  roleId: '',
  deptId: null as number | null,
})

const userBasic = ref<UserListItemDto | null>(null)

const rules: FormRules = {
  displayName: [
    { required: true, message: () => t('user.edit.errors.displayNameRequired'), trigger: 'blur' },
  ],
  roleId: [
    { required: true, message: () => t('user.edit.errors.roleRequired'), trigger: 'change' },
  ],
}

const errorCodeMessages = computed<Record<string, string>>(() => ({
  '10010': t('user.edit.errors.10010'),
  '20005': t('user.edit.errors.20005'),
}))

onMounted(async () => {
  try {
    // Load roles list
    const rolesRes = await listRoles()
    roles.value = rolesRes.body.filter((r: RoleDto) => r.enabled)

    // Load user list to find this user's data
    await userStore.fetchUserList({ page: 0, size: 9999, keyword: undefined })
    const found = userStore.userList.find(u => u.userId === userId)
    if (found) {
      userBasic.value = found
      form.displayName = found.displayName || ''
      form.phone = found.phone || ''
      form.roleId = found.roleId || ''
      form.deptId = found.deptId ?? null
    } else {
      ElMessage.error(t('user.edit.userNotFound'))
      router.push('/admin/users')
      return
    }
  } catch {
    ElMessage.error(t('user.edit.loadFailed'))
  } finally {
    pageLoading.value = false
  }
})

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await updateUser(userId, {
      displayName: form.displayName,
      phone: form.phone,
      roleId: form.roleId,
      deptId: form.deptId,
    })
    ElMessage.success(t('user.edit.updatedSuccess'))
  } catch (err: unknown) {
    const error = err as { response?: { data?: { errorCode?: string } } }
    const errorCode = error?.response?.data?.errorCode
    const msg = (errorCode && errorCodeMessages.value[errorCode]) || t('user.edit.updateFailed')
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/admin/users')
}
</script>

<template>
  <div class="page-container">
    <div class="page-content" v-loading="pageLoading">
      <!-- Basic Info Card -->
      <div class="page-card">
        <div class="page-header">
          <h1 class="page-title">{{ t('user.edit.title') }}</h1>
          <p class="page-subtitle" v-if="userBasic">{{ userBasic.email }}</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          size="large"
          @submit.prevent="handleSubmit"
        >
          <el-form-item :label="t('user.edit.displayNameLabel')" prop="displayName">
            <el-input v-model="form.displayName" :placeholder="t('user.edit.displayNamePlaceholder')">
              <template #prefix>
                <User :size="18" class="input-icon" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item :label="t('user.edit.phoneLabel')">
            <el-input v-model="form.phone" :placeholder="t('user.edit.phonePlaceholder')">
              <template #prefix>
                <Phone :size="18" class="input-icon" />
              </template>
            </el-input>
          </el-form-item>

          <el-form-item :label="t('user.edit.roleLabel')" prop="roleId">
            <el-select v-model="form.roleId" :placeholder="t('user.edit.rolePlaceholder')" class="full-width">
              <template #prefix>
                <Shield :size="18" class="input-icon" />
              </template>
              <el-option
                v-for="role in roles"
                :key="role.roleId"
                :label="role.name"
                :value="role.roleId"
              />
            </el-select>
          </el-form-item>

          <el-form-item :label="t('user.edit.deptLabel')">
            <DeptTreeSelector v-model="form.deptId" :placeholder="t('user.edit.deptPlaceholder')" />
          </el-form-item>

          <div class="button-row">
            <el-button class="cancel-btn" @click="goBack">{{ t('user.edit.backBtn') }}</el-button>
            <el-button
              class="submit-btn"
              :loading="loading"
              native-type="submit"
            >
              {{ t('user.edit.submitBtn') }}
            </el-button>
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  display: flex;
  justify-content: center;
  padding: 40px 24px;
  min-height: 100vh;
  background-color: var(--bg-base);
}

.page-content {
  width: 640px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-card {
  background-color: var(--bg-surface);
  border: 1px solid var(--border-subtle);
  border-radius: 12px;
  padding: 40px 32px;
  box-shadow: var(--shadow-card);
}

.page-header {
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

.input-icon {
  color: var(--text-muted);
}

.button-row {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
}

.cancel-btn {
  background: transparent;
  color: var(--text-secondary);
  border: none;
  border-radius: 6px;
  padding: 8px 16px;
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.cancel-btn:hover {
  opacity: 0.6;
  color: var(--text-primary);
}

.submit-btn {
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

/* Element Plus dark overrides */
:deep(.el-form-item__label) {
  color: var(--text-label);
  font-family: 'Inter', sans-serif;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0.2px;
}

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

.full-width {
  width: 100%;
}

:deep(.el-select .el-input__wrapper) {
  background-color: var(--bg-base);
  border: 1px solid var(--border-medium);
  border-radius: 8px;
  box-shadow: none;
}

:deep(.el-select .el-input__wrapper:hover) {
  border-color: var(--border-strong);
}
</style>
