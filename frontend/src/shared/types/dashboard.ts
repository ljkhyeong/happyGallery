export type DashboardGranularity = "DAILY" | "WEEKLY" | "MONTHLY";

export interface DashboardOverview {
  todayRevenue: number;
  todayOrderCount: number;
  pendingApprovalCount: number;
  todayBookingCount: number;
  monthRevenue: number;
  monthOrderCount: number;
}

export interface PeriodSalesSummary {
  periodLabel: string;
  totalRevenue: number;
  orderCount: number;
  avgOrderValue: number;
}

export interface RevenueBreakdown {
  orderRevenue: number;
  bookingDepositRevenue: number;
  bookingBalanceRevenue: number;
  passPurchaseRevenue: number;
  totalRevenue: number;
}

export interface RefundStats {
  totalRefundCount: number;
  totalRefundedAmount: number;
  refundRate: number;
}

export interface DailyRevenue {
  date: string;
  revenue: number;
}
