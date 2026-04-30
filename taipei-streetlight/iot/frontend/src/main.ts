import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { BarChart, PieChart, LineChart } from 'echarts/charts'
import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/themeStore'
import { useLocaleStore } from './stores/localeStore'
import i18n from './i18n'

// Register ECharts renderer + components globally (required by vue-echarts 8 + echarts 6)
use([CanvasRenderer, TooltipComponent, LegendComponent, GridComponent, BarChart, PieChart, LineChart])

const app = createApp(App)
const pinia = createPinia()
app.use(pinia)
app.use(router)
app.use(ElementPlus)
app.use(i18n)

// Initialize theme and locale before mounting
useThemeStore(pinia)
useLocaleStore(pinia)

app.mount('#app')
