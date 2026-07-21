import * as Sentry from "@sentry/react";

const dsn = import.meta.env.VITE_SENTRY_DSN?.trim();
const environment = import.meta.env.VITE_SENTRY_ENVIRONMENT?.trim() || import.meta.env.MODE;
const release = import.meta.env.VITE_SENTRY_RELEASE?.trim() || undefined;

function removeSensitiveQuery(url: string): string {
  try {
    const parsed = new URL(url, window.location.origin);
    if (parsed.pathname === "/payments/success" || parsed.pathname === "/payments/fail") {
      parsed.search = "";
    }
    return parsed.toString();
  } catch {
    return url;
  }
}

export function initSentry() {
  if (!dsn) return;

  Sentry.init({
    dsn,
    environment,
    release,
    tracesSampleRate: 0.1,
    sendDefaultPii: false,
    beforeSend(event) {
      if (event.request?.url) {
        event.request.url = removeSensitiveQuery(event.request.url);
      }
      return event;
    },
    beforeBreadcrumb(breadcrumb) {
      if (breadcrumb.data?.url && typeof breadcrumb.data.url === "string") {
        breadcrumb.data.url = removeSensitiveQuery(breadcrumb.data.url);
      }
      if (breadcrumb.data?.from && typeof breadcrumb.data.from === "string") {
        breadcrumb.data.from = removeSensitiveQuery(breadcrumb.data.from);
      }
      if (breadcrumb.data?.to && typeof breadcrumb.data.to === "string") {
        breadcrumb.data.to = removeSensitiveQuery(breadcrumb.data.to);
      }
      return breadcrumb;
    },
  });
}
