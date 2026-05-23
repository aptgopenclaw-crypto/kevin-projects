<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Trash2, Plus } from 'lucide-vue-next'
import { getDisposals, createDisposal } from '@/api/material'
import type { DisposalResponse, DisposalRequest, DisposalType } from '@/types/material'
import { formatDateTime } from '@/utils/datetime'

const { t } = useI18n()

const loading = ref(false)
const pageSize = ref(15)
const items = ref<DisposalResponse[]>([])
const pagination = ref({ page: 0, totalElements: 0, totalPages: 0 })

const dialogVisible = ref(false)
const dialogLoading = ref(false)
const formRef = ref<FormInstance>()
const form = ref<DisposalRequest>({
  materialSpecId: 0,
  quantity: 1,
  disposalType: 'SCRAP',
})

const rules = computed<FormRules>(() => ({
  materialSpecId: [{ required: true, message: t('material.errors.specRequired'), trigger: 'change' }],
  quantity: [{ required: true, message: t('material.errors.quantityRequired'), trigger: 'blur' }],
  disposalType: [{ required: true, message: t('material.errors.disposalTypeRequired'), trigger: 'change' }],
}))

const disposalTypeOptions: { value: DisposalType; label: string }[] = [
  { value: 'SCRAP', label: '報廢' },
  { value: 'RETURN_VENDOR', label: '退還廠商' },
  { value: 'DONATION', label: '捐贈' },
  { value: 'OTHER', label: '其他' },
]

const typeLabel = (dt: DisposalType) =>
  disposalTypeOptions.find(o => o.value === dt)?.label ?? dt

async function fetchData(page = 0) {
  loading.value = true
  try {
    const res = await getDisposals({ page, size: pageSize.value })
    items.value = res.body.content
    pagination.value = {
      page: res.body.page,
      totalElements: res.body.totalElements,
      totalPages: res.body.totalPages,
    }
  } catch {
    ElMessage.error(t('material.loadFailed'))
  } finally {
    loading.value = false
  }
}

function handlePageChange(p: number) {
  fetchData(p - 1)
}

function openCreate() {
  form.value = { materialSpecId: 0, quantity: 1, disposalType: 'SCRAP' }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  dialogLoading.value = true
  try {
    await createDisposal(form.value)
    ElMessage.success(t('material.disposalSuccess'))
    dialogVisible.value = false
    fetchData(pagination.value.page)
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    dialogLoading.value = false
  }
}

onMounted(() => fetchData())
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <div class="header-left">
        <Trash2 :size="22" />
        <h2>{{ t('material.disposalTitle') }}</h2>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="16" />
        {{ t('common.add') }}
      </el-button>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="specName" :label="t('material.specName')" min-width="200" />
      <el-table-column prop="quantity" :label="t('material.quantity')" width="100" align="right" />
      <el-table-column :label="t('material.disposalType')" width="120">
        <template #default="{ row }">{{ typeLabel(row.disposalType) }}</template>
      </el-table-column>
      <el-table-column prop="reason" :label="t('material.reason')" min-width="200" />
      <el-table-column prop="disposedBy" :label="t('material.disposedBy')" width="120" />
      <el-table-column :label="t('material.disposedAt')" width="170">
        <template #default="{ row }">{{ formatDateTime(row.disposedAt) }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination-bar">
      <el-pagination
        :current-page="pagination.page + 1"
        :page-size="pageSize"
        :total="pagination.totalElements"
        layout="total, prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="t('material.newDisposal')" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item :label="t('material.specId')" prop="materialSpecId">
          <el-input-number v-model="form.materialSpecId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('material.quantity')" prop="quantity">
          <el-input-number v-model="form.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="t('material.disposalType')" prop="disposalType">
          <el-select v-model="form.disposalType" style="width: 100%">
            <el-option v-for="opt in disposalTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('material.reason')">
          <el-input v-model="form.reason" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :loading="dialogLoading" @click="handleSubmit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container { padding: 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header-left { display: flex; align-items: center; gap: 8px; }
.pagination-bar { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
