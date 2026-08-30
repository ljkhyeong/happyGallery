export function sanitizeTelemetryUrl(url: string, origin: string): string {
  try {
    const parsed = new URL(url, origin);
    parsed.search = "";
    parsed.hash = "";
    return parsed.toString();
  } catch {
    return url.replace(/[?#].*$/, "");
  }
}

export function sanitizeTelemetryPath(url: string, origin: string): string {
  try {
    return new URL(url, origin).pathname;
  } catch {
    return url.replace(/[?#].*$/, "");
  }
}

const SENSITIVE_HEADERS = new Set([
  "authorization",
  "cookie",
  "x-access-token",
  "x-payment-status-token",
  "x-xsrf-token",
]);
const REFERRER_HEADERS = new Set(["referer", "referrer"]);

export function sanitizeTelemetryHeaders(
  headers: Record<string, string>,
  origin: string,
): Record<string, string> {
  return Object.fromEntries(
    Object.entries(headers).flatMap(([name, value]) => {
      const normalizedName = name.toLowerCase();
      if (SENSITIVE_HEADERS.has(normalizedName)) {
        return [];
      }
      if (REFERRER_HEADERS.has(normalizedName)) {
        return [[name, sanitizeTelemetryUrl(value, origin)]];
      }
      return [[name, value]];
    }),
  );
}
