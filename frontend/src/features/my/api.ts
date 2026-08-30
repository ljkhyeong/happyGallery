import {
  listMyBookings as requestRecentMyBookings,
  listMyBookingsPage,
} from "@/generated/api/booking";
import {
  listMyOrders as requestRecentMyOrders,
  listMyOrdersPage,
  listMyPasses as requestRecentMyPasses,
  listMyPassesPage,
  refundMyPass as requestMyPassRefund,
} from "@/generated/api/customerStore";
import type { MyPassSummary } from "@/generated/api/customerStore";

export type { MyBookingSummary } from "@/generated/api/booking";
export type {
  MyOrderSummary,
} from "@/generated/api/customerStore";
export type { MyPassSummary };

export const MY_HISTORY_PAGE_SIZE = 20;

export function fetchRecentMyOrders(signal?: AbortSignal) {
  return requestRecentMyOrders({ signal });
}

export function fetchMyOrdersPage(cursor?: string, signal?: AbortSignal) {
  return listMyOrdersPage(
    { cursor, size: MY_HISTORY_PAGE_SIZE },
    { signal },
  );
}

export function fetchRecentMyBookings(signal?: AbortSignal) {
  return requestRecentMyBookings({ signal });
}

export function fetchMyBookingsPage(cursor?: string, signal?: AbortSignal) {
  return listMyBookingsPage(
    { cursor, size: MY_HISTORY_PAGE_SIZE },
    { signal },
  );
}

export function fetchRecentMyPasses(signal?: AbortSignal) {
  return requestRecentMyPasses({ signal });
}

export function fetchMyPassesPage(cursor?: string, signal?: AbortSignal) {
  return listMyPassesPage(
    { cursor, size: MY_HISTORY_PAGE_SIZE },
    { signal },
  );
}

export async function fetchAllMyPasses(signal?: AbortSignal) {
  const passes: MyPassSummary[] = [];
  const seenCursors = new Set<string>();
  let cursor: string | undefined | null;

  while (cursor !== null) {
    const page = await fetchMyPassesPage(cursor ?? undefined, signal);
    passes.push(...page.content);
    if (!page.hasMore || !page.nextCursor || seenCursors.has(page.nextCursor)) {
      cursor = null;
      continue;
    }
    seenCursors.add(page.nextCursor);
    cursor = page.nextCursor;
  }

  return passes;
}

export function refundMyPass(passId: number) {
  return requestMyPassRefund(passId);
}
