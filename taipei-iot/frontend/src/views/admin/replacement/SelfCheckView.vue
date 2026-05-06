<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ClipboardCheck } from 'lucide-vue-next'
import { getReplacementItems, selfCheckReplacementOrder } from '@/api/replacement'
import type { ReplacementItemResponse, SelfCheckItemRequest } from '@/types/replacement'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

const loading = ref(false)
const submitting = ref(false)
const items = ref<ReplacementItemResponse[]>([])
const checkForms = ref<Record<number, { deviceCode: string; notes: string }>>({})

const pendingItems = computed(() =>
  items.value.filter(i => i.status === 'PENDING' || i.status === 'IN_PROGRESS'),
)

const allFilled = computed(() =>
  pendingItems.value.every(i => checkForms.value[i.id]?.deviceCode?.trim()),
)

const fetchItems = async () => {
  loading.value = true
  try {
    const res = await getReplacementItems(orderId)
    items.value = res.body
    // Initialize forms for pending items
    for (const item of res.body) {
      if ((item.status === 'PENDING' || item.status === 'IN_PROGRESS') && !checkForms.value[item.id]) {
        checkForms.value[item.id] = { deviceCode: '', notes: '' }
      }
    }
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!allFilled.value) return
  submitting.value = true
  try {
    const checkItems: SelfCheckItemRequest[] = pendingItems.value.map(i => ({
      itemId: i.id,
      deviceCode: checkForms.value[i.id].deviceCode.trim(),
      notes: checkForms.value[i.id].notes || undefined,
    }))
    await selfCheckReplacementOrder(orderId, { items: checkItems })
    ElMessage.success(t('replacement.operationSuccess'))
    router.back()
  } finally {
    submitting.value = false
  }
}

onMounted(() => fetchItems())
</script>

<template>
  <div class="page-container" v-loading="loading">
    <!-- Header -->
    <div class="page-header">
      <div class="header-left">
        <el-button class="back-btn" @click="router.back()"><ArrowLeft :size="16" /></el-button>
        <div class="header-icon"><ClipboardCheck :size="20" /></div>
        <div>
          <h2 class="header-title">{{ t('replacement.selfCheckTitle') }}</h2>
          <p class="header-subtitle">{{ t('replacement.selfCheckSubtitle') }}</p>
        </div>
      </div>
    </div>

    <!-- No pending items -->
    <div v-if="!loading && pendingItems.length === 0" class="empty-state">
      {{ t('replacement.noItemsToCheck') }}
    </div>

    <!-- Check items -->
    <div v-for="(item, idx) in pendingItems" :key="item.id" class="check-card">
      <div class="check-card-header">
        <span class="check-index">{{ t('replacement.selfCheckItemIndex', { index: idx + 1 }) }}</span>
        <span class="check-device">{{ t('replacement.parentDevice') }}: {{ item.parentDeviceCode || '#' + item.parentDeviceId }}</span>
        <span class="check-device">{{ t('replacement.oldDevice') }}: {{ item.oldDeviceCode || '#' + item.oldDeviceId }}</span>
      </div>

      <div class="check-card-body">
        <div class="spec-row" v-if="item.beforeDeviceType">
          <span class="spec-label">{{ t('replacement.beforeSpec') }}</span>
          <span class="spec-value">{{ item.beforeDeviceType }}</span>
        </div>
        <div class="spec-row" v-if="item.afterDeviceType">
          <span class="spec-label">{{ t('replacement.afterSpec') }}</span>
          <span class="spec-value">{{ item.afterDeviceType }}</span>
        </div>

        <el-form label-position="top" class="check-form">
          <el-form-item :label="t('replacement.selfCheckDeviceCode')" required>
            <el-input
              v-model="checkForms[item.id].deviceCode"
              :placeholder="t('replacement.selfCheckDeviceCodeHint')"
            />
          </el-form-item>
          <el-form-item :label="t('replacement.selfCheckNotes')">
            <el-input v-model="checkForms[item.id].notes" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <!-- Submit -->
    <div v-if="pendingItems.length > 0" class="submit-bar">
      <el-button class="submit-btn" :disabled="!allFilled" :loading="submitting" @click="handleSubmit">
        {{ t('replacement.selfCheckSubmit') }}
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.page-container { padding: 24px; height: 100%; overflow-y: auto; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-left { display: flex; align-items: center; gap: 14px; }
.back-btn { width: 36px; height: 36px; border-radius: 8px; padding: 0; display: flex; align-items: center; justify-content: center; }
.header-icon {
  width: 40px; height: 40px;
  background: rgba(230, 162, 60, 0.1);
  border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: #e6a23c;
}
.header-title { font-size: 20px; font-weight: 700; color: var(--text-heading); margin: 0; }
.header-subtitle { font-size: 13px; color: var(--text-secondary); margin: 4px 0 0; }
.empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--text-secondary);
  font-size: 14px;
}
.check-card {
  border: 1px solid var(--bg-active);
  border-radius: 12px;
  background: var(--bg-surface);
  margin-bottom: 16px;
  overflow: hidden;
}
.check-card-header {
  display: flex;
  gap: 16px;
  padding: 12px 20px;
  background: var(--bg-base);
  border-bottom: 1px solid var(--bg-active);
  font-size: 13px;
  align-items: center;
}
.check-index { font-weight: 700; color: var(--text-heading); }
.check-device { color: var(--text-secondary); }
.check-card-body { padding: 20px; }
.spec-row { display: flex; gap: 12px; margin-bottom: 8px; font-size: 13px; }
.spec-label { color: var(--text-secondary); min-width: 80px; }
.spec-value { color: var(--text-heading); font-weight: 500; }
.check-form { margin-top: 16px; }
.submit-bar { display: flex; justify-content: flex-end; margin-top: 20px; }
</style>
