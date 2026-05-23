<script setup lang="ts">
interface WorkflowStepLog {
  id: number
  stepName: string
  action: string
  operator?: string
  comment?: string
  createdAt?: string
}

defineProps<{
  logs: WorkflowStepLog[]
}>()
</script>

<template>
  <el-steps direction="vertical" :active="logs.length" finish-status="success">
    <el-step
      v-for="log in logs"
      :key="log.id"
      :title="log.stepName"
      :description="`${log.action}${log.operator ? ' by ' + log.operator : ''}${log.comment ? ' — ' + log.comment : ''}`"
    />
  </el-steps>
  <el-empty v-if="!logs.length" description="No workflow logs" />
</template>
