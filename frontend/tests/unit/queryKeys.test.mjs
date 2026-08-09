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

test("관리자 쿠폰 키는 관리자 캐시 경계 안에 있다", () => {
  assert.deepEqual(queryKeys.admin.coupons, ["admin", "coupons"]);
  assert.equal(queryKeys.admin.coupons[0], queryKeys.admin.all[0]);
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
    queryKeys.admin.reviews.reports.page("PENDING").slice(0, 3),
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
  const key = queryKeys.member.guestRecovery.orders(
    "storage-owner-epoch",
    null,
    17,
    "2099-01-01T00:00:00",
  );

  assert.equal(key[0], queryKeys.member.all[0]);
  assert.equal(JSON.stringify(key).includes(secretToken), false);
  assert.deepEqual(key.slice(0, 2), queryKeys.member.guestRecovery.all);
});
