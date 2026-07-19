import { adminHeaders, api } from "@/shared/api";
import type {
  DailyRevenue,
  DashboardGranularity,
  DashboardOverview,
  PeriodSalesSummary,
  RefundStats,
  RevenueBreakdown,
} from "@/shared/types";

interface DashboardRange {
  from: string;
  to: string;
}

export interface DashboardSnapshot {
  overview: DashboardOverview;
  revenueBreakdown: RevenueBreakdown;
  refundStats: RefundStats;
  dailyRevenue: DailyRevenue[];
}

export async function fetchDashboardSnapshot(
  adminKey: string,
  range: DashboardRange,
): Promise<DashboardSnapshot> {
  const options = {
    headers: adminHeaders(adminKey),
    params: { from: range.from, to: range.to },
  };
  const [overview, revenueBreakdown, refundStats, dailyRevenue] = await Promise.all([
    api<DashboardOverview>("/admin/dashboard/overview", options),
    api<RevenueBreakdown>("/admin/dashboard/revenue-breakdown", options),
    api<RefundStats>("/admin/dashboard/refunds", options),
    api<DailyRevenue[]>("/admin/dashboard/daily-revenue", options),
  ]);

  return { overview, revenueBreakdown, refundStats, dailyRevenue };
}

export function fetchSalesSummary(
  adminKey: string,
  range: DashboardRange,
  granularity: DashboardGranularity,
): Promise<PeriodSalesSummary[]> {
  return api<PeriodSalesSummary[]>("/admin/dashboard/sales-summary", {
    headers: adminHeaders(adminKey),
    params: { ...range, granularity },
  });
}
