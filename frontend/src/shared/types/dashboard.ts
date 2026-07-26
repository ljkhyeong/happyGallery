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

export interface StatusCount {
  status: string;
  count: number;
}

export interface TopProduct {
  productId: number;
  productName: string;
  productType: string;
  totalRevenue: number;
  totalQuantity: number;
}

export interface SlotUtilization {
  date: string;
  className: string;
  totalCapacity: number;
  totalBooked: number;
  utilizationRate: number;
}
