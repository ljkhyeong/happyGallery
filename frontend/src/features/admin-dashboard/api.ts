import {
  dailyRevenue as getDailyRevenue,
  orderStatusDistribution as getOrderStatusDistribution,
  overview as getOverview,
  refundStats as getRefundStats,
  revenueBreakdown as getRevenueBreakdown,
  salesSummary as getSalesSummary,
  slotUtilization as getSlotUtilization,
  topProducts as getTopProducts,
  type DailyRevenueResponse,
  type DashboardOverviewResponse,
  type PeriodSalesSummaryResponse,
  type RefundStatsResponse,
  type RevenueBreakdownResponse,
  type SalesSummaryGranularity,
  type SlotUtilizationResponse,
  type StatusCountResponse,
  type TopProductResponse,
} from "@/generated/api/adminDashboard";
import { adminHeaders } from "@/shared/api";

interface DashboardRange {
  from: string;
  to: string;
}

export type DashboardGranularity = SalesSummaryGranularity;
export type { DailyRevenueResponse };

export interface DashboardSnapshot {
  overview: DashboardOverviewResponse;
  revenueBreakdown: RevenueBreakdownResponse;
  refundStats: RefundStatsResponse;
  dailyRevenue: DailyRevenueResponse[];
  orderStatus: StatusCountResponse[];
  topProducts: TopProductResponse[];
  slotUtilization: SlotUtilizationResponse[];
}

export async function fetchDashboardSnapshot(
  adminKey: string,
  range: DashboardRange,
): Promise<DashboardSnapshot> {
  const options = { headers: adminHeaders(adminKey) };
  const [
    overview,
    revenueBreakdown,
    refundStats,
    dailyRevenue,
    orderStatus,
    topProducts,
    slotUtilization,
  ] = await Promise.all([
    getOverview(range, options),
    getRevenueBreakdown(range, options),
    getRefundStats(range, options),
    getDailyRevenue(range, options),
    getOrderStatusDistribution(options),
    getTopProducts(range, options),
    getSlotUtilization(range, options),
  ]);

  return {
    overview,
    revenueBreakdown,
    refundStats,
    dailyRevenue,
    orderStatus,
    topProducts,
    slotUtilization,
  };
}

export function fetchSalesSummary(
  adminKey: string,
  range: DashboardRange,
  granularity: DashboardGranularity,
): Promise<PeriodSalesSummaryResponse[]> {
  return getSalesSummary(
    { ...range, granularity },
    { headers: adminHeaders(adminKey) },
  );
}
