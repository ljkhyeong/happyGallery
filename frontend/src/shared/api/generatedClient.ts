import { api } from "@/shared/api/client";

const API_PREFIX = "/api/v1";

interface GeneratedRequestConfig extends RequestInit {
  data?: unknown;
  params?: Record<string, string | number | undefined>;
}

export async function generatedApiClient<T>(
  url: string,
  { body, data, ...options }: GeneratedRequestConfig,
): Promise<T> {
  const path = url.startsWith(API_PREFIX) ? url.slice(API_PREFIX.length) : url;
  return api<T>(path, {
    ...options,
    ...(body === undefined ? { body: data } : { rawBody: body }),
  });
}
