/**
 * logistics/ — Sub-components for the Logistics partner dashboard.
 *
 *   utils.js               — Shared helpers (flags, status mapping, document lists, freight estimate)
 *   OverviewView.jsx       — Dashboard KPIs, quick actions, recent shipments
 *   AssignedOrdersView.jsx — Pending orders table with accept/reject and real API data
 *   ShipmentsView.jsx      — Shipment tracking, milestone update, horizontal progress bar
 *   LogisticsPlannerView.jsx — Route planner, document checklist, freight cost estimate
 *   ProfileView.jsx        — Company profile loaded from API
 */

export { default as OverviewView }         from './OverviewView';
export { default as AssignedOrdersView }   from './AssignedOrdersView';
export { default as ShipmentsView }        from './ShipmentsView';
export { default as ProfileView }          from './ProfileView';
