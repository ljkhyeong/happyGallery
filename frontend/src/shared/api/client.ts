import * as Sentry from "@sentry/react";
import { ApiError } from "@/shared/api/error";
import type { ErrorResponse } from "@/shared/types/error";

const BASE_URL = "/api/v1";
const REQUEST_TIMEOUT_MS = 35_000;
const CSRF_COOKIE_NAME = "XSRF-TOKEN";
const CSRF_HEADER_NAME = "X-XSRF-TOKEN";
const SAFE_METHODS = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);

let csrfTokenRequest: Promise<string> | undefined;

interface RequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
  params?: Record<string, string | number | undefined>;
}

function buildUrl(path: string, params?: Record<string, string | number | undefined>): string {
  const url = new URL(`${BASE_URL}${path}`, window.location.origin);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined) {
        url.searchParams.set(key, String(value));
      }
    }
  }
  return url.toString();
}

function readCookie(name: string): string | undefined {
  const prefix = `${encodeURIComponent(name)}=`;
  const cookie = document.cookie
    .split(";")
    .map((value) => value.trim())
    .find((value) => value.startsWith(prefix));
  return cookie ? decodeURIComponent(cookie.slice(prefix.length)) : undefined;
}

async function getCsrfToken(signal: AbortSignal): Promise<string> {
  const cookieToken = readCookie(CSRF_COOKIE_NAME);
  if (cookieToken) return cookieToken;

  csrfTokenRequest ??= fetch(buildUrl("/auth/csrf"), {
    cache: "no-store",
    credentials: "include",
    signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(`CSRF token request failed: ${response.status}`);
      }
      await response.json();
      const issuedToken = readCookie(CSRF_COOKIE_NAME);
      if (!issuedToken) {
        throw new Error("CSRF token cookie was not issued");
      }
      return issuedToken;
    })
    .finally(() => {
      csrfTokenRequest = undefined;
    });

  return csrfTokenRequest;
}

function requiresCsrf(path: string, method: string | undefined): boolean {
  const normalizedMethod = (method ?? "GET").toUpperCase();
  const adminRequest = path === "/admin" || path.startsWith("/admin/");
  return !adminRequest && !SAFE_METHODS.has(normalizedMethod);
}

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, params, headers: customHeaders, ...rest } = options;
  const headers = new Headers(customHeaders);
  const multipartBody = body instanceof FormData;

  if (body !== undefined && !multipartBody) {
    headers.set("Content-Type", "application/json");
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  let response: Response;
  try {
    if (requiresCsrf(path, rest.method)) {
      headers.set(CSRF_HEADER_NAME, await getCsrfToken(controller.signal));
    }

    response = await fetch(buildUrl(path, params), {
      ...rest,
      headers,
      body: body === undefined ? undefined : multipartBody ? body : JSON.stringify(body),
      signal: controller.signal,
      credentials: "include",
    });
  } finally {
    clearTimeout(timeoutId);
  }

  if (!response.ok) {
    let errorBody: ErrorResponse | undefined;
    try {
      errorBody = (await response.json()) as ErrorResponse;
    } catch {
      // non-JSON error
    }
    const error = new ApiError(
      response.status,
      errorBody?.code ?? "UNKNOWN",
      errorBody?.message ?? response.statusText,
      errorBody?.requestId,
    );
    if (response.status >= 500) {
      Sentry.withScope((scope) => {
        if (error.requestId) scope.setTag("requestId", error.requestId);
        scope.setTag("api.path", path);
        scope.setTag("api.status", response.status);
        Sentry.captureException(error);
      });
    }
    throw error;
  }

  if (response.status === 204 || response.headers.get("content-length") === "0") {
    return undefined as T;
  }

  return (await response.json()) as T;
}
