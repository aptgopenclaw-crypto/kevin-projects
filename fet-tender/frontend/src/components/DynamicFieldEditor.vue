<script setup lang="ts">
import { computed } from 'vue'

interface SchemaField {
  key: string
  label: string
  type: string
  required?: boolean
  options?: string[]
}

const props = defineProps<{
  schema: { fields: SchemaField[] }
  modelValue: Record<string, unknown>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, unknown>]
}>()

const formData = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

function updateField(key: string, value: unknown) {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}
</script>

<template>
  <el-form label-position="top">
    <el-form-item
      v-for="field in schema.fields"
      :key="field.key"
      :label="field.label"
      :required="field.required"
    >
      <el-select
        v-if="field.type === 'select'"
        :model-value="(formData[field.key] as string)"
        @update:model-value="updateField(field.key, $event)"
      >
        <el-option
          v-for="opt in field.options"
          :key="opt"
          :label="opt"
          :value="opt"
        />
      </el-select>
      <el-input-number
        v-else-if="field.type === 'number'"
        :model-value="(formData[field.key] as number)"
        @update:model-value="updateField(field.key, $event)"
      />
      <el-input
        v-else
        :model-value="(formData[field.key] as string)"
        @update:model-value="updateField(field.key, $event)"
      />
    </el-form-item>
    <el-empty v-if="!schema.fields.length" description="No fields defined" />
  </el-form>
</template>
