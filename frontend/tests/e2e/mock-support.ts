// API를 모의하는 E2E spec의 공용 응답·세션·fixture. 실제 백엔드를 쓰는 준비 helper는 support.ts에 둔다.
import { expect, type BrowserContext, type Page, type Route } from "@playwright/test";
import type {
  BookingCancelPolicyResponse,
  ClassResponse,
  MyBookingDetail,
} from "../../src/generated/api/booking";
import type { CustomerUserResponse } from "../../src/generated/api/customerAuth";
import type {
  CartResponse,
  GroupInquirySummaryResponse,
  ItemDto,
  OrderDetailResponse,
  RestockAlertResponse,
} from "../../src/generated/api/customerStore";
import type { MyCouponResponse, RewardWalletResponse } from "../../src/generated/api/memberBenefit";
import type { UnreadCountResponse } from "../../src/generated/api/notification";
import type { OrderPricePolicyResponse } from "../../src/generated/api/order";
import type { CurrentPolicyConsentResponse } from "../../src/generated/api/policyConsent";
import type { ProductDetailResponse } from "../../src/generated/api/product";
import type { PublicReviewPageResponse } from "../../src/generated/api/review";
import type { WorkshopProfileResponse } from "../../src/generated/api/workshop";

/** 관리자 화면이 토큰을 읽는 sessionStorage 키. src/features/admin-product/useAdminKey.ts의 TOKEN_KEY와 같다. */
export const ADMIN_TOKEN_KEY = "hg_admin_token";

/** 값을 확인하지 않는 모의 관리자 API에 쓰는 기본 토큰 */
export const MOCK_ADMIN_TOKEN = "e2e-admin-token";

/** 로그인하지 않은 요청의 401 응답 본문. 화면 문구는 code 매핑(src/shared/lib/errorMessages.ts)이 정하므로 message는 단정에 쓰지 않는다. */
export const UNAUTHORIZED_ERROR = { code: "UNAUTHORIZED", message: "로그인이 필요합니다." };

/** 재시도하면 회복되는 일시 장애의 503 응답 본문. 화면 문구는 SERVICE_UNAVAILABLE 매핑이 정한다. */
export const TEMPORARY_ERROR = { code: "SERVICE_UNAVAILABLE", message: "잠시 후 다시 시도해 주세요." };

/** 항목이 없는 커서 페이지. 자원마다 생성 타입이 달라 타입을 붙이지 않는다. 공유 객체이므로 spec에서 수정하지 않는다. */
export const EMPTY_CURSOR_PAGE = { content: [], nextCursor: null, hasMore: false };

/** 빈 회원 장바구니의 cartVersion */
export const EMPTY_CART_VERSION = "0".repeat(64);

/** 빈 회원 장바구니. 공유 객체이므로 spec에서 수정하지 않는다. */
export const EMPTY_CART = { cartVersion: EMPTY_CART_VERSION, items: [], totalAmount: 0 } satisfies CartResponse;

/** 적립금 이력이 없는 회원 지갑. 공유 객체이므로 spec에서 수정하지 않는다. */
export const EMPTY_REWARDS = {
  availableBalance: 0,
  reservedBalance: 0,
  debtBalance: 0,
  history: [],
} satisfies RewardWalletResponse;

/** 고객 화면 머리글·바닥글이 이름만 읽는 공방 정보의 최소 형태. 연락처·주소를 단정하는 spec은 직접 응답한다. */
export const WORKSHOP = { name: "해피갤러리" } satisfies Pick<WorkshopProfileResponse, "name">;

/**
 * 휴대폰 인증을 마친 비밀번호 회원. 시나리오에 필요한 값만 overrides로 바꾼다.
 * 반환 타입이 overrides를 유지하므로 email·phone을 넘긴 spec은 null 검사 없이 그 값을 입력에 쓸 수 있다.
 */
export function memberFixture<Overrides extends Partial<CustomerUserResponse> = Partial<CustomerUserResponse>>(
  overrides: Overrides = {} as Overrides,
): CustomerUserResponse & Overrides {
  const base: CustomerUserResponse = {
    id: 501,
    name: "회원",
    email: "member@example.com",
    phone: "01012345678",
    phoneVerified: true,
    localPasswordEnabled: true,
  };
  return { ...base, ...overrides };
}

/**
 * 회원 세션 화면이 공통으로 보내는 GET 조회(/me·/me/cart·/me/notifications/unread-count·/me/coupons·/me/rewards·/workshop)에
 * 정적 fixture로 응답하고, 처리했으면 true를 반환한다. member가 null이면 /me는 401, 생략하면 /me는 처리하지 않는다.
 * 요청마다 다른 응답이 필요한 경로는 spec이 이 helper보다 먼저 직접 처리한다.
 */
export async function fulfillMemberCommon(
  route: Route,
  { member }: { member?: CustomerUserResponse | null } = {},
): Promise<boolean> {
  const request = route.request();
  if (request.method() !== "GET") return false;
  switch (new URL(request.url()).pathname) {
    case "/api/v1/me":
      if (member === undefined) return false;
      if (member === null) {
        await fulfillJson(route, UNAUTHORIZED_ERROR, 401);
      } else {
        await fulfillJson(route, member);
      }
      return true;
    case "/api/v1/me/cart":
      await fulfillJson(route, EMPTY_CART);
      return true;
    case "/api/v1/me/notifications/unread-count":
      await fulfillJson(route, { count: 0 } satisfies UnreadCountResponse);
      return true;
    case "/api/v1/me/coupons":
      await fulfillJson(route, [] satisfies MyCouponResponse[]);
      return true;
    case "/api/v1/me/rewards":
      await fulfillJson(route, EMPTY_REWARDS);
      return true;
    case "/api/v1/workshop":
      await fulfillJson(route, WORKSHOP);
      return true;
    default:
      return false;
  }
}

/** 후기가 없는 공개 후기 목록. 공유 객체이므로 spec에서 수정하지 않는다. */
export const EMPTY_REVIEW_PAGE = {
  content: [],
  filteredCount: 0,
  hasMore: false,
  nextCursor: null,
  summary: {
    averageRating: 0,
    reviewCount: 0,
    histogram: { rating1: 0, rating2: 0, rating3: 0, rating4: 0, rating5: 0 },
  },
} satisfies PublicReviewPageResponse;

/** 배송비 3,000원과 2026-08 주문제작 동의 문구. 배송비·동의 문구·버전을 단정하는 spec은 그 값을 넘긴다. */
export function orderPolicyFixture(overrides: Partial<OrderPricePolicyResponse> = {}): OrderPricePolicyResponse {
  return {
    shippingFee: 3000,
    madeToOrderConsentVersion: "2026-08",
    madeToOrderConsentText: "주문제작 동의",
    ...overrides,
  } satisfies OrderPricePolicyResponse;
}

/** 약관·개인정보 처리방침 두 문서에 각각 줄 값 */
type PolicyDocumentValues = Record<keyof CurrentPolicyConsentResponse, string>;

/**
 * 현재 약관·개인정보 처리방침. 기본은 두 문서 모두 2026-07이고 문서 경로는 /terms/<버전>·/privacy/<버전>이다.
 * 문서별 버전이 다르면 version을 { terms, privacy }로, 경로에 버전이 없으면 documentPaths를 넘긴다.
 * policyAcceptance 버전을 단정하는 spec은 버전을 명시한다.
 */
export function currentPoliciesFixture(
  version: string | PolicyDocumentValues = "2026-07",
  documentPaths?: PolicyDocumentValues,
): CurrentPolicyConsentResponse {
  const versions = typeof version === "string" ? { terms: version, privacy: version } : version;
  return {
    terms: { version: versions.terms, documentPath: documentPaths?.terms ?? `/terms/${versions.terms}` },
    privacy: { version: versions.privacy, documentPath: documentPaths?.privacy ?? `/privacy/${versions.privacy}` },
  };
}

/** 옵션 없는 재고 작품 상세. 시나리오에 필요한 값만 overrides로 바꾼다. */
export function productDetailFixture(overrides: Partial<ProductDetailResponse> = {}): ProductDetailResponse {
  return {
    id: 42,
    name: "테스트 작품",
    description: null,
    category: "테스트",
    type: "READY_STOCK",
    price: 12000,
    imageUrl: null,
    available: true,
    stockQuantity: 10,
    specification: null,
    careInstructions: null,
    productionLeadDays: null,
    optionGroups: [],
    variants: [],
    ...overrides,
  } satisfies ProductDetailResponse;
}

/**
 * 이용권 대상이 아닌 운영 중인 가죽 수업(90분·50,000원·정원 8). 시나리오에 필요한 값만 overrides로 바꾼다.
 * 이름·이용권 여부·상황 태그처럼 화면이 보여 주거나 거르는 값은 spec이 넘긴다.
 */
export function classFixture<Overrides extends Partial<ClassResponse> = Partial<ClassResponse>>(
  overrides: Overrides = {} as Overrides,
): ClassResponse & Overrides {
  const base: ClassResponse = {
    id: 42,
    name: "테스트 클래스",
    category: "LEATHER",
    durationMin: 90,
    price: 50000,
    bufferMin: 30,
    capacity: 8,
    passEligible: false,
    description: null,
    imageUrl: null,
    preparationInfo: null,
    targetAudience: null,
    situationTags: [],
    status: "ACTIVE",
  };
  return { ...base, ...overrides };
}

/**
 * 혜택 없이 12,000원을 결제해 배송된 회원 주문. 품목은 비어 있으므로 품목·클레임을 단정하는 spec은 items를 orderItemFixture로 넘긴다.
 * 상태·금액·환불처럼 시나리오를 정하는 값은 기본값에 맡기지 않고 overrides로 넘긴다.
 */
export function orderDetailFixture<Overrides extends Partial<OrderDetailResponse> = Partial<OrderDetailResponse>>(
  overrides: Overrides = {} as Overrides,
): OrderDetailResponse & Overrides {
  const base: OrderDetailResponse = {
    orderId: 701,
    orderNumber: "HG-701",
    status: "DELIVERED",
    totalAmount: 12000,
    productAmount: 12000,
    shippingFee: 0,
    couponDiscountAmount: 0,
    rewardUsedAmount: 0,
    pgPaidAmount: 12000,
    rewardEarnBase: 12000,
    paidAt: "2026-08-01T10:00:00",
    approvalDeadlineAt: null,
    receiptUrl: null,
    fulfillment: null,
    issuedCouponId: null,
    couponStatus: null,
    refund: null,
    items: [],
  };
  return { ...base, ...overrides };
}

/** 옵션 없는 재고 작품을 혜택 없이 12,000원에 1개 산 주문 품목. 금액을 단정하는 spec은 단가와 합계를 함께 넘긴다. */
export function orderItemFixture<Overrides extends Partial<ItemDto> = Partial<ItemDto>>(
  overrides: Overrides = {} as Overrides,
): ItemDto & Overrides {
  const base: ItemDto = {
    orderItemId: 7011,
    productId: 42,
    productName: "테스트 작품",
    productType: "READY_STOCK",
    productVariantId: null,
    qty: 1,
    basePrice: 12000,
    unitPrice: 12000,
    variantPriceAdjustment: 0,
    textOptionPriceAdjustment: 0,
    grossAmount: 12000,
    couponDiscountAmount: 0,
    rewardUsedAmount: 0,
    netPaidAmount: 12000,
    options: [],
    specification: null,
    careInstructions: null,
    productionLeadDays: null,
  };
  return { ...base, ...overrides };
}

/** 마감 전이라 취소와 환불이 모두 가능한 예약 취소 정책. 취소 가능 여부·마감을 단정하는 spec은 그 값을 넘긴다. */
export function bookingCancelPolicyFixture<
  Overrides extends Partial<BookingCancelPolicyResponse> = Partial<BookingCancelPolicyResponse>,
>(overrides: Overrides = {} as Overrides): BookingCancelPolicyResponse & Overrides {
  const base: BookingCancelPolicyResponse = {
    cancellable: true,
    refundable: true,
    deadlineAt: "2099-01-11T00:00:00",
    passCreditRestorable: false,
    manualCompensationRequired: false,
    warningCode: null,
  };
  return { ...base, ...overrides };
}

/**
 * 예약금 10,000원만 낸 1인 회원 예약(BOOKED). 상태·인원·금액·일정·취소 정책처럼 시나리오를 정하는 값은 overrides로 넘긴다.
 * 비회원 상세(BookingDetailResponse)는 이 결과에 bookingNumber·guestName·guestPhone을 더해 만든다.
 */
export function myBookingDetailFixture<Overrides extends Partial<MyBookingDetail> = Partial<MyBookingDetail>>(
  overrides: Overrides = {} as Overrides,
): MyBookingDetail & Overrides {
  const base: MyBookingDetail = {
    bookingId: 7,
    classId: 42,
    className: "테스트 클래스",
    slotId: 70,
    status: "BOOKED",
    startAt: "2099-01-12T10:00:00",
    endAt: "2099-01-12T11:30:00",
    participantCount: 1,
    depositAmount: 10000,
    balanceAmount: 0,
    balanceStatus: "UNPAID",
    passBooking: false,
    receiptUrl: null,
    refund: null,
    cancelPolicy: bookingCancelPolicyFixture(),
  };
  return { ...base, ...overrides };
}

/** 옵션 없는 작품의 대기 중 재입고 알림. 작품 이름·옵션·상태·알림 시각을 단정하는 spec은 그 값을 넘긴다. */
export function restockAlertFixture<Overrides extends Partial<RestockAlertResponse> = Partial<RestockAlertResponse>>(
  overrides: Overrides = {} as Overrides,
): RestockAlertResponse & Overrides {
  const base: RestockAlertResponse = {
    id: 901,
    productId: 42,
    productVariantId: null,
    productName: "테스트 작품",
    optionLabel: "기본 상품",
    status: "WAITING",
    createdAt: "2026-09-05T10:00:00",
    notifiedAt: null,
  };
  return { ...base, ...overrides };
}

/** 웹사이트로 접수된 20명 단체 수업 문의 요약. 기관명·상태·일정·인원처럼 화면이 보여 주는 값은 spec이 넘긴다. */
export function groupInquirySummaryFixture<
  Overrides extends Partial<GroupInquirySummaryResponse> = Partial<GroupInquirySummaryResponse>,
>(overrides: Overrides = {} as Overrides): GroupInquirySummaryResponse & Overrides {
  const base: GroupInquirySummaryResponse = {
    id: 51,
    source: "WEBSITE",
    status: "RECEIVED",
    organization: "단체 수업 기관",
    headcount: 20,
    preferredSchedule: "10월 오전",
    location: "기관 강당",
    classInterest: "레진아트",
    createdAt: "2026-09-05T10:00:00",
  };
  return { ...base, ...overrides };
}

/** 전체 문서를 다시 받지 않고 React Router의 클라이언트 이동으로 path를 연다. */
export async function navigateInApp(page: Page, path: string) {
  await page.evaluate((nextPath) => {
    window.history.pushState({}, "", nextPath);
    window.dispatchEvent(new PopStateEvent("popstate"));
  }, path);
}

/** API 모의 응답을 JSON으로 보낸다. Playwright는 json 값이 falsy이면 content-type을 붙이지 않으므로 직접 지정한다. */
export async function fulfillJson(route: Route, body: unknown, status = 200) {
  await route.fulfill({ status, contentType: "application/json", json: body });
}

/** 고객 변경 요청이 /auth/csrf를 거치지 않고 보낼 CSRF 쿠키를 넣는다. 헤더 값을 단정할 때만 value를 넘긴다. */
export async function installCsrfCookie(
  context: BrowserContext,
  url: string | undefined,
  value = "e2e-csrf",
) {
  if (!url) throw new Error("Playwright baseURL이 필요합니다.");
  await context.addCookies([{ name: "XSRF-TOKEN", value, url }]);
}

/** 다음 문서부터 관리자 토큰이 저장된 상태로 연다. 토큰 값을 단정할 때만 token을 넘긴다. */
export async function seedAdminSession(page: Page, token = MOCK_ADMIN_TOKEN) {
  await page.addInitScript(([key, value]) => {
    sessionStorage.setItem(key, value);
  }, [ADMIN_TOKEN_KEY, token] as const);
}

/** 모의 관리자 세션으로 관리자 화면의 view를 열고 머리글이 보일 때까지 기다린다. */
export async function openMockedAdmin(page: Page, view: string, token?: string) {
  await seedAdminSession(page, token);
  await page.goto(`/admin?view=${view}`);
  await expect(page.getByRole("heading", { name: "관리자", exact: true })).toBeVisible();
}

/** Toss SDK 대역이 requestPayment 요청을 쌓는 전역 이름. 직접 내려주는 SDK 스크립트도 이 이름을 쓴다. */
export const TOSS_REQUESTS_GLOBAL = "tossRequests";

/** 문서를 열 때마다 결제창 대신 requestPayment 요청만 기록하는 Toss SDK 대역을 설치한다. */
export async function installTossRequestRecorder(page: Page) {
  await page.addInitScript((globalName) => {
    const recorded = globalThis as unknown as Record<string, unknown[]>;
    const sdk = globalThis as unknown as { TossPayments: unknown };
    recorded[globalName] = [];
    sdk.TossPayments = () => ({
      payment: () => ({
        requestPayment: async (request: unknown) => {
          recorded[globalName].push(request);
        },
      }),
    });
  }, TOSS_REQUESTS_GLOBAL);
}

/** installTossRequestRecorder가 기록한 requestPayment 요청을 읽는다. */
export function readTossRequests(page: Page): Promise<unknown[]> {
  return page.evaluate(
    (globalName) => (globalThis as unknown as Record<string, unknown[]>)[globalName],
    TOSS_REQUESTS_GLOBAL,
  );
}
