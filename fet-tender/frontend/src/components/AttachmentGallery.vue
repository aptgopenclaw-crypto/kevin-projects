<script setup lang="ts">
defineProps<{
  attachments: Array<{ id: number; fileName: string; url: string }>
}>()
</script>

<template>
  <div class="attachment-gallery">
    <el-empty v-if="!attachments.length" description="No attachments" />
    <div v-else class="gallery-grid">
      <div v-for="att in attachments" :key="att.id" class="gallery-item">
        <el-link :href="att.url" target="_blank" type="primary">
          {{ att.fileName }}
        </el-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
}
.gallery-item {
  padding: 8px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
}
</style>
