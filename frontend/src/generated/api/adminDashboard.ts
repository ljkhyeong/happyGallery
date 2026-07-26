import { generatedApiClient } from '../../shared/api/generatedClient';
export interface DailyRevenueResponse {
  date: string;
  revenue: number;
}

export interface StatusCountResponse {
  count: number;
  status: string;
}

export interface DashboardOverviewResponse {
  monthOrderCount: number;
  monthRevenue: number;
  pendingApprovalCount: number;
  todayBookingCount: number;
  todayOrderCount: number;
  todayRevenue: number;
}

export interface RefundStatsResponse {
  refundRate: number;
  totalRefundCount: number;
  totalRefundedAmount: number;
}

export interface RevenueBreakdownResponse {
  bookingBalanceRevenue: number;
  bookingDepositRevenue: number;
  orderRevenue: number;
  passPurchaseRevenue: number;
  totalRevenue: number;
}

export interface PeriodSalesSummaryResponse {
  avgOrderValue: number;
  orderCount: number;
  periodLabel: string;
  totalRevenue: number;
}

export interface SlotUtilizationResponse {
  className: string;
  date: string;
  totalBooked: number;
  totalCapacity: number;
  utilizationRate: number;
}

export interface TopProductResponse {
  productId: number;
  productName: string;
  productType: string;
  totalQuantity: number;
  totalRevenue: number;
}

export type DailyRevenueParams = {
from: string;
to: string;
};

export type OverviewParams = {
from: string;
to: string;
};

export type RefundStatsParams = {
from: string;
to: string;
};

export type RevenueBreakdownParams = {
from: string;
to: string;
};

export type SalesSummaryParams = {
from: string;
to: string;
granularity?: SalesSummaryGranularity;
};

export type SalesSummaryGranularity = typeof SalesSummaryGranularity[keyof typeof SalesSummaryGranularity];


export const SalesSummaryGranularity = {
  DAILY: 'DAILY',
  WEEKLY: 'WEEKLY',
  MONTHLY: 'MONTHLY',
} as const;

export type SlotUtilizationParams = {
from: string;
to: string;
};

export type TopProductsParams = {
from: string;
to: string;
limit?: number;
sort?: TopProductsSort;
};

export type TopProductsSort = typeof TopProductsSort[keyof typeof TopProductsSort];


export const TopProductsSort = {
  REVENUE: 'REVENUE',
  QUANTITY: 'QUANTITY',
} as const;

export const getDailyRevenueUrl = (params: DailyRevenueParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/dashboard/daily-revenue?${stringifiedParams}` : `/api/v1/admin/dashboard/daily-revenue`
}

export const dailyRevenue = async (params: DailyRevenueParams, options?: RequestInit): Promise<DailyRevenueResponse[]> => {

  return generatedApiClient<DailyRevenueResponse[]>(getDailyRevenueUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getOrderStatusDistributionUrl = () => {




  return `/api/v1/admin/dashboard/order-status`
}

export const orderStatusDistribution = async ( options?: RequestInit): Promise<StatusCountResponse[]> => {

  return generatedApiClient<StatusCountResponse[]>(getOrderStatusDistributionUrl(),
  {
    ...options,
    method: 'GET'


  }
);}



export const getOverviewUrl = (params: OverviewParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/dashboard/overview?${stringifiedParams}` : `/api/v1/admin/dashboard/overview`
}

export const overview = async (params: OverviewParams, options?: RequestInit): Promise<DashboardOverviewResponse> => {

  return generatedApiClient<DashboardOverviewResponse>(getOverviewUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRefundStatsUrl = (params: RefundStatsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/dashboard/refunds?${stringifiedParams}` : `/api/v1/admin/dashboard/refunds`
}

export const refundStats = async (params: RefundStatsParams, options?: RequestInit): Promise<RefundStatsResponse> => {

  return generatedApiClient<RefundStatsResponse>(getRefundStatsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getRevenueBreakdownUrl = (params: RevenueBreakdownParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/dashboard/revenue-breakdown?${stringifiedParams}` : `/api/v1/admin/dashboard/revenue-breakdown`
}

export const revenueBreakdown = async (params: RevenueBreakdownParams, options?: RequestInit): Promise<RevenueBreakdownResponse> => {

  return generatedApiClient<RevenueBreakdownResponse>(getRevenueBreakdownUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getSalesSummaryUrl = (params: SalesSummaryParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/dashboard/sales-summary?${stringifiedParams}` : `/api/v1/admin/dashboard/sales-summary`
}

export const salesSummary = async (params: SalesSummaryParams, options?: RequestInit): Promise<PeriodSalesSummaryResponse[]> => {

  return generatedApiClient<PeriodSalesSummaryResponse[]>(getSalesSummaryUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getSlotUtilizationUrl = (params: SlotUtilizationParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/dashboard/slot-utilization?${stringifiedParams}` : `/api/v1/admin/dashboard/slot-utilization`
}

export const slotUtilization = async (params: SlotUtilizationParams, options?: RequestInit): Promise<SlotUtilizationResponse[]> => {

  return generatedApiClient<SlotUtilizationResponse[]>(getSlotUtilizationUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}



export const getTopProductsUrl = (params: TopProductsParams,) => {
  const normalizedParams = new URLSearchParams();

  Object.entries(params || {}).forEach(([key, value]) => {

    if (value !== undefined) {
      normalizedParams.append(key, value === null ? 'null' : String(value))
    }
  });

  const stringifiedParams = normalizedParams.toString();

  return stringifiedParams.length > 0 ? `/api/v1/admin/dashboard/top-products?${stringifiedParams}` : `/api/v1/admin/dashboard/top-products`
}

export const topProducts = async (params: TopProductsParams, options?: RequestInit): Promise<TopProductResponse[]> => {

  return generatedApiClient<TopProductResponse[]>(getTopProductsUrl(params),
  {
    ...options,
    method: 'GET'


  }
);}
