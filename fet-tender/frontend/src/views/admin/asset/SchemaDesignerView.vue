<script setup lang="ts">
import { ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { getDeviceSchema, updateDeviceSchema, type SchemaField, type DeviceSchema } from '@/api/device'
import SchemaFieldCard from '@/components/SchemaFieldCard.vue'
import DynamicFieldEditor from '@/components/DynamicFieldEditor.vue'
import { ElMessage } from 'element-plus'
import { Plus } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const deviceTypeOptions = [
  { value: 'POLE', label: '燈桿' },
  { value: 'LUMINAIRE', label: '燈具' },
  { value: 'CONTROLLER', label: '控制器' },
  { value: 'PANEL_BOX', label: '分電箱' },
  { value: 'POWER_EQUIPMENT', label: '電力設備' },
  { value: 'ATTACHMENT', label: '附掛物' },
]

const selectedType = ref('POLE')
const fields = ref<SchemaField[]>([])
const loading = ref(false)
const saving = ref(false)
const previewAttrs = ref<Record<string, unknown>>({})

async function loadSchema() {
  loading.value = true
  try {
    const res = await getDeviceSchema(selectedType.value)
    fields.value = res.data?.fields ?? []
    previewAttrs.value = {}
  } catch {
    fields.value = []
  } finally {
    loading.value = false
  }
}

watch(selectedType, loadSchema, { immediate: true })

function addField() {
  fields.value.push({
    key: '',
    title: '',
    type: 'text',
    required: false,
  })
}

function removeField(index: number) {
  fields.value.splice(index, 1)
}

function updateField(index: number, updated: SchemaField) {
  fields.value[index] = updated
}

async function saveSchema() {
  // validate: all fields must have key
  const invalid = fields.value.some(f => !f.key.trim())
  if (invalid) {
    ElMessage.warning(t('deviceTemplate.keyRequired'))
    return
  }
  // check duplicate keys
  const keys = fields.value.map(f => f.key.trim())
  if (new Set(keys).size !== keys.length) {
    ElMessage.warning(t('deviceTemplate.keyDuplicate'))
    return
  }

  saving.value = true
  try {
    const schema: DeviceSchema = { fields: fields.value }
    await updateDeviceSchema(selectedType.value, schema)
    ElMessage.success(t('deviceTemplate.saveSuccess'))
  } catch {
    ElMessage.error(t('common.operationFailed'))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="schema-designer-page">
    <div class="page-header">
      <h2>{{ t('deviceTemplate.schemaDesigner') }}</h2>
      <div class="header-actions">
        <el-select v-model="selectedType" style="width: 160px">
          <el-option
            v-for="opt in deviceTypeOptions"
            :key="opt.value"
            :value="opt.value"
            :label="opt.label"
          />
        </el-select>
        <el-button type="primary" :loading="saving" @click="saveSchema">
          {{ t('common.save') }}
        </el-button>
      </div>
    </div>

    <div class="designer-layout">
      <!-- Left: field list -->
      <div class="designer-panel">
        <div class="panel-title">
          {{ t('deviceTemplate.fieldList') }}
          <el-button :icon="Plus" size="small" @click="addField">
            {{ t('deviceTemplate.addField') }}
          </el-button>
        </div>

        <div v-loading="loading" class="field-list">
          <draggable
            v-model="fields"
            item-key="key"
            handle=".drag-handle"
            animation="200"
            ghost-class="ghost"
          >
            <template #item="{ element, index }">
              <SchemaFieldCard
                :field="element"
                @update:field="(val: SchemaField) => updateField(index, val)"
                @remove="removeField(index)"
              />
            </template>
          </draggable>

          <div v-if="!loading && fields.length === 0" class="empty-hint">
            {{ t('deviceTemplate.noFields') }}
          </div>
        </div>
      </div>

      <!-- Right: preview -->
      <div class="designer-panel preview-panel">
        <div class="panel-title">{{ t('deviceTemplate.preview') }}</div>
        <el-form label-position="top" class="preview-form">
          <DynamicFieldEditor
            :schema="{ fields }"
            v-model="previewAttrs"
          />
        </el-form>
        <div v-if="fields.length === 0" class="empty-hint">
          {{ t('deviceTemplate.noFieldsPreview') }}
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.schema-designer-page {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header h2 {
  margin: 0;
  font-size: 18px;
  color: var(--el-text-color-primary);
}
.header-actions {
  display: flex;
  gap: 12px;
}
.designer-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  min-height: 400px;
}
.designer-panel {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 16px;
  background: var(--el-bg-color-overlay);
}
.panel-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--el-text-color-primary);
}
.field-list {
  min-height: 200px;
}
.empty-hint {
  text-align: center;
  padding: 40px 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.preview-form {
  max-width: 100%;
}
.ghost {
  opacity: 0.4;
  border: 2px dashed var(--el-color-primary);
}
</style>
