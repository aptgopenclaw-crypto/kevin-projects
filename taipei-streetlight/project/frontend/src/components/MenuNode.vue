<script setup lang="ts">
import { useRouter } from 'vue-router'
import type { UserMenuDto } from '@/types/rbac'
import type { Component } from 'vue'
import {
  Users,
  Settings,
  FileText,
  Monitor,
  Shield,
  LayoutDashboard,
  Menu as MenuIcon,
  Building2,
  Activity,
  Wrench,
  Eye,
  FolderOpen,
} from 'lucide-vue-next'

defineProps<{
  menu: UserMenuDto
}>()

const router = useRouter()

const iconMap: Record<string, Component> = {
  user: Users,
  users: Users,
  setting: Settings,
  settings: Settings,
  file: FileText,
  monitor: Monitor,
  shield: Shield,
  dashboard: LayoutDashboard,
  menu: MenuIcon,
  building: Building2,
  activity: Activity,
  audit: FileText,
  wrench: Wrench,
  device: Monitor,
  eye: Eye,
  folder: FolderOpen,
}

function getIcon(iconName: string | null): Component {
  if (!iconName) return FolderOpen
  return iconMap[iconName.toLowerCase()] ?? FolderOpen
}

function handleClick(menu: UserMenuDto) {
  if (menu.routeName) {
    router.push({ name: menu.routeName })
  } else if (menu.routePath) {
    router.push(menu.routePath)
  }
}
</script>

<template>
  <!-- DIRECTORY with children → el-sub-menu -->
  <template v-if="menu.children?.length">
    <el-sub-menu :index="menu.routePath ?? `dir-${menu.menuId}`">
      <template #title>
        <el-icon><component :is="getIcon(menu.icon)" /></el-icon>
        <span>{{ menu.name }}</span>
      </template>
      <MenuNode
        v-for="child in [...menu.children].sort((a, b) => a.sortOrder - b.sortOrder)"
        :key="child.menuId"
        :menu="child"
      />
    </el-sub-menu>
  </template>
  <!-- PAGE leaf node → el-menu-item -->
  <template v-else-if="menu.menuType !== 'BUTTON'">
    <el-menu-item :index="menu.routePath ?? `item-${menu.menuId}`" @click="handleClick(menu)">
      <el-icon v-if="menu.icon"><component :is="getIcon(menu.icon)" /></el-icon>
      <template #title>
        <span>{{ menu.name }}</span>
      </template>
    </el-menu-item>
  </template>
</template>
