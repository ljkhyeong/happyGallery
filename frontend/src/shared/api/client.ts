import * as Sentry from "@sentry/react";
import { ApiError } from "@/shared/api/error";
import type { ErrorResponse } from "@/shared/types/error";

const BASE_URL = "/api/v1";
const REQUEST_TIMEOUT_MS = 15_000;

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

export async function api<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, params, headers: customHeaders, ...rest } = options;

  const headers: Record<string, string> = {
    ...Object.fromEntries(Object.entries(customHeaders ?? {}).map(([k, v]) => [k, String(v)])),
  };

  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  let response: Response;
  try {
    response = await fetch(buildUrl(path, params), {
      ...rest,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: controller.signal,
      credentials: 'include',
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
