import {
  searchBookings,
  SearchBookingsStatus,
  type AdminBookingSearchPageResponse,
} from "@/generated/api/adminBooking";
import {
  searchOrders,
  SearchOrdersStatus,
  type AdminOrderSearchPageResponse,
} from "@/generated/api/adminOrder";
import {
  searchAdminPasses,
  type AdminPassPageResponse,
} from "@/generated/api/adminOperations";
import { adminHeaders } from "@/shared/api";
import { parseEnumValue } from "@/shared/lib/enum";

export type AdminSearchTarget = "CUSTOMER" | "ORDER" | "BOOKING";

export interface AdminSearchCriteria {
  target: AdminSearchTarget;
  /** 화면 선택지는 대상별 생성 enum 값으로 만들고 대상을 바꾸면 비운다. */
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
      { ...common, status: parseEnumValue(SearchOrdersStatus, criteria.status) },
      options,
    );
    return { target: "ORDER", page };
  }

  const page = await searchBookings(
    { ...common, status: parseEnumValue(SearchBookingsStatus, criteria.status) },
    options,
  );
  return { target: "BOOKING", page };
}
