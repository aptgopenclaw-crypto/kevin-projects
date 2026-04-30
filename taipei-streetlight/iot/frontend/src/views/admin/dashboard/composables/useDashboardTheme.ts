import { inject, ref, type Ref, type InjectionKey } from 'vue'

export const ECHARTS_THEME_KEY: InjectionKey<Ref<string | undefined>> = Symbol('echartsTheme')

/** Inject the current ECharts theme name ('dark' | undefined) provided by DashboardView */
export function useEchartsTheme(): Ref<string | undefined> {
  return inject(ECHARTS_THEME_KEY, ref(undefined))
}
