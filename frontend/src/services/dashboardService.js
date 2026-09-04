/**
 * dashboardService.js — Dashboard statistics API.
 *
 * Backend: DashboardController @ /api/dashboard
 */

import { get } from './httpClient';

export const dashboardApi = {
  /**
   * GET /api/dashboard/exporter — exporter dashboard stats
   */
  getExporterDashboard() {
    return get('/dashboard/exporter');
  },

  /**
   * GET /api/dashboard/logistics — logistics partner dashboard stats
   */
  getLogisticsDashboard() {
    return get('/dashboard/logistics');
  },
};
