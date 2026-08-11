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
    reviews: {
      all: ["admin", "reviews"] as const,
      page: (targetType?: string, status?: string, cursor?: string) =>
        ["admin", "reviews", targetType, status, cursor] as const,
      detail: (reviewId: number) =>
        ["admin", "reviews", "detail", reviewId] as const,
      moderation: (reviewId: number) =>
        ["admin", "reviews", "moderation", reviewId] as const,
      reports: {
        all: ["admin", "reviews", "reports"] as const,
        page: (status?: string, cursor?: string) =>
          ["admin", "reviews", "reports", status, cursor] as const,
        detail: (reportId: number) =>
          ["admin", "reviews", "reports", "detail", reportId] as const,
      },
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
    classDetail: (classId: number) => ["classes", classId] as const,
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
  reviews: {
    all: ["reviews"] as const,
    products: {
      all: ["reviews", "products"] as const,
      byProduct: (productId: number) => ["reviews", "products", productId] as const,
      history: (productId: number, rating?: number, sort = "LATEST") =>
        ["reviews", "products", productId, "history", rating, sort] as const,
    },
    classes: {
      all: ["reviews", "classes"] as const,
      byClass: (classId: number) => ["reviews", "classes", classId] as const,
      history: (classId: number, rating?: number, sort = "LATEST") =>
        ["reviews", "classes", classId, "history", rating, sort] as const,
    },
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
    reviews: {
      all: ["me", "reviews"] as const,
      history: ["me", "reviews", "history"] as const,
      opportunities: ["me", "reviews", "opportunities"] as const,
      byOrder: (orderId: number) => ["me", "reviews", "orders", orderId] as const,
      byBooking: (bookingId: number) => ["me", "reviews", "bookings", bookingId] as const,
      productCreationState: (orderItemId: number) =>
        ["me", "reviews", "creation-state", "products", orderItemId] as const,
      classCreationState: (bookingId: number) =>
        ["me", "reviews", "creation-state", "classes", bookingId] as const,
      reactions: (reviewIds: readonly number[]) =>
        ["me", "reviews", "reactions", ...reviewIds] as const,
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
