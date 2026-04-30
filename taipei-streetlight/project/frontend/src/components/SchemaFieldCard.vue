<script setup lang="ts">
import { computed } from 'vue'
import type { SchemaField } from '@/api/device'
import { useI18n } from 'vue-i18n'
import { GripVertical, Trash2 } from 'lucide-vue-next'

const { t } = useI18n()

const props = defineProps<{
  field: SchemaField
}>()

const emit = defineEmits<{
  (e: 'update:field', val: SchemaField): void
  (e: 'remove'): void
}>()

const localField = computed({
  get: () => props.field,
  set: (val) => emit('update:field', val),
})

function update(patch: Partial<SchemaField>) {
  emit('update:field', { ...props.field, ...patch })
}

const typeOptions = [
  { value: 'text', label: t('deviceTemplate.typeText') },
  { value: 'number', label: t('deviceTemplate.typeNumber') },
  { value: 'select', label: t('deviceTemplate.typeSelect') },
  { value: 'checkbox', label: t('deviceTemplate.typeCheckbox') },
  { value: 'date', label: t('deviceTemplate.typeDate') },
]

const optionsStr = computed({
  get: () => (props.field.options ?? []).join(', '),
  set: (val: string) => {
    update({ options: val.split(',').map(s => s.trim()).filter(Boolean) })
  },
})
</script>

<template>
  <div class="schema-field-card">
    <div class="drag-handle">
      <GripVertical :size="16" />
    </div>
    <div class="card-body">
      <div class="card-row">
        <el-input
          :model-value="localField.key"
          :placeholder="t('deviceTemplate.fieldKey')"
          size="small"
          style="width: 120px"
          @update:model-value="v => update({ key: v })"
        />
        <el-input
          :model-value="localField.title"
          :placeholder="t('deviceTemplate.fieldTitle')"
          size="small"
          style="width: 140px"
          @update:model-value="v => update({ title: v })"
        />
        <el-select
          :model-value="localField.type"
          size="small"
          style="width: 100px"
          @update:model-value="v => update({ type: v as SchemaField['type'] })"
        >
          <el-option v-for="opt in typeOptions" :key="opt.value" :value="opt.value" :label="opt.label" />
        </el-select>
        <el-checkbox
          :model-value="localField.required ?? false"
          @update:model-value="v => update({ required: !!v })"
        >
          {{ t('deviceTemplate.required') }}
        </el-checkbox>
        <el-button type="danger" :icon="Trash2" size="small" circle @click="emit('remove')" />
      </div>

      <!-- select options -->
      <div v-if="localField.type === 'select'" class="card-row extra">
        <span class="extra-label">{{ t('deviceTemplate.options') }}:</span>
        <el-input
          v-model="optionsStr"
          size="small"
          :placeholder="t('deviceTemplate.optionsPlaceholder')"
        />
      </div>

      <!-- number min/max -->
      <div v-if="localField.type === 'number'" class="card-row extra">
        <span class="extra-label">{{ t('deviceTemplate.range') }}:</span>
        <el-input-number
          :model-value="localField.minimum"
          size="small"
          :controls="false"
          :placeholder="t('deviceTemplate.min')"
          style="width: 90px"
          @update:model-value="v => update({ minimum: v ?? undefined })"
        />
        <span style="color: var(--el-text-color-secondary)">~</span>
        <el-input-number
          :model-value="localField.maximum"
          size="small"
          :controls="false"
          :placeholder="t('deviceTemplate.max')"
          style="width: 90px"
          @update:model-value="v => update({ maximum: v ?? undefined })"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.schema-field-card {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  background: var(--el-bg-color);
  margin-bottom: 8px;
}
.drag-handle {
  cursor: grab;
  padding: 4px 0;
  color: var(--el-text-color-secondary);
}
.card-body {
  flex: 1;
}
.card-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.card-row.extra {
  margin-top: 8px;
}
.extra-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  white-space: nowrap;
}
</style>
