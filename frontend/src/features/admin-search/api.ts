import {
  searchBookings,
  SearchBookingsStatus,
  type AdminBookingSearchPageResponse,
  type SearchBookingsStatus as BookingSearchStatus,
} from "@/generated/api/adminBooking";
import {
  searchOrders,
  SearchOrdersStatus,
  type AdminOrderSearchPageResponse,
  type SearchOrdersStatus as OrderSearchStatus,
} from "@/generated/api/adminOrder";
import {
  searchAdminPasses,
  type AdminPassPageResponse,
} from "@/generated/api/adminOperations";
import { adminHeaders } from "@/shared/api";

export type AdminSearchTarget = "CUSTOMER" | "ORDER" | "BOOKING";

export interface AdminSearchCriteria {
  target: AdminSearchTarget;
  status?: string;
  dateFrom?: string;
  dateTo?: string;
  keyword?: string;
  page: number;
}

export type AdminSearchResult =
  | {
      target: "CUSTOMER";
      orders: AdminOrderSearchPageResponse;
      bookings: AdminBookingSearchPageResponse;
      passes: AdminPassPageResponse;
    }
  | { target: "ORDER"; page: AdminOrderSearchPageResponse }
  | { target: "BOOKING"; page: AdminBookingSearchPageResponse };

export async function searchAdminRecords(
  adminKey: string,
  criteria: AdminSearchCriteria,
): Promise<AdminSearchResult> {
  const options = { headers: adminHeaders(adminKey) };
  const common = {
    dateFrom: criteria.dateFrom,
    dateTo: criteria.dateTo,
    keyword: criteria.keyword,
    page: criteria.page,
    size: 20,
  };

  if (criteria.target === "CUSTOMER") {
    const [orders, bookings, passes] = await Promise.all([
      searchOrders({ ...common }, options),
      searchBookings({ ...common }, options),
      searchAdminPasses(
        { keyword: criteria.keyword, page: criteria.page, size: 20 },
        options,
      ),
    ]);
    return { target: "CUSTOMER", orders, bookings, passes };
  }

  if (criteria.target === "ORDER") {
    const page = await searchOrders(
      { ...common, status: orderStatus(criteria.status) },
      options,
    );
    return { target: "ORDER", page };
  }

  const page = await searchBookings(
    { ...common, status: bookingStatus(criteria.status) },
    options,
  );
  return { target: "BOOKING", page };
}

function orderStatus(value: string | undefined): OrderSearchStatus | undefined {
  return enumValue(SearchOrdersStatus, value, "주문");
}

function bookingStatus(value: string | undefined): BookingSearchStatus | undefined {
  return enumValue(SearchBookingsStatus, value, "예약");
}

function enumValue<T extends string>(
  values: Record<string, T>,
  value: string | undefined,
  label: string,
): T | undefined {
  if (value === undefined) {
    return undefined;
  }
  const matched = Object.values(values).find((candidate) => candidate === value);
  if (matched === undefined) {
    throw new Error(`지원하지 않는 ${label} 상태입니다.`);
  }
  return matched;
}
