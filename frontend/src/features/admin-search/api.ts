import { adminHeaders, api } from "@/shared/api";
import type {
  AdminBookingSearchRow,
  AdminOrderSearchRow,
  OffsetPage,
} from "@/shared/types";

export type AdminSearchTarget = "ORDER" | "BOOKING";

export interface AdminSearchCriteria {
  target: AdminSearchTarget;
  status?: string;
  dateFrom?: string;
  dateTo?: string;
  keyword?: string;
  page: number;
}

export type AdminSearchResult =
  | { target: "ORDER"; page: OffsetPage<AdminOrderSearchRow> }
  | { target: "BOOKING"; page: OffsetPage<AdminBookingSearchRow> };

export async function searchAdminRecords(
  adminKey: string,
  criteria: AdminSearchCriteria,
): Promise<AdminSearchResult> {
  const params = {
    status: criteria.status,
    dateFrom: criteria.dateFrom,
    dateTo: criteria.dateTo,
    keyword: criteria.keyword,
    page: criteria.page,
    size: 20,
  };

  if (criteria.target === "ORDER") {
    const page = await api<OffsetPage<AdminOrderSearchRow>>("/admin/orders/search", {
      headers: adminHeaders(adminKey),
      params,
    });
    return { target: "ORDER", page };
  }

  const page = await api<OffsetPage<AdminBookingSearchRow>>("/admin/bookings/search", {
    headers: adminHeaders(adminKey),
    params,
  });
  return { target: "BOOKING", page };
}
