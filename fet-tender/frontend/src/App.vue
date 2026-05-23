<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppSidebar from '@/components/AppSidebar.vue'
import AppTopBar from '@/components/AppTopBar.vue'
import IdleTimeoutDialog from '@/components/IdleTimeoutDialog.vue'
import { useLocaleStore } from '@/stores/localeStore'
import { useIdleTimeout } from '@/composables/useIdleTimeout'
import zhTW from 'element-plus/es/locale/lang/zh-tw'
import zhCN from 'element-plus/es/locale/lang/zh-cn'
import en from 'element-plus/es/locale/lang/en'

const route = useRoute()
const localeStore = useLocaleStore()
const { showWarning, remainingSeconds, continueSession } = useIdleTimeout()

const publicRoutes = ['/login', '/select-tenant']
const showSidebar = computed(() => !publicRoutes.includes(route.path))

const elLocale = computed(() => {
  if (localeStore.locale === 'zh-TW') return zhTW
  if (localeStore.locale === 'zh-CN') return zhCN
  return en
})
</script>

<template>
  <el-config-provider :locale="elLocale">
    <div v-if="showSidebar" class="app-layout">
      <AppSidebar />
      <main class="app-main">
        <AppTopBar />
        <router-view />
      </main>
    </div>
    <router-view v-else />
    <IdleTimeoutDialog
      :visible="showWarning"
      :remaining-seconds="remainingSeconds"
      @continue="continueSession"
    />
  </el-config-provider>
</template>

<style>
/* ─── Dark theme (default) ─────────────────────────── */
html, html.dark {
  --bg-base:           var(--bg-base);
  --bg-surface:        var(--bg-surface);
  --bg-active:         var(--bg-active);
  --border-divider:    var(--border-divider);
  --border-subtle:     var(--border-subtle);
  --border-medium:     var(--border-medium);
  --border-light:      var(--border-light);
  --border-strong:     var(--border-strong);
  --bg-hover:          var(--bg-hover);
  --bg-hover-subtle:   var(--bg-hover-subtle);
  --text-primary:      #f9f9f9;
  --text-heading:      #ffffff;
  --text-secondary:    #9c9c9d;
  --text-muted:        #6a6b6c;
  --text-label:        #cecece;
  --shadow-card:       rgb(27, 28, 30) 0px 0px 0px 1px, rgb(7, 8, 10) 0px 0px 0px 1px inset;
  --btn-primary-bg:    var(--btn-primary-bg);
  --btn-primary-hover: var(--btn-primary-hover);
  --btn-primary-text:  var(--btn-primary-text);
  color-scheme: dark;
}

/* ─── Light theme ───────────────────────────────────── */
html.light {
  --bg-base:           #f0f2f5;
  --bg-surface:        #ffffff;
  --bg-active:         #e8edf3;
  --border-divider:    #e4e6eb;
  --border-subtle:     rgba(0, 0, 0, 0.08);
  --border-medium:     rgba(0, 0, 0, 0.12);
  --border-light:      rgba(0, 0, 0, 0.1);
  --border-strong:     rgba(0, 0, 0, 0.18);
  --bg-hover:          rgba(0, 0, 0, 0.04);
  --bg-hover-subtle:   rgba(0, 0, 0, 0.03);
  --text-primary:      #1a1a1a;
  --text-heading:      #1a1a1a;
  --text-secondary:    #606266;
  --text-muted:        #909399;
  --text-label:        #474747;
  --shadow-card:       rgba(0, 0, 0, 0.06) 0px 2px 12px;
  --btn-primary-bg:    #1a1a1a;
  --btn-primary-hover: #333333;
  --btn-primary-text:  #ffffff;
  color-scheme: light;
}

html, body, #app {
  margin: 0;
  padding: 0;
  background: var(--bg-base);
  color: var(--text-primary);
  font-family: 'Inter', sans-serif;
  min-height: 100vh;
  transition: background-color 200ms ease, color 200ms ease;
}

.app-layout {
  display: flex;
  min-height: 100vh;
}

.app-main {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
}
</style>
