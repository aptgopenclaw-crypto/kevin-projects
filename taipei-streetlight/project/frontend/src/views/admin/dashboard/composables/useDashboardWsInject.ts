import { inject, type InjectionKey } from 'vue'
import type { WidgetType } from '@/types/dashboard'
import type { ConnectionStatus } from './useDashboardWebSocket'

type WidgetCallback = (data: unknown) => void

export interface DashboardWsContext {
  status: import('vue').Ref<ConnectionStatus>
  onWidgetUpdate: (widgetType: WidgetType, callback: WidgetCallback) => () => void
}

export const DASHBOARD_WS_KEY: InjectionKey<DashboardWsContext> = Symbol('dashboardWs')

/**
 * Inject the dashboard WebSocket context provided by DashboardView.
 * Returns a safe fallback if used outside dashboard.
 */
export function useDashboardWsInject(): DashboardWsContext | undefined {
  return inject(DASHBOARD_WS_KEY, undefined)
}
