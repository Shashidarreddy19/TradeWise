/**
 * Exporter page view components — split from the main Exporter.jsx for maintainability.
 *
 * Structure:
 *   Exporter.jsx          — Parent orchestrator (state management, handlers, routing)
 *   exporter/
 *     OverviewView.jsx    — Dashboard overview tab (stats, quick actions, activity feed)
 *     ProductsView.jsx    — Products catalog table (search, filter, CRUD actions)
 *     OrdersView.jsx      — Orders & fulfillment (table, detail modal, shipment timeline)
 *     ProfileView.jsx     — Profile settings page
 *     index.js            — Barrel exports
 *
 * NOTE: The Market Analysis view (AnalysisView) remains inline in Exporter.jsx
 * due to its deep state coupling with 15+ analysis sub-views. It can be further
 * extracted once the analysis logic is refactored into a custom hook (useMarketAnalysis).
 */

export { default as OverviewView } from './OverviewView';
export { default as ProductsView } from './ProductsView';
export { default as AnalysisView } from './AnalysisView';
export { default as OrdersView } from './OrdersView';
export { default as ProfileView } from './ProfileView';
export { default as LogisticsPlannerView } from './LogisticsPlannerView';

