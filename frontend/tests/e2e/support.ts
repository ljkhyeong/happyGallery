import { expect, type APIRequestContext, type Locator, type Page } from "@playwright/test";

const ADMIN_KEY = process.env.PLAYWRIGHT_ADMIN_KEY ?? "dev-admin-key";
const ADMIN_USERNAME = process.env.PLAYWRIGHT_ADMIN_USERNAME ?? "admin";
const ADMIN_PASSWORD = process.env.PLAYWRIGHT_ADMIN_PASSWORD ?? "admin1234";
const BACKEND_BASE_URL = (process.env.PLAYWRIGHT_BACKEND_URL ?? "http://127.0.0.1:8080/api/v1").replace(/\/$/, "");
const ADMIN_TOKEN_KEY = "hg_admin_token";
const CUSTOMER_SESSION_COOKIE = "HG_SESSION";
const FRONTEND_ORIGIN = "http://127.0.0.1:3000";

let cachedAdminToken: string | null = null;

export interface BookingClass {
  id: number;
  name: string;
  category: string;
  durationMin: number;
}

export interface AdminProduct {
  id: number;
  name: string;
  type: string;
  price: number;
  quantity: number;
  available: boolean;
}

export interface AdminSlot {
  id: number;
  classId: number;
  startAt: string;
  endAt: string;
  capacity: number;
  bookedCount: number;
  isActive: boolean;
}

export interface AdminBooking {
  bookingId: number;
  bookingNumber: string;
  guestName: string;
  guestPhone: string;
  className: string;
  startAt: string;
  endAt: string;
  status: string;
  depositAmount: number;
  balanceAmount: number;
  passBooking: boolean;
}

export interface AdminOrder {
  orderId: number;
  orderNumber: string;
  status: string;
  totalAmount: number;
  paidAt: string | null;
  approvalDeadlineAt: string | null;
  createdAt: string;
}

export interface AdminFailedRefund {
  refundId: number;
  bookingId: number | null;
  orderId: number | null;
  amount: number;
  failReason: string;
  createdAt: string;
}

export interface CustomerCredentials {
  email: string;
  password: string;
  name: string;
  phone: string;
}

interface ApiOptions {
  admin?: boolean;
  query?: Record<string, number | string | undefined>;
}

const timeFormatter = new Intl.DateTimeFormat("en-US", {
  hour: "2-digit",
  minute: "2-digit",
  timeZone: "Asia/Seoul",
  hour12: true,
});

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

export function plusDays(days: number, hour: number, minute: number, durationMin: number) {
  const start = new Date();
  start.setDate(start.getDate() + days);
  start.setHours(hour, minute, 0, 0);

  const end = new Date(start);
  end.setMinutes(end.getMinutes() + durationMin);

  return { start, end };
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

export function formatTimeTokenForUi(iso: string): string {
  const parts = timeFormatter.formatToParts(new Date(iso));
  const hour = parts.find((part) => part.type === "hour")?.value;
  const minute = parts.find((part) => part.type === "minute")?.value;
  if (!hour || !minute) {
    throw new Error(`Could not format UI time token for ${iso}`);
  }
  return `${hour}:${minute}`;
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

export async function loginAdmin(page: Page) {
  if (!cachedAdminToken) {
    const response = await page.request.post(`${BACKEND_BASE_URL}/admin/auth/login`, {
      data: { username: ADMIN_USERNAME, password: ADMIN_PASSWORD },
    });
    expect(response.ok(), "Admin login API should succeed").toBeTruthy();
    const body = (await response.json()) as { token: string };
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

export async function signupCustomer(page: Page, prefix: string): Promise<CustomerCredentials> {
  const label = makeUniqueLabel(prefix);
  const credentials: CustomerCredentials = {
    email: makeEmail(label),
    password: "password123",
    name: label,
    phone: makePhoneNumber(label),
  };

  const response = await page.request.post(`${BACKEND_BASE_URL}/auth/signup`, {
    data: credentials,
  });
  expect(response.ok(), "Customer signup API should succeed").toBeTruthy();
  await setCustomerSessionFromResponse(page, response.headers()["set-cookie"]);
  await page.goto("/my");
  await expect(page.getByText(credentials.email)).toBeVisible();
  return credentials;
}

export async function loginCustomer(page: Page, credentials: CustomerCredentials) {
  const response = await page.request.post(`${BACKEND_BASE_URL}/auth/login`, {
    data: { email: credentials.email, password: credentials.password },
  });
  expect(response.ok(), "Customer login API should succeed").toBeTruthy();
  await setCustomerSessionFromResponse(page, response.headers()["set-cookie"]);
  await page.goto("/my");
  await expect(page.getByText(credentials.email)).toBeVisible();
}

export async function logoutCustomer(page: Page) {
  const logoutButton = page.getByRole("button", { name: "로그아웃" }).first();
  if (await logoutButton.isVisible()) {
    await logoutButton.click();
  }
}

export async function completePhoneVerification(page: Page, phone: string) {
  await page.getByLabel("휴대폰 번호").fill(phone);
  await page.getByRole("button", { name: "인증코드 발송" }).click();

  const codeHint = page.locator("p").filter({ hasText: "[MVP] 인증코드:" }).first();
  await expect(codeHint).toBeVisible();
  const raw = await codeHint.textContent();
  const code = raw?.match(/인증코드:\s*(\S+)/)?.[1];
  if (!code) {
    throw new Error("Could not parse MVP verification code");
  }

  await page.getByLabel("인증코드").fill(code);
  await page.getByRole("button", { name: "확인" }).click();
}

export async function completeGuestAuthGate(page: Page, phone: string, name: string) {
  await page.locator(".nav-link").filter({ hasText: "비회원" }).first().click();
  await completePhoneVerification(page, phone);
  await page.locator("#gate-guest-name").fill(name);
  await page.getByRole("button", { name: "비회원으로 진행" }).click();
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

export function adminCard(page: Page, title: string): Locator {
  return page.locator(".card").filter({ hasText: title }).first();
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

export async function fetchClasses(request: APIRequestContext): Promise<BookingClass[]> {
  return apiGet<BookingClass[]>(request, "/classes");
}

export async function fetchAdminProducts(request: APIRequestContext): Promise<AdminProduct[]> {
  return apiGet<AdminProduct[]>(request, "/admin/products", { admin: true });
}

export async function fetchAdminSlots(request: APIRequestContext, classId: number): Promise<AdminSlot[]> {
  return apiGet<AdminSlot[]>(request, "/admin/slots", { admin: true, query: { classId } });
}

export async function fetchAdminBookings(
  request: APIRequestContext,
  date: string,
  status?: string,
): Promise<AdminBooking[]> {
  return apiGet<AdminBooking[]>(request, "/admin/bookings", {
    admin: true,
    query: { date, status },
  });
}

export async function fetchAdminOrders(
  request: APIRequestContext,
  status?: string,
): Promise<AdminOrder[]> {
  return apiGet<AdminOrder[]>(request, "/admin/orders", {
    admin: true,
    query: { status },
  });
}

export async function fetchFailedRefunds(request: APIRequestContext): Promise<AdminFailedRefund[]> {
  return apiGet<AdminFailedRefund[]>(request, "/admin/refunds/failed", { admin: true });
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
): Promise<AdminProduct> {
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
): Promise<AdminSlot> {
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
): Promise<AdminBooking> {
  await expect.poll(async () => {
    const bookings = await fetchAdminBookings(request, date);
    return bookings.some((booking) => booking.guestPhone === phone);
  }).toBeTruthy();

  const bookings = await fetchAdminBookings(request, date);
  const booking = bookings.find((item) => item.guestPhone === phone);
  if (!booking) {
    throw new Error(`Could not find booking for phone: ${phone}`);
  }
  return booking;
}

export async function waitForOrder(
  request: APIRequestContext,
  orderId: number,
  status?: string,
): Promise<AdminOrder> {
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
): Promise<AdminFailedRefund> {
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
