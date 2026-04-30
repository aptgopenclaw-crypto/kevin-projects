<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getMaterialSpecs } from '@/api/material'
import type { MaterialSpecResponse, MaterialCategory } from '@/types/material'

const props = defineProps<{
  modelValue?: number
  category?: MaterialCategory
  placeholder?: string
  disabled?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: number | undefined]
  change: [spec: MaterialSpecResponse | undefined]
}>()

const { t } = useI18n()

const options = ref<MaterialSpecResponse[]>([])
const loading = ref(false)
const keyword = ref('')

async function fetchSpecs(query?: string) {
  loading.value = true
  try {
    const res = await getMaterialSpecs({
      category: props.category || undefined,
      keyword: query || undefined,
      size: 50,
    })
    options.value = res.body.content
  } catch {
    options.value = []
  } finally {
    loading.value = false
  }
}

function handleChange(val: number | undefined) {
  emit('update:modelValue', val)
  const spec = options.value.find(o => o.id === val)
  emit('change', spec)
}

function remoteSearch(query: string) {
  keyword.value = query
  fetchSpecs(query)
}

watch(() => props.category, () => fetchSpecs())

onMounted(() => fetchSpecs())
</script>

<template>
  <el-select
    :model-value="modelValue"
    :placeholder="placeholder || t('material.specName')"
    :disabled="disabled"
    :loading="loading"
    filterable
    remote
    :remote-method="remoteSearch"
    clearable
    style="width: 100%"
    @update:model-value="handleChange"
  >
    <el-option
      v-for="spec in options"
      :key="spec.id"
      :label="`${spec.specCode} - ${spec.specName}`"
      :value="spec.id"
    />
  </el-select>
</template>
