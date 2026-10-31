import assert from "node:assert/strict";
import test from "node:test";

import { queryKeys } from "../../src/shared/api/queryKeys.ts";

test("공개 공지 목록 키는 상세 키의 공통 접두사다", () => {
  assert.deepEqual(queryKeys.notices.all, ["notices"]);
  assert.deepEqual(queryKeys.notices.detail(42), ["notices", 42]);
});

test("관리자 공지 키는 관리자 캐시 경계 안에 있다", () => {
  assert.deepEqual(queryKeys.admin.notices, ["admin", "notices"]);
  assert.equal(queryKeys.admin.notices[0], queryKeys.admin.all[0]);
});

test("공개 이벤트 목록과 상세는 함께 무효화할 접두사를 공유한다", () => {
  assert.deepEqual(queryKeys.events.all, ["events"]);
  assert.deepEqual(queryKeys.events.detail(42), ["events", 42]);
});

test("관리자 이벤트 키는 관리자 캐시 경계 안에 있다", () => {
  assert.deepEqual(queryKeys.admin.events, ["admin", "events"]);
  assert.equal(queryKeys.admin.events[0], queryKeys.admin.all[0]);
});

test("회원 쿠폰과 적립금 키는 로그인 세대 캐시 경계 안에 있다", () => {
  assert.deepEqual(queryKeys.member.coupons, ["me", "coupons"]);
  assert.deepEqual(queryKeys.member.claimableCoupons, ["me", "coupons", "claimable"]);
  assert.deepEqual(queryKeys.member.rewards, ["me", "rewards"]);
  assert.equal(queryKeys.member.coupons[0], queryKeys.member.all[0]);
  assert.equal(queryKeys.member.rewards[0], queryKeys.member.all[0]);
});

test("회원 빈자리 알림 키는 로그인 세대 캐시 경계 안에 있다", () => {
  assert.deepEqual(queryKeys.member.vacancyAlerts, ["me", "vacancy-alerts"]);
  assert.equal(queryKeys.member.vacancyAlerts[0], queryKeys.member.all[0]);
});

test("관리자 쿠폰 키는 관리자 캐시 경계 안에 있다", () => {
  assert.deepEqual(queryKeys.admin.coupons, ["admin", "coupons"]);
  assert.equal(queryKeys.admin.coupons[0], queryKeys.admin.all[0]);
});

test("관리자 키 레지스트리의 모든 키는 관리자 캐시 경계 안에 있다", () => {
  const visit = (value) => {
    if (typeof value === "function") return visit(value("x", {}, "z", 1));
    if (Array.isArray(value)) {
      assert.equal(value[0], queryKeys.admin.all[0]);
      return;
    }
    for (const nested of Object.values(value)) visit(nested);
  };
  visit(queryKeys.admin);
});

test("관리자 예약 목록 키는 예약 접두사 아래에서 날짜·상태로 나뉜다", () => {
  assert.deepEqual(
    queryKeys.admin.bookingList("2026-10-25", "BOOKED"),
    ["admin", "bookings", "2026-10-25", "BOOKED"],
  );
  assert.deepEqual(queryKeys.admin.bookingList("2026-10-25", "").slice(0, 2), queryKeys.admin.bookings);
});

test("관리자 상품 하위 조회 키는 상품 목록 접두사를 공유한다", () => {
  assert.deepEqual(queryKeys.admin.products.all, ["admin", "products"]);
  assert.deepEqual(queryKeys.admin.products.stockLevels.slice(0, 2), queryKeys.admin.products.all);
  assert.deepEqual(queryKeys.admin.products.stockLevelsByProduct(3).slice(0, 2), queryKeys.admin.products.all);
  assert.deepEqual(queryKeys.admin.products.inventoryAdjustments(3).slice(0, 2), queryKeys.admin.products.all);
  assert.deepEqual(queryKeys.admin.products.smartStoreInventory(3).slice(0, 2), queryKeys.admin.products.all);
  assert.deepEqual(queryKeys.admin.products.smartStorePreview(3).slice(0, 2), queryKeys.admin.products.all);
});

test("관리자 단체 문의 후속 연락 키는 목록 접두사 아래에 있고 상세 키는 밖에 있다", () => {
  assert.deepEqual(queryKeys.admin.groupInquiries.all, ["admin", "group-inquiries"]);
  assert.deepEqual(
    queryKeys.admin.groupInquiries.page("CONSULTING", {}, undefined).slice(0, 2),
    queryKeys.admin.groupInquiries.all,
  );
  assert.deepEqual(
    queryKeys.admin.groupInquiries.followUps("cursor").slice(0, 2),
    queryKeys.admin.groupInquiries.all,
  );
  assert.deepEqual(queryKeys.admin.groupInquiry.detail(51), ["admin", "group-inquiry", 51]);
  assert.notDeepEqual(queryKeys.admin.groupInquiry.detail(51).slice(0, 2), queryKeys.admin.groupInquiries.all);
});

test("관리자 목록 무효화 접두사는 하위 페이지·상세 키를 포함한다", () => {
  const admin = queryKeys.admin;
  const under = [
    [admin.inquiries.page("cursor"), admin.inquiries.all],
    [admin.orderClaims.page("REQUESTED", "cursor"), admin.orderClaims.all],
    [admin.orders.page(""), admin.orders.all],
    [admin.orders.fulfillment(7), admin.orders.all],
    [admin.orders.history(7), admin.orders.all],
    [admin.passes.search("keyword", 0), admin.passes.all],
    [admin.passes.detail(7), admin.passes.all],
    [admin.refunds.failedPage("cursor"), admin.refunds.failed],
    [admin.smartStore.orders.detail("order"), admin.smartStore.orders.all],
    [admin.smartStore.orders.actions("order"), admin.smartStore.orders.all],
    [admin.smartStore.orders.currentStatus("order"), admin.smartStore.orders.all],
    [admin.smartStore.orderActions.unresolvedPage(null), admin.smartStore.orderActions.unresolved],
    [admin.smartStore.notices.page(1), admin.smartStore.notices.all],
    [admin.smartStore.notices.detail(3), admin.smartStore.notices.all],
    [admin.smartStore.inspections.page(1), admin.smartStore.inspections.all],
    [admin.smartStore.inquiries.product({}), admin.smartStore.inquiries.all],
    [admin.smartStore.inquiries.customer({}), admin.smartStore.inquiries.all],
    [admin.smartStore.inquiries.template, admin.smartStore.inquiries.all],
  ];
  for (const [child, parent] of under) {
    assert.deepEqual(child.slice(0, parent.length), parent);
  }
  // 환불 상태 확인은 실패 목록 재조회에 걸리지 않고, MFA 등록 안내는 설정 화면과 다른 캐시를 쓴다.
  assert.notDeepEqual(admin.refund(7).slice(0, 2), admin.refunds.failed.slice(0, 2));
  assert.notDeepEqual(admin.auth.mfaEnrollmentGate, admin.auth.mfa);
});

test("관리자 공방 정보 키는 공개 공방 키와 캐시 경계가 다르다", () => {
  assert.deepEqual(queryKeys.admin.workshopProfile, ["admin", "workshop-profile"]);
  assert.deepEqual(queryKeys.workshop, ["workshop-profile"]);
});

test("상품 Q&A 목록과 상세 키는 답변 뒤 함께 무효화할 상품 접두사를 공유한다", () => {
  assert.deepEqual(
    queryKeys.productQna.history(42).slice(0, 2),
    queryKeys.productQna.byProduct(42),
  );
  assert.deepEqual(
    queryKeys.productQna.detail(42, 7).slice(0, 2),
    queryKeys.productQna.byProduct(42),
  );
  assert.deepEqual(
    queryKeys.member.productQna.history(42).slice(0, 3),
    queryKeys.member.productQna.byProduct(42),
  );
  assert.deepEqual(
    queryKeys.member.productQna.detail(42, 7).slice(0, 3),
    queryKeys.member.productQna.byProduct(42),
  );
  assert.deepEqual(
    queryKeys.admin.productQna.byProduct(42).slice(0, 2),
    queryKeys.admin.productQna.all,
  );
});

test("후기 키는 공개 대상과 회원·관리자 세션 경계를 각각 유지한다", () => {
  assert.deepEqual(
    queryKeys.reviews.products.history(42, 5, "RATING_HIGH").slice(0, 3),
    queryKeys.reviews.products.byProduct(42),
  );
  assert.deepEqual(
    queryKeys.reviews.classes.history(7, 4, "LATEST").slice(0, 3),
    queryKeys.reviews.classes.byClass(7),
  );
  assert.deepEqual(
    queryKeys.reviews.products.history(42, 5, "RATING_HIGH").slice(-2),
    [5, "RATING_HIGH"],
  );
  // 고객 화면은 대상 종류로, 관리자 화면은 종류별 키로 같은 공개 후기 캐시를 무효화한다.
  assert.deepEqual(queryKeys.reviews.byTarget("PRODUCT", 42), queryKeys.reviews.products.byProduct(42));
  assert.deepEqual(queryKeys.reviews.byTarget("CLASS", 7), queryKeys.reviews.classes.byClass(7));
  assert.deepEqual(
    queryKeys.reviews.historyByTarget("CLASS", 7, 4, "LATEST"),
    queryKeys.reviews.classes.history(7, 4, "LATEST"),
  );
  assert.equal(queryKeys.member.reviews.byOrder(9)[0], queryKeys.member.all[0]);
  assert.equal(queryKeys.member.reviews.byBooking(11)[0], queryKeys.member.all[0]);
  assert.equal(queryKeys.member.reviews.opportunities[0], queryKeys.member.all[0]);
  assert.equal(queryKeys.member.reviews.productCreationState(13)[0], queryKeys.member.all[0]);
  assert.equal(queryKeys.member.reviews.classCreationState(17)[0], queryKeys.member.all[0]);
  assert.deepEqual(
    queryKeys.member.reviews.reactions([3, 8]),
    ["me", "reviews", "reactions", 3, 8],
  );
  assert.deepEqual(
    queryKeys.admin.reviews.page("PRODUCT", "HIDDEN").slice(0, 2),
    queryKeys.admin.reviews.all,
  );
  assert.deepEqual(
    queryKeys.admin.reviews.detail(31).slice(0, 2),
    queryKeys.admin.reviews.all,
  );
  assert.deepEqual(
    queryKeys.admin.reviews.reports.page("PENDING").slice(0, 3),
    queryKeys.admin.reviews.reports.all,
  );
  assert.deepEqual(
    queryKeys.admin.reviews.reports.detail(71).slice(0, 3),
    queryKeys.admin.reviews.reports.all,
  );
});

test("회원 이력 페이지 키는 기존 무효화 접두사 아래에서 summary 캐시와 분리된다", () => {
  assert.deepEqual(queryKeys.member.orders.history.slice(0, 2), queryKeys.member.orders.all);
  assert.deepEqual(queryKeys.member.bookings.history.slice(0, 2), queryKeys.member.bookings.all);
  assert.deepEqual(queryKeys.member.passHistory.slice(0, 2), queryKeys.member.passes);
  assert.deepEqual(queryKeys.member.inquiryHistory.slice(0, 2), queryKeys.member.inquiries);
  assert.notDeepEqual(queryKeys.member.orders.history, queryKeys.member.orders.all);
});

test("비회원 복구 이력 키는 회원 세대에 속하고 원문 토큰을 포함하지 않는다", () => {
  const secretToken = "raw-guest-recovery-token";
  const key = queryKeys.member.guestRecovery.records(
    "orders",
    "storage-owner-epoch",
    null,
    17,
    "2099-01-01T00:00:00",
  );

  assert.equal(key[0], queryKeys.member.all[0]);
  assert.equal(JSON.stringify(key).includes(secretToken), false);
  assert.deepEqual(key.slice(0, 2), queryKeys.member.guestRecovery.all);
});
