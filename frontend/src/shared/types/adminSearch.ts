import type { AdminBookingSearchItemResponse } from "@/generated/api/adminBooking";
import type {
  AdminOrderSearchPageResponse,
  AdminOrderSearchResult,
} from "@/generated/api/adminOrder";

export type OffsetPage<T> = Omit<AdminOrderSearchPageResponse, "content"> & {
  content: T[];
};
export type AdminOrderSearchRow = AdminOrderSearchResult;
export type AdminBookingSearchRow = AdminBookingSearchItemResponse;
