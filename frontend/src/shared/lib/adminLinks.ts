/**
 * 관리자 화면에서 특정 항목을 바로 열 때 쓰는 주소 값.
 * 다른 메뉴로 이동하면 AdminPage가 이 값을 모두 지운다.
 */
export const ADMIN_FOCUS_PARAMS = [
  "orderId",
  "orderStatus",
  "bookingId",
  "bookingDate",
  "bookingStatus",
  "reviewId",
  "productId",
  "variantId",
  "smartstoreOrderId",
] as const;

export type AdminFocusParam = (typeof ADMIN_FOCUS_PARAMS)[number];

type FocusValue = string | number | null | undefined;

/** 메뉴 다음에 넘긴 순서대로 값을 붙인다. 값이 없으면 생략한다. */
function adminHref(view: string, focus: ReadonlyArray<readonly [AdminFocusParam, FocusValue]>): string {
  const params = new URLSearchParams({ view });
  for (const [name, value] of focus) {
    if (value != null && value !== "") params.set(name, String(value));
  }
  return `/admin?${params.toString()}`;
}

export function adminOrderHref(orderId: number, orderStatus: string): string {
  return adminHref("orders", [["orderId", orderId], ["orderStatus", orderStatus]]);
}

export function adminBookingHref(bookingId: number, bookingDate: string, bookingStatus: string): string {
  return adminHref("bookings", [
    ["bookingId", bookingId],
    ["bookingDate", bookingDate],
    ["bookingStatus", bookingStatus],
  ]);
}

export function adminProductHref(productId: number, variantId?: number | null): string {
  return adminHref("products", [["productId", productId], ["variantId", variantId]]);
}

export function adminSmartStoreOrderHref(productOrderId: string): string {
  return adminHref("orders", [["smartstoreOrderId", productOrderId]]);
}
