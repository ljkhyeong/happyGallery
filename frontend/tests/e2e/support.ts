import { expect, type APIRequestContext, type Locator, type Page } from "@playwright/test";
import type {
  AdminBookingResponse,
  ListBookingsStatus,
} from "../../src/generated/api/adminBooking";
import type {
  BookingDetailResponse,
  ClassResponse,
  MyBookingDetail,
  SendVerificationRequestPurpose,
} from "../../src/generated/api/booking";
import type {
  CreateProductRequest,
  CreateSlotRequest,
  ProductResponse,
  SlotResponse,
} from "../../src/generated/api/adminCatalog";
import type { LoginRequest, LoginResponse } from "../../src/generated/api/adminAuth";
import type {
  AdminOrderListItemResponse,
  AdminOrderPageResponse,
  ListOrdersStatus,
} from "../../src/generated/api/adminOrder";
import type {
  FailedRefundPageResponse,
  FailedRefundResponse,
} from "../../src/generated/api/adminOperations";
import type { SignupRequest } from "../../src/generated/api/customerAuth";
import type { CurrentPolicyConsentResponse } from "../../src/generated/api/policyConsent";

const ADMIN_KEY = process.env.PLAYWRIGHT_ADMIN_KEY ?? "dev-admin-key";
const ADMIN_USERNAME = process.env.PLAYWRIGHT_ADMIN_USERNAME ?? "admin";
const ADMIN_PASSWORD = process.env.PLAYWRIGHT_ADMIN_PASSWORD ?? "admin1234";
const BACKEND_BASE_URL = (process.env.PLAYWRIGHT_BACKEND_URL ?? "http://127.0.0.1:8080/api/v1").replace(/\/$/, "");
const ADMIN_TOKEN_KEY = "hg_admin_token";
const CUSTOMER_SESSION_COOKIE = "HG_SESSION";
const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
const FRONTEND_ORIGIN = "http://127.0.0.1:3000";

let cachedAdminToken: string | null = null;

export type CustomerFixtureCredentials = Pick<
  SignupRequest,
  "email" | "password" | "name" | "phone"
>;

type CustomerFixtureOverrides = Partial<CustomerFixtureCredentials>;

type AdminProductFixtureInput = Pick<
  CreateProductRequest,
  "name" | "price" | "quantity"
> & Partial<Pick<CreateProductRequest, "type">>;

type AdminSlotFixtureInput = Pick<CreateSlotRequest, "classId"> & {
  startAt: Date;
};

interface DevVerificationCodeFixtureResponse {
  code: string;
}

interface ApiOptions {
  admin?: boolean;
  query?: Record<string, number | string | undefined>;
}

interface TossPaymentStubRequest {
  amount: { value: number };
  orderId: string;
  successUrl: string;
}

interface BrowserGlobalWithTossStub {
  location: { assign(url: string): void };
  TossPayments?: (clientKey: string) => {
    payment(opts: { customerKey: string }): {
      requestPayment(opts: TossPaymentStubRequest): Promise<void>;
    };
  };
}

export function makeUniqueLabel(prefix: string): string {
  const stamp = `${Date.now()}${Math.floor(Math.random() * 1000)}`.slice(-9);
  return `${prefix}-${stamp}`;
}

export function makePhoneNumber(seed: string): string {
  const digits = seed.replace(/\D/g, "").slice(-8).padStart(8, "0");
  return `010${digits}`;
}

export function makeEmail(seed: string): string {
  const normalized = seed.toLowerCase().replace(/[^a-z0-9]/g, "").slice(-12) || "member";
  return `${normalized}${Date.now().toString().slice(-6)}@example.com`;
}

async function expectMyPageEmail(page: Page, email: string) {
  await expect(
    page.locator(".my-dashboard-hero").getByText(email, { exact: true }),
  ).toBeVisible();
}

export function plusDays(days: number, hour: number, minute: number, durationMin: number) {
  const start = new Date();
  start.setDate(start.getDate() + days);
  start.setHours(hour, minute, 0, 0);

  const end = new Date(start);
  end.setMinutes(end.getMinutes() + durationMin);

  return { start, end };
}

export async function findUniqueSlotStart(
  request: APIRequestContext,
  classId: number,
  days: number,
  hour: number,
  minute: number,
) {
  const existingSlots = await fetchAdminSlots(request, classId);
  const occupiedStarts = new Set(existingSlots.map((slot) => slot.startAt.slice(0, 16)));

  const start = new Date();
  start.setDate(start.getDate() + days);
  start.setHours(hour, minute, 0, 0);

  let attempts = 0;
  while (occupiedStarts.has(toDateTimeLocalInput(start))) {
    start.setMinutes(start.getMinutes() + 1);
    attempts += 1;
    if (attempts > 180) {
      throw new Error(`Could not find a unique slot start for class=${classId}`);
    }
  }

  return start;
}

export function toDateInput(date: Date): string {
  return [
    date.getFullYear(),
    String(date.getMonth() + 1).padStart(2, "0"),
    String(date.getDate()).padStart(2, "0"),
  ].join("-");
}

export function toDateTimeLocalInput(date: Date): string {
  return `${toDateInput(date)}T${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

export function extractFirstNumber(text: string, label: string): number {
  const escaped = label.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = text.match(new RegExp(`${escaped}\\s*(\\d+)`));
  if (!match) {
    throw new Error(`Could not find numeric value after label: ${label}`);
  }
  return Number(match[1]);
}

export function extractAccessToken(text: string): string {
  const match = text.match(/Access Token:\s*([^\s]+)/);
  if (!match) {
    throw new Error("Could not find access token in success card");
  }
  return match[1];
}

export async function installTossPaymentStub(page: Page) {
  const install = () => {
    const browserGlobal = globalThis as unknown as BrowserGlobalWithTossStub;
    browserGlobal.TossPayments = () => ({
      payment: () => ({
        requestPayment: async (opts: TossPaymentStubRequest) => {
          const separator = opts.successUrl.includes("?") ? "&" : "?";
          const url = `${opts.successUrl}${separator}paymentKey=e2e-${encodeURIComponent(opts.orderId)}&orderId=${encodeURIComponent(opts.orderId)}&amount=${opts.amount.value}`;
          browserGlobal.location.assign(url);
        },
      }),
    });
  };

  await page.addInitScript(install);
  await page.evaluate(install);
}

export async function readRouterState<T>(page: Page): Promise<T | null> {
  return page.evaluate(() => {
    const browserGlobal = globalThis as unknown as {
      history?: { state?: { usr?: unknown } };
    };
    return (browserGlobal.history?.state?.usr ?? null) as T | null;
  });
}

export async function loginAdmin(page: Page) {
  if (!cachedAdminToken) {
    const request: LoginRequest = {
      username: ADMIN_USERNAME,
      password: ADMIN_PASSWORD,
    };
    const response = await page.request.post(`${BACKEND_BASE_URL}/admin/auth/login`, {
      data: request,
    });
    expect(response.ok(), "Admin login API should succeed").toBeTruthy();
    const body = (await response.json()) as LoginResponse;
    if (!body.token) {
      throw new Error("Admin login response did not include a token");
    }
    cachedAdminToken = body.token;
  }

  await page.goto("/admin");
  await page.evaluate(([tokenKey, token]) => {
    const browserGlobal = globalThis as {
      sessionStorage: { setItem(key: string, value: string): void };
    };
    browserGlobal.sessionStorage.setItem(tokenKey, token);
  }, [ADMIN_TOKEN_KEY, cachedAdminToken] as const);
  await page.reload();
  await expect(page.getByRole("heading", { name: "관리자" })).toBeVisible();
}

export async function openAdminView(page: Page, name: string) {
  await page.getByRole("button", { name, exact: true }).click();
}

async function fetchVerificationCode(
  page: Page,
  phone: string,
  purpose: SendVerificationRequestPurpose,
): Promise<string> {
  const res = await page.request.get(
    `${BACKEND_BASE_URL}/admin/dev/phone-verifications/latest?phone=${phone}&purpose=${purpose}`,
    { headers: { "X-Admin-Key": ADMIN_KEY } },
  );
  expect(res.ok(), "Dev phone-verification lookup should succeed").toBeTruthy();
  const body = (await res.json()) as DevVerificationCodeFixtureResponse;
  return body.code;
}

export async function signupCustomer(
  page: Page,
  prefix: string,
  overrides: CustomerFixtureOverrides = {},
): Promise<CustomerFixtureCredentials> {
  const label = makeUniqueLabel(prefix);
  const credentials: CustomerFixtureCredentials = {
    email: overrides.email ?? makeEmail(label),
    password: overrides.password ?? "password123",
    name: overrides.name ?? label,
    phone: (overrides.phone ?? makePhoneNumber(label)).replace(/\D/g, ""),
  };

  const csrfHeaders = await issueCustomerCsrfHeaders(page);
  const verificationResponse = await page.request.post(
    `${BACKEND_BASE_URL}/bookings/phone-verifications`,
    {
      data: { phone: credentials.phone, purpose: "SIGNUP" },
      headers: csrfHeaders,
    },
  );
  expect(verificationResponse.ok(), "Phone verification send API should succeed").toBeTruthy();
  const verificationCode = await fetchVerificationCode(page, credentials.phone, "SIGNUP");
  const policyResponse = await page.request.get(`${BACKEND_BASE_URL}/policies/current`);
  expect(policyResponse.ok(), "Current policy API should succeed").toBeTruthy();
  const policy = (await policyResponse.json()) as CurrentPolicyConsentResponse;
  const signupRequest: SignupRequest = {
    ...credentials,
    verificationCode,
    policyAcceptance: {
      termsVersion: policy.terms.version,
      termsAccepted: true,
      privacyVersion: policy.privacy.version,
      privacyAccepted: true,
    },
  };
  const response = await page.request.post(`${BACKEND_BASE_URL}/auth/signup`, {
    data: signupRequest,
    headers: csrfHeaders,
  });
  expect(response.ok(), "Customer signup API should succeed").toBeTruthy();
  await setCustomerSessionFromResponse(page, response.headers()["set-cookie"]);
  await page.goto("/my");
  await expectMyPageEmail(page, credentials.email);
  return credentials;
}

export async function loginCustomer(page: Page, credentials: CustomerFixtureCredentials) {
  const csrfHeaders = await issueCustomerCsrfHeaders(page);
  const response = await page.request.post(`${BACKEND_BASE_URL}/auth/login`, {
    data: { email: credentials.email, password: credentials.password },
    headers: csrfHeaders,
  });
  expect(response.ok(), "Customer login API should succeed").toBeTruthy();
  await setCustomerSessionFromResponse(page, response.headers()["set-cookie"]);
  await page.goto("/my");
  await expectMyPageEmail(page, credentials.email);
}

export async function logoutCustomer(page: Page) {
  const logoutButton = page.getByRole("button", { name: /로그아웃|LOGOUT/ }).first();
  if (await logoutButton.isVisible()) {
    await logoutButton.click();
  }
}

export async function completePhoneVerification(
  page: Page,
  phone: string,
  purpose: SendVerificationRequestPurpose = "GUEST_ORDER",
) {
  await page.getByLabel("휴대폰 번호").fill(phone);
  await page.getByRole("button", { name: "인증코드 발송" }).click();
  await expect(page.getByLabel("인증코드")).toBeVisible();
  const code = await fetchVerificationCode(page, phone, purpose);
  await page.getByLabel("인증코드").fill(code);
  await page.getByRole("button", { name: "확인" }).click();
}

export async function completeLockedPhoneVerification(
  root: Page | Locator,
  page: Page,
  phone: string,
  purpose: SendVerificationRequestPurpose,
  confirmLabel = "확인",
) {
  await root.getByRole("button", { name: "인증코드 발송" }).click();
  await expect(root.getByLabel("인증코드")).toBeVisible();
  const code = await fetchVerificationCode(page, phone, purpose);
  await root.getByLabel("인증코드").fill(code);
  await root.getByRole("button", { name: confirmLabel }).click();
}

export async function completeGuestAuthGate(page: Page, phone: string, name: string) {
  await page.locator(".nav-link").filter({ hasText: "비회원" }).first().click();
  await completePhoneVerification(page, phone, "GUEST_BOOKING");
  await page.locator("#gate-guest-name").fill(name);
  await acceptCurrentPolicies(page);
  await page.getByRole("button", { name: "비회원으로 진행" }).click();
}

export async function acceptCurrentPolicies(page: Page) {
  await page.getByRole("checkbox", {
    name: /이용약관.*개인정보처리방침/,
  }).check();
}

async function setCustomerSessionFromResponse(page: Page, setCookieHeader?: string) {
  const match = setCookieHeader?.match(/HG_SESSION=([^;]+)/);
  if (!match) {
    throw new Error("Could not extract HG_SESSION cookie from auth response");
  }

  await page.context().addCookies([
    {
      name: CUSTOMER_SESSION_COOKIE,
      value: match[1]!,
      url: FRONTEND_ORIGIN,
      httpOnly: true,
    },
  ]);
}

async function issueCustomerCsrfHeaders(page: Page): Promise<Record<string, string>> {
  const response = await page.request.get(`${BACKEND_BASE_URL}/auth/csrf`);
  expect(response.ok(), "CSRF token API should succeed").toBeTruthy();

  const csrfCookie = (await page.context().cookies(BACKEND_BASE_URL))
    .find((cookie) => cookie.name === CSRF_COOKIE_NAME);
  if (!csrfCookie) {
    throw new Error("Could not find XSRF-TOKEN cookie from CSRF response");
  }
  return { [CSRF_HEADER_NAME]: csrfCookie.value };
}

export function adminCard(page: Page, title: string): Locator {
  return page.locator(".admin-workspace-panel, .card").filter({ hasText: title }).first();
}

export async function apiGet<T>(
  request: APIRequestContext,
  path: string,
  options: ApiOptions = {},
): Promise<T> {
  const url = new URL(`${BACKEND_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`);
  for (const [key, value] of Object.entries(options.query ?? {})) {
    if (value !== undefined) {
      url.searchParams.set(key, String(value));
    }
  }

  const response = await request.get(url.toString(), {
    headers: options.admin ? { "X-Admin-Key": ADMIN_KEY } : undefined,
  });
  expect(response.ok(), `GET ${url} should succeed`).toBeTruthy();
  return (await response.json()) as T;
}

export async function apiPost<T>(
  request: APIRequestContext,
  path: string,
  body?: unknown,
  options: ApiOptions = {},
): Promise<T | undefined> {
  const url = new URL(`${BACKEND_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`);

  const response = await request.post(url.toString(), {
    data: body,
    headers: options.admin ? { "X-Admin-Key": ADMIN_KEY } : undefined,
  });
  expect(response.ok(), `POST ${url} should succeed`).toBeTruthy();

  if (response.status() === 204) {
    return undefined;
  }
  return (await response.json()) as T;
}

export async function apiDelete(
  request: APIRequestContext,
  path: string,
  options: ApiOptions = {},
): Promise<void> {
  const url = new URL(`${BACKEND_BASE_URL}${path.startsWith("/") ? path : `/${path}`}`);

  const response = await request.delete(url.toString(), {
    headers: options.admin ? { "X-Admin-Key": ADMIN_KEY } : undefined,
  });
  expect(response.ok(), `DELETE ${url} should succeed`).toBeTruthy();
}

export async function fetchClasses(request: APIRequestContext): Promise<ClassResponse[]> {
  return apiGet<ClassResponse[]>(request, "/classes");
}

export async function fetchGuestBookingSlot(
  request: APIRequestContext,
  bookingId: number,
  token: string,
): Promise<Pick<BookingDetailResponse, "slotId">> {
  const response = await request.get(`${BACKEND_BASE_URL}/bookings/${bookingId}`, {
    headers: { "X-Access-Token": token },
  });
  expect(response.ok(), "Guest booking lookup API should succeed").toBeTruthy();
  return (await response.json()) as Pick<BookingDetailResponse, "slotId">;
}

export async function fetchMyBookingSlot(
  page: Page,
  bookingId: number,
): Promise<Pick<MyBookingDetail, "slotId">> {
  const response = await page.request.get(`${BACKEND_BASE_URL}/me/bookings/${bookingId}`);
  expect(response.ok(), "Member booking lookup API should succeed").toBeTruthy();
  return (await response.json()) as Pick<MyBookingDetail, "slotId">;
}

export async function fetchAdminProducts(request: APIRequestContext): Promise<ProductResponse[]> {
  return apiGet<ProductResponse[]>(request, "/admin/products", { admin: true });
}

export async function createAdminProduct(
  request: APIRequestContext,
  body: AdminProductFixtureInput,
): Promise<ProductResponse> {
  const product = await apiPost<ProductResponse>(
    request,
    "/admin/products",
    {
      name: body.name,
      type: body.type ?? "READY_STOCK",
      price: body.price,
      quantity: body.quantity,
    },
    { admin: true },
  );
  if (!product) {
    throw new Error("Admin product create response was empty");
  }
  return product;
}

export async function fetchAdminSlots(request: APIRequestContext, classId: number): Promise<SlotResponse[]> {
  return apiGet<SlotResponse[]>(request, "/admin/slots", { admin: true, query: { classId } });
}

export async function createAdminSlot(
  request: APIRequestContext,
  body: AdminSlotFixtureInput,
): Promise<SlotResponse> {
  const slot = await apiPost<SlotResponse>(
    request,
    "/admin/slots",
    {
      classId: body.classId,
      startAt: toDateTimeLocalInput(body.startAt),
    },
    { admin: true },
  );
  if (!slot) {
    throw new Error("Admin slot create response was empty");
  }
  return slot;
}

export async function fetchAdminBookings(
  request: APIRequestContext,
  date: string,
  status?: ListBookingsStatus,
): Promise<AdminBookingResponse[]> {
  return apiGet<AdminBookingResponse[]>(request, "/admin/bookings", {
    admin: true,
    query: { date, status },
  });
}

export async function fetchAdminOrders(
  request: APIRequestContext,
  status?: ListOrdersStatus,
): Promise<AdminOrderListItemResponse[]> {
  const page = await apiGet<AdminOrderPageResponse>(request, "/admin/orders", {
    admin: true,
    query: { status },
  });
  return page.content;
}

export async function fetchFailedRefunds(request: APIRequestContext): Promise<FailedRefundResponse[]> {
  const page = await apiGet<FailedRefundPageResponse>(
    request,
    "/admin/refunds/failed",
    { admin: true },
  );
  return page.content;
}

export async function armNextRefundFailure(
  request: APIRequestContext,
  reason: string,
): Promise<void> {
  await apiPost(request, "/admin/dev/payment/refunds/fail-next", { reason }, { admin: true });
}

export async function clearNextRefundFailure(request: APIRequestContext): Promise<void> {
  await apiDelete(request, "/admin/dev/payment/refunds/fail-next", { admin: true });
}

export async function waitForProduct(
  request: APIRequestContext,
  name: string,
): Promise<ProductResponse> {
  await expect.poll(async () => {
    const products = await fetchAdminProducts(request);
    return products.some((product) => product.name === name);
  }).toBeTruthy();

  const products = await fetchAdminProducts(request);
  const product = products.find((item) => item.name === name);
  if (!product) {
    throw new Error(`Could not find product: ${name}`);
  }
  return product;
}

export async function waitForSlot(
  request: APIRequestContext,
  classId: number,
  startAtPrefix: string,
): Promise<SlotResponse> {
  await expect.poll(async () => {
    const slots = await fetchAdminSlots(request, classId);
    return slots.some((slot) => slot.startAt.startsWith(startAtPrefix));
  }).toBeTruthy();

  const slots = await fetchAdminSlots(request, classId);
  const slot = slots.find((item) => item.startAt.startsWith(startAtPrefix));
  if (!slot) {
    throw new Error(`Could not find slot for class=${classId}, start=${startAtPrefix}`);
  }
  return slot;
}

export async function waitForBookingByPhone(
  request: APIRequestContext,
  date: string,
  phone: string,
): Promise<AdminBookingResponse> {
  await expect.poll(async () => {
    const bookings = await fetchAdminBookings(request, date);
    return bookings.some((booking) => booking.customerSummary.phone === phone);
  }).toBeTruthy();

  const bookings = await fetchAdminBookings(request, date);
  const booking = bookings.find((item) => item.customerSummary.phone === phone);
  if (!booking) {
    throw new Error(`Could not find booking for phone: ${phone}`);
  }
  return booking;
}

export async function waitForOrder(
  request: APIRequestContext,
  orderId: number,
  status?: ListOrdersStatus,
): Promise<AdminOrderListItemResponse> {
  await expect.poll(async () => {
    const orders = await fetchAdminOrders(request, status);
    return orders.some((order) => order.orderId === orderId);
  }).toBeTruthy();

  const orders = await fetchAdminOrders(request, status);
  const order = orders.find((item) => item.orderId === orderId);
  if (!order) {
    throw new Error(`Could not find order: ${orderId}`);
  }
  return order;
}

export async function waitForFailedRefundByOrderId(
  request: APIRequestContext,
  orderId: number,
): Promise<FailedRefundResponse> {
  await expect.poll(async () => {
    const refunds = await fetchFailedRefunds(request);
    return refunds.some((refund) => refund.orderId === orderId);
  }).toBeTruthy();

  const refunds = await fetchFailedRefunds(request);
  const refund = refunds.find((item) => item.orderId === orderId);
  if (!refund) {
    throw new Error(`Could not find failed refund for order: ${orderId}`);
  }
  return refund;
}

export async function waitForFailedRefundGone(
  request: APIRequestContext,
  refundId: number,
): Promise<void> {
  await expect.poll(async () => {
    const refunds = await fetchFailedRefunds(request);
    return refunds.every((refund) => refund.refundId !== refundId);
  }).toBeTruthy();
}
