import type { QueryKey } from "@tanstack/react-query";

export const queryKeys = {
  admin: {
    all: ["admin"] as const,
    classes: ["admin", "classes"] as const,
    slots: {
      all: ["admin", "slots"] as const,
      byClass: (classId: number) => ["admin", "slots", classId] as const,
    },
    bookings: ["admin", "bookings"] as const,
    bookingCancellationTasks: ["admin", "bookings", "cancellation-tasks"] as const,
  },
  catalog: {
    classes: ["classes"] as const,
  },
  member: {
    all: ["me"] as const,
    orders: {
      all: ["me", "orders"] as const,
      detail: (orderId: number) => ["me", "orders", orderId] as const,
      claims: (orderId: number) => ["me", "orders", orderId, "claims"] as const,
    },
    bookings: {
      all: ["me", "bookings"] as const,
      detail: (bookingId: number) => ["me", "bookings", bookingId] as const,
    },
    passes: ["me", "passes"] as const,
    cart: ["me", "cart"] as const,
    inquiries: ["me", "inquiries"] as const,
    guestClaimPreview: ["me", "guest-claims", "preview"] as const,
    productQna: {
      all: ["me", "product-qna"] as const,
      byProduct: (productId: number) => ["me", "product-qna", productId] as const,
      detail: (productId: number, qnaId: number) =>
        ["me", "product-qna", productId, qnaId] as const,
    },
  },
  slotAvailability: {
    upcoming: {
      all: ["upcoming-slots"] as const,
      byClass: (classId: number, days: number) =>
        ["upcoming-slots", classId, days] as const,
    },
    reschedule: {
      all: ["reschedule-slots"] as const,
      byClassAndDate: (classId: number, date: string) =>
        ["reschedule-slots", classId, date] as const,
    },
  },
} as const;

export function isCustomerQueryKey(queryKey: QueryKey): boolean {
  return queryKey[0] === queryKeys.member.all[0];
}
