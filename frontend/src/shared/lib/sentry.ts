import * as Sentry from "@sentry/react";
import {
  sanitizeTelemetryHeaders,
  sanitizeTelemetryUrl,
} from "@/shared/lib/sentryUrl";

const dsn = import.meta.env.VITE_SENTRY_DSN?.trim();
const environment = import.meta.env.VITE_SENTRY_ENVIRONMENT?.trim() || import.meta.env.MODE;
const release = import.meta.env.VITE_SENTRY_RELEASE?.trim() || undefined;

export function initSentry() {
  if (!dsn) return;

  Sentry.init({
    dsn,
    environment,
    release,
    tracesSampleRate: 0.1,
    // Sentry 11은 dataCollection을 비워 두면 IP·쿠키·본문까지 수집한다. v10의 sendDefaultPii: false 기준을 명시하고 URL query는 보내지 않는다.
    dataCollection: {
      userInfo: false,
      cookies: false,
      httpHeaders: { deny: ["forwarded", "-ip", "remote-", "via", "-user"] },
      httpBodies: [],
      urlQueryParams: false,
      genAI: { inputs: false, outputs: false },
      databaseQueryData: false,
      queues: false,
      graphQL: { document: false, variables: false },
    },
    beforeSend(event) {
      const request = event.request;
      if (request?.url) {
        request.url = sanitizeTelemetryUrl(request.url, window.location.origin);
      }
      if (request?.headers) {
        request.headers = sanitizeTelemetryHeaders(
          request.headers,
          window.location.origin,
        );
      }
      if (request) {
        delete request.query_string;
        delete request.cookies;
        delete request.data;
      }
      return event;
    },
    beforeBreadcrumb(breadcrumb) {
      if (breadcrumb.data?.url && typeof breadcrumb.data.url === "string") {
        breadcrumb.data.url = sanitizeTelemetryUrl(
          breadcrumb.data.url,
          window.location.origin,
        );
      }
      if (breadcrumb.data?.from && typeof breadcrumb.data.from === "string") {
        breadcrumb.data.from = sanitizeTelemetryUrl(
          breadcrumb.data.from,
          window.location.origin,
        );
      }
      if (breadcrumb.data?.to && typeof breadcrumb.data.to === "string") {
        breadcrumb.data.to = sanitizeTelemetryUrl(
          breadcrumb.data.to,
          window.location.origin,
        );
      }
      return breadcrumb;
    },
  });
}
