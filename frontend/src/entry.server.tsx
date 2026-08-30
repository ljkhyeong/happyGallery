import { randomBytes } from "node:crypto";
import { PassThrough } from "node:stream";
import type { EntryContext } from "react-router";
import { ServerRouter } from "react-router";
import { createReadableStreamFromReadable } from "@react-router/node";
import { isbot } from "isbot";
import type { RenderToPipeableStreamOptions } from "react-dom/server";
import { renderToPipeableStream } from "react-dom/server";
import { CspNonceContext } from "@/shared/seo/CspJsonLd";

export const streamTimeout = 5_000;

function createNonce(): string {
  return randomBytes(18).toString("base64");
}

function contentSecurityPolicy(nonce: string): string {
  return [
    "default-src 'self'",
    "base-uri 'self'",
    "object-src 'none'",
    "frame-ancestors 'none'",
    "form-action 'self'",
    `script-src 'self' 'report-sample' 'nonce-${nonce}' https://js.tosspayments.com`,
    "script-src-attr 'none'",
    "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com",
    "font-src 'self' data: https://cdn.jsdelivr.net https://fonts.gstatic.com",
    "img-src 'self' data: blob:",
    "connect-src 'self' https://tosspayments.com https://*.tosspayments.com https://*.ingest.sentry.io https://*.ingest.us.sentry.io https://*.ingest.de.sentry.io",
    "frame-src https://tosspayments.com https://*.tosspayments.com",
    "manifest-src 'self'",
    "media-src 'self'",
    "worker-src 'self' blob:",
  ].join("; ");
}

function setSecurityHeaders(headers: Headers, nonce: string): void {
  headers.set("Content-Security-Policy-Report-Only", contentSecurityPolicy(nonce));
  headers.set("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
  headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
  headers.set("X-Content-Type-Options", "nosniff");
  headers.set("X-Frame-Options", "DENY");
}

export default function handleRequest(
  request: Request,
  responseStatusCode: number,
  responseHeaders: Headers,
  routerContext: EntryContext,
) {
  const nonce = createNonce();
  setSecurityHeaders(responseHeaders, nonce);

  if (request.method.toUpperCase() === "HEAD") {
    return new Response(null, {
      status: responseStatusCode,
      headers: responseHeaders,
    });
  }

  return new Promise<Response>((resolve, reject) => {
    let shellRendered = false;
    const userAgent = request.headers.get("user-agent");
    const readyOption: keyof RenderToPipeableStreamOptions =
      (userAgent && isbot(userAgent)) || routerContext.isSpaMode
        ? "onAllReady"
        : "onShellReady";
    let timeoutId: ReturnType<typeof setTimeout> | undefined = setTimeout(
      () => abort(),
      streamTimeout + 1_000,
    );

    const { pipe, abort } = renderToPipeableStream(
      <CspNonceContext value={nonce}>
        <ServerRouter context={routerContext} url={request.url} nonce={nonce} />
      </CspNonceContext>,
      {
        nonce,
        [readyOption]() {
          shellRendered = true;
          const body = new PassThrough({
            final(callback) {
              clearTimeout(timeoutId);
              timeoutId = undefined;
              callback();
            },
          });
          responseHeaders.set("Content-Type", "text/html; charset=utf-8");
          pipe(body);
          resolve(
            new Response(createReadableStreamFromReadable(body), {
              headers: responseHeaders,
              status: responseStatusCode,
            }),
          );
        },
        onShellError(error: unknown) {
          reject(error);
        },
        onError(error: unknown) {
          responseStatusCode = 500;
          if (shellRendered) console.error(error);
        },
      },
    );
  });
}
