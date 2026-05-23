<script setup lang="ts">
interface SchemaField {
  key: string
  label: string
  type: string
  required?: boolean
  options?: string[]
}

const props = defineProps<{
  field: SchemaField
}>()

const emit = defineEmits<{
  'update:field': [value: SchemaField]
  remove: []
}>()

function update(patch: Partial<SchemaField>) {
  emit('update:field', { ...props.field, ...patch })
}
</script>

<template>
  <el-card shadow="hover" class="schema-field-card">
    <div class="card-header">
      <el-icon class="drag-handle" style="cursor: move;"><Rank /></el-icon>
      <span class="field-label">{{ field.label || field.key }}</span>
      <el-tag size="small" type="info">{{ field.type }}</el-tag>
      <el-button type="danger" text size="small" @click="emit('remove')">
        <el-icon><Delete /></el-icon>
      </el-button>
    </div>
    <el-form label-position="top" size="small">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="Key">
            <el-input :model-value="field.key" @update:model-value="update({ key: $event })" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Label">
            <el-input :model-value="field.label" @update:model-value="update({ label: $event })" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="Type">
            <el-select :model-value="field.type" @update:model-value="update({ type: $event })">
              <el-option label="Text" value="text" />
              <el-option label="Number" value="number" />
              <el-option label="Select" value="select" />
              <el-option label="Date" value="date" />
              <el-option label="Boolean" value="boolean" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="Required">
            <el-switch :model-value="field.required" @update:model-value="update({ required: $event })" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </el-card>
</template>

<style scoped>
.schema-field-card {
  margin-bottom: 12px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.field-label {
  flex: 1;
  font-weight: 500;
}
</style>
