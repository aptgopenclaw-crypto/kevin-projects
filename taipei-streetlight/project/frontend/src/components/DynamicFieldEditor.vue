<script setup lang="ts">
import { computed } from 'vue'
import type { SchemaField } from '@/api/device'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

const props = defineProps<{
  schema: { fields: SchemaField[] } | null
  modelValue: Record<string, unknown>
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: Record<string, unknown>): void
}>()

const fields = computed(() => props.schema?.fields ?? [])

function updateField(key: string, value: unknown) {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}
</script>

<template>
  <div v-if="fields.length" class="dynamic-field-editor">
    <div class="section-label">{{ t('deviceTemplate.dynamicFields') }}</div>
    <div class="field-grid">
      <el-form-item
        v-for="field in fields"
        :key="field.key"
        :label="field.title"
        :required="field.required"
        :class="{ 'full-width': field.type === 'checkbox' }"
      >
        <!-- text -->
        <el-input
          v-if="field.type === 'text'"
          :model-value="(modelValue[field.key] as string) ?? ''"
          :placeholder="field.placeholder"
          :disabled="disabled"
          @update:model-value="v => updateField(field.key, v)"
        />

        <!-- number -->
        <el-input-number
          v-else-if="field.type === 'number'"
          :model-value="(modelValue[field.key] as number) ?? undefined"
          :min="field.minimum"
          :max="field.maximum"
          :controls="false"
          :disabled="disabled"
          style="width: 100%"
          @update:model-value="v => updateField(field.key, v)"
        />

        <!-- select -->
        <el-select
          v-else-if="field.type === 'select'"
          :model-value="(modelValue[field.key] as string) ?? ''"
          :placeholder="t('common.pleaseSelect')"
          :disabled="disabled"
          style="width: 100%"
          @update:model-value="v => updateField(field.key, v)"
        >
          <el-option
            v-for="opt in field.options"
            :key="opt"
            :label="opt"
            :value="opt"
          />
        </el-select>

        <!-- checkbox -->
        <el-checkbox
          v-else-if="field.type === 'checkbox'"
          :model-value="(modelValue[field.key] as boolean) ?? false"
          :disabled="disabled"
          @update:model-value="v => updateField(field.key, v)"
        >
          {{ field.title }}
        </el-checkbox>

        <!-- date -->
        <el-date-picker
          v-else-if="field.type === 'date'"
          :model-value="(modelValue[field.key] as string) ?? ''"
          type="date"
          value-format="YYYY-MM-DD"
          :placeholder="t('common.pleaseSelect')"
          :disabled="disabled"
          style="width: 100%"
          @update:model-value="v => updateField(field.key, v)"
        />
      </el-form-item>
    </div>
  </div>
</template>

<style scoped>
.dynamic-field-editor {
  margin-top: 8px;
}
.section-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin-bottom: 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding-bottom: 6px;
}
.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}
.field-grid .full-width {
  grid-column: 1 / -1;
}
</style>
