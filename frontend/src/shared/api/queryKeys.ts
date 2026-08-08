import type { QueryKey } from "@tanstack/react-query";

export const queryKeys = {
  admin: {
    all: ["admin"] as const,
    notices: ["admin", "notices"] as const,
    events: ["admin", "events"] as const,
    coupons: ["admin", "coupons"] as const,
    productQna: {
      all: ["admin", "qna"] as const,
      unanswered: (cursor?: string) =>
        ["admin", "qna", "unanswered", cursor] as const,
      byProduct: (productId: number, cursor?: string) =>
        ["admin", "qna", "product", productId, cursor] as const,
    },
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
  notices: {
    all: ["notices"] as const,
    detail: (noticeId: number) => ["notices", noticeId] as const,
  },
  events: {
    all: ["events"] as const,
    detail: (eventId: number) => ["events", eventId] as const,
  },
  productQna: {
    all: ["product-qna"] as const,
    byProduct: (productId: number) => ["product-qna", productId] as const,
    history: (productId: number) =>
      ["product-qna", productId, "history"] as const,
    detail: (productId: number, qnaId: number) =>
      ["product-qna", productId, qnaId] as const,
  },
  member: {
    all: ["me"] as const,
    orders: {
      all: ["me", "orders"] as const,
      history: ["me", "orders", "history"] as const,
      detail: (orderId: number) => ["me", "orders", orderId] as const,
      claims: (orderId: number) => ["me", "orders", orderId, "claims"] as const,
    },
    bookings: {
      all: ["me", "bookings"] as const,
      history: ["me", "bookings", "history"] as const,
      detail: (bookingId: number) => ["me", "bookings", bookingId] as const,
    },
    passes: ["me", "passes"] as const,
    passHistory: ["me", "passes", "history"] as const,
    passCandidates: ["me", "passes", "candidates"] as const,
    coupons: ["me", "coupons"] as const,
    claimableCoupons: ["me", "coupons", "claimable"] as const,
    rewards: ["me", "rewards"] as const,
    cart: ["me", "cart"] as const,
    inquiries: ["me", "inquiries"] as const,
    inquiryHistory: ["me", "inquiries", "history"] as const,
    guestClaimPreview: ["me", "guest-claims", "preview"] as const,
    productQna: {
      all: ["me", "product-qna"] as const,
      byProduct: (productId: number) => ["me", "product-qna", productId] as const,
      history: (productId: number) =>
        ["me", "product-qna", productId, "history"] as const,
      detail: (productId: number, qnaId: number) =>
        ["me", "product-qna", productId, qnaId] as const,
    },
    guestRecovery: {
      all: ["me", "guest-recovery"] as const,
      orders: (
        boundaryEpoch: string | null,
        boundaryCustomerId: number | null,
        sessionVersion: number,
        expiresAt: string,
      ) => [
        "me",
        "guest-recovery",
        "orders",
        boundaryEpoch,
        boundaryCustomerId,
        sessionVersion,
        expiresAt,
      ] as const,
      bookings: (
        boundaryEpoch: string | null,
        boundaryCustomerId: number | null,
        sessionVersion: number,
        expiresAt: string,
      ) => [
        "me",
        "guest-recovery",
        "bookings",
        boundaryEpoch,
        boundaryCustomerId,
        sessionVersion,
        expiresAt,
      ] as const,
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
