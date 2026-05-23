<script setup lang="ts">
defineProps<{
  oldDeviceCode: string
  newDeviceCode: string
  beforeDeviceType: string
  afterDeviceType: string
  beforeSpec: string
  afterSpec: string
  parentDeviceCode: string
}>()
</script>

<template>
  <div class="spec-comparison">
    <el-descriptions :column="2" border>
      <el-descriptions-item label="Parent Device">{{ parentDeviceCode }}</el-descriptions-item>
      <el-descriptions-item label="" />
      <el-descriptions-item label="Old Device Code">{{ oldDeviceCode }}</el-descriptions-item>
      <el-descriptions-item label="New Device Code">{{ newDeviceCode }}</el-descriptions-item>
      <el-descriptions-item label="Before Device Type">{{ beforeDeviceType }}</el-descriptions-item>
      <el-descriptions-item label="After Device Type">{{ afterDeviceType }}</el-descriptions-item>
      <el-descriptions-item label="Before Spec">
        <pre class="spec-text">{{ beforeSpec }}</pre>
      </el-descriptions-item>
      <el-descriptions-item label="After Spec">
        <pre class="spec-text">{{ afterSpec }}</pre>
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<style scoped>
.spec-text {
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
  font-size: 13px;
}
</style>
