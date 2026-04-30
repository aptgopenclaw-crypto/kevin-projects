-- ============================================================
-- V62: KPI 選單 — 修正 component 路徑 (對齊實際 Vue 檔名)
-- ============================================================

UPDATE menus SET component = 'views/admin/kpi/IndicatorView.vue'
WHERE route_name = 'KpiIndicators' AND component = 'views/admin/kpi/KpiIndicatorView.vue';

UPDATE menus SET component = 'views/admin/kpi/DataView.vue'
WHERE route_name = 'KpiData' AND component = 'views/admin/kpi/KpiDataView.vue';

UPDATE menus SET component = 'views/admin/kpi/CalculateView.vue'
WHERE route_name = 'KpiCalculate' AND component = 'views/admin/kpi/KpiCalculateView.vue';

UPDATE menus SET component = 'views/admin/kpi/ReportView.vue'
WHERE route_name = 'KpiReports' AND component = 'views/admin/kpi/KpiReportView.vue';

UPDATE menus SET component = 'views/admin/kpi/PeriodView.vue'
WHERE route_name = 'KpiPeriods' AND component = 'views/admin/kpi/KpiPeriodView.vue';
