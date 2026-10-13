import type { QueryKey } from "@tanstack/react-query";

type ReviewTargetType = "PRODUCT" | "CLASS";

const reviewTargetSegment = (targetType: ReviewTargetType) =>
  targetType === "PRODUCT" ? "products" : "classes";

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
    products: ["admin", "products"] as const,
    classes: ["admin", "classes"] as const,
    slots: {
      all: ["admin", "slots"] as const,
      byClass: (classId: number) => ["admin", "slots", classId] as const,
      calendar: (dateFrom: string, dateTo: string) =>
        ["admin", "slots", "calendar", dateFrom, dateTo] as const,
    },
    bookings: ["admin", "bookings"] as const,
    bookingCancellationTasks: ["admin", "bookings", "cancellation-tasks"] as const,
  },
  catalog: {
    products: ["products"] as const,
    productCategories: ["product-categories"] as const,
    productDetail: (productId: number) => ["products", productId] as const,
    classes: ["classes"] as const,
    classDetail: (classId: number) => ["classes", classId] as const,
    /** 조건이 없으면 전체 목록(products)과 같은 캐시를 쓴다. */
    productList: (filters: object) => Object.keys(filters).length > 0
      ? ["products", filters] as const
      : ["products"] as const,
  },
  workshop: ["workshop-profile"] as const,
  notices: {
    all: ["notices"] as const,
    detail: (noticeId: number) => ["notices", noticeId] as const,
  },
  events: {
    all: ["events"] as const,
    detail: (eventId: number) => ["events", eventId] as const,
  },
  productQna: {
    byProduct: (productId: number) => ["product-qna", productId] as const,
    history: (productId: number) =>
      ["product-qna", productId, "history"] as const,
    detail: (productId: number, qnaId: number) =>
      ["product-qna", productId, qnaId] as const,
  },
  reviews: {
    products: {
      byProduct: (productId: number) => ["reviews", "products", productId] as const,
      history: (productId: number, rating?: number, sort = "LATEST") =>
        ["reviews", "products", productId, "history", rating, sort] as const,
    },
    classes: {
      byClass: (classId: number) => ["reviews", "classes", classId] as const,
      history: (classId: number, rating?: number, sort = "LATEST") =>
        ["reviews", "classes", classId, "history", rating, sort] as const,
    },
    /** 작품·클래스 후기를 대상 종류와 무관하게 다룰 때 쓴다. products·classes 키와 값이 같다. */
    byTarget: (targetType: ReviewTargetType, targetId: number) =>
      ["reviews", reviewTargetSegment(targetType), targetId] as const,
    historyByTarget: (targetType: ReviewTargetType, targetId: number, rating?: number, sort = "LATEST") =>
      ["reviews", reviewTargetSegment(targetType), targetId, "history", rating, sort] as const,
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
    vacancyAlerts: ["me", "vacancy-alerts"] as const,
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
    restockAlerts: ["me", "restock-alerts"] as const,
    socialAccounts: ["me", "social-accounts"] as const,
    defaultShippingAddress: ["me", "default-shipping-address"] as const,
    favorites: {
      all: ["me", "favorites"] as const,
      lists: ["me", "favorites", "list"] as const,
      list: (type: string) => ["me", "favorites", "list", type] as const,
      status: (type: string, targetId: number) =>
        ["me", "favorites", "status", type, targetId] as const,
    },
    groupInquiries: {
      all: ["me", "group-inquiries"] as const,
      lists: ["me", "group-inquiries", "list"] as const,
      list: (cursor: string | undefined) =>
        ["me", "group-inquiries", "list", cursor] as const,
      detail: (inquiryId: number) =>
        ["me", "group-inquiries", "detail", inquiryId] as const,
    },
    notifications: {
      all: ["me", "notifications"] as const,
      page: (page: number, unreadOnly: boolean) =>
        ["me", "notifications", page, unreadOnly] as const,
      unreadCount: ["me", "notifications", "unread-count"] as const,
    },
    paymentAbandonStatus: (
      sessionVersion: number,
      boundaryEpoch: string | null,
      orderId: string | undefined,
    ) => ["me", "payment-abandon-status", sessionVersion, boundaryEpoch, orderId] as const,
    productQna: {
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
      records: (
        kind: "orders" | "bookings",
        boundaryEpoch: string | null,
        boundaryCustomerId: number | null,
        sessionVersion: number,
        expiresAt: string,
      ) => [
        "me",
        "guest-recovery",
        kind,
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
  /** 비회원 조회는 접근 토큰 대신 조회마다 만든 요청 ID로 캐시를 나눈다. */
  guest: {
    record: (
      kind: "order" | "booking",
      recordId: number | undefined,
      requestId: string | undefined,
    ) => ["guest", kind, recordId, requestId] as const,
    orderClaims: (orderId: number, requestKey: string) =>
      ["guest", "order", orderId, "claims", requestKey] as const,
    paymentStatus: (
      sessionVersion: number,
      boundaryEpoch: string | null,
      boundaryCustomerId: number | null,
      orderId: string,
      statusToken: string | null,
    ) => [
      "guest",
      "payment-status",
      sessionVersion,
      boundaryEpoch,
      boundaryCustomerId,
      orderId,
      statusToken,
    ] as const,
  },
  policies: {
    consent: ["policy-consent", "current"] as const,
    orderPrice: ["orders", "price-policy"] as const,
    passPayment: ["payment", "pass-policy"] as const,
  },
  botProtection: ["bot-protection"] as const,
} as const;

export function isCustomerQueryKey(queryKey: QueryKey): boolean {
  return queryKey[0] === queryKeys.member.all[0];
}
