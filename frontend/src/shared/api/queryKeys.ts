import type { QueryKey } from "@tanstack/react-query";

type ReviewTargetType = "PRODUCT" | "CLASS";

const reviewTargetSegment = (targetType: ReviewTargetType) =>
  targetType === "PRODUCT" ? "products" : "classes";

export const queryKeys = {
  admin: {
    all: ["admin"] as const,
    auth: {
      mfa: ["admin", "auth", "mfa"] as const,
      /** 운영 MFA 등록 안내는 설정 화면과 같은 상태를 별도 캐시로 읽어 등록 완료 뒤 복구 코드 화면을 유지한다. */
      mfaEnrollmentGate: ["admin", "auth", "mfa", "enrollment-gate"] as const,
    },
    dashboard: {
      snapshot: (from: string, to: string) =>
        ["admin", "dashboard", "snapshot", from, to] as const,
      sales: (from: string, to: string, granularity: string) =>
        ["admin", "dashboard", "sales", from, to, granularity] as const,
    },
    search: (criteria: object | null) => ["admin", "search", criteria] as const,
    workshopProfile: ["admin", "workshop-profile"] as const,
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
    /** 상품별 보조 조회는 상품 목록 접두사 아래에 두어 상품 목록 무효화에 함께 다시 읽힌다. */
    products: {
      all: ["admin", "products"] as const,
      stockLevels: ["admin", "products", "stock-levels"] as const,
      stockLevelsByProduct: (productId: number) =>
        ["admin", "products", productId, "stock-levels"] as const,
      inventoryAdjustments: (productId: number) =>
        ["admin", "products", productId, "inventory-adjustments"] as const,
      smartStoreInventory: (productId: number) =>
        ["admin", "products", productId, "smartstore-inventory"] as const,
      smartStoreInventoryHistory: (productId: number) =>
        ["admin", "products", productId, "smartstore-inventory-history"] as const,
      smartStorePreview: (productId: number) =>
        ["admin", "products", productId, "smartstore-product-preview"] as const,
    },
    restockDemand: (page: number) => ["admin", "restock-demand", page] as const,
    classes: ["admin", "classes"] as const,
    slots: {
      all: ["admin", "slots"] as const,
      byClass: (classId: number) => ["admin", "slots", classId] as const,
      calendar: (dateFrom: string, dateTo: string) =>
        ["admin", "slots", "calendar", dateFrom, dateTo] as const,
    },
    bookings: ["admin", "bookings"] as const,
    bookingList: (date: string, status: string) =>
      ["admin", "bookings", date, status] as const,
    bookingCancellationTasks: ["admin", "bookings", "cancellation-tasks"] as const,
    orders: {
      all: ["admin", "orders"] as const,
      page: (status: string) => ["admin", "orders", status] as const,
      fulfillment: (orderId: number | undefined) =>
        ["admin", "orders", orderId, "fulfillment"] as const,
      history: (orderId: number) => ["admin", "orders", orderId, "history"] as const,
    },
    orderClaims: {
      all: ["admin", "order-claims"] as const,
      page: (status: string, cursor: string | undefined) =>
        ["admin", "order-claims", status, cursor] as const,
    },
    passes: {
      all: ["admin", "passes"] as const,
      search: (keyword: string, page: number) =>
        ["admin", "passes", "search", keyword, page] as const,
      detail: (passId: number | null) => ["admin", "passes", "detail", passId] as const,
    },
    refunds: {
      failed: ["admin", "refunds", "failed"] as const,
      failedPage: (cursor: string | undefined) =>
        ["admin", "refunds", "failed", cursor] as const,
    },
    /** 환불 상태 확인은 실패 목록 접두사와 별개로 한 건씩 읽는다. */
    refund: (refundId: number) => ["admin", "refund", refundId] as const,
    payments: {
      reconciliationRequired: ["admin", "payment-attempts", "reconciliation-required"] as const,
      settlementIssues: ["admin", "payment-settlements", "issues"] as const,
    },
    notifications: {
      failed: ["admin", "notifications", "failed"] as const,
    },
    inquiries: {
      all: ["admin", "inquiries"] as const,
      page: (cursor: string | undefined) => ["admin", "inquiries", cursor] as const,
    },
    groupInquiries: {
      all: ["admin", "group-inquiries"] as const,
      page: (status: string, filters: object, cursor: string | undefined) =>
        ["admin", "group-inquiries", status, filters, cursor] as const,
      followUps: (cursor: string | undefined) =>
        ["admin", "group-inquiries", "follow-ups", cursor] as const,
    },
    /** 단체 문의 상세는 목록 접두사 밖에 두어 목록을 다시 읽어도 상세는 다시 읽지 않는다. */
    groupInquiry: {
      detail: (inquiryId: number) => ["admin", "group-inquiry", inquiryId] as const,
    },
    smartStore: {
      products: {
        page: (page: number) => ["admin", "smartstore-products", page] as const,
        detail: (originProductNo: number) =>
          ["admin", "smartstore-products", "detail", originProductNo] as const,
        noticeCatalog: (page: number) =>
          ["admin", "smartstore-products", "notice", page] as const,
      },
      notices: {
        all: ["admin", "smartstore-notices"] as const,
        page: (page: number) => ["admin", "smartstore-notices", "page", page] as const,
        detail: (sellerNoticeId: number | null | undefined) =>
          ["admin", "smartstore-notices", sellerNoticeId] as const,
      },
      inspections: {
        all: ["admin", "smartstore-inspections"] as const,
        page: (page: number) => ["admin", "smartstore-inspections", page] as const,
      },
      inquiries: {
        all: ["admin", "smartstore-inquiries"] as const,
        product: (params: object) =>
          ["admin", "smartstore-inquiries", "product", params] as const,
        customer: (params: object) =>
          ["admin", "smartstore-inquiries", "customer", params] as const,
        template: ["admin", "smartstore-inquiries", "template"] as const,
      },
      orders: {
        all: ["admin", "smartstore-orders"] as const,
        detail: (productOrderId: string) =>
          ["admin", "smartstore-orders", "detail", productOrderId] as const,
        actions: (productOrderId: string) =>
          ["admin", "smartstore-orders", "actions", productOrderId] as const,
        currentStatus: (productOrderId: string | undefined) =>
          ["admin", "smartstore-orders", productOrderId, "current-status"] as const,
      },
      orderActions: {
        unresolved: ["admin", "smartstore-order-actions", "unresolved"] as const,
        unresolvedPage: (cursor: string | null) =>
          ["admin", "smartstore-order-actions", "unresolved", cursor] as const,
      },
      returnDeliveryCompanies: ["admin", "smartstore-return-delivery-companies"] as const,
      settlements: {
        issues: ["admin", "smartstore-settlements", "issues"] as const,
        accounting: (from: string, to: string) =>
          ["admin", "smartstore-settlements", "accounting", from, to] as const,
      },
    },
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
