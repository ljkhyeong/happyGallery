import { useContext, useState, type ReactNode } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import {
  Links,
  Meta,
  Scripts,
  ScrollRestoration,
  useRouteError,
} from "react-router";
import { CustomerAuthProvider } from "@/features/customer-auth/useCustomerAuth";
import { CartProvider } from "@/features/cart/CartProvider";
import { DEFAULT_NOT_FOUND } from "@/pages/NotFoundPage";
import { createQueryClient } from "@/shared/api";
import {
  ErrorBoundary as AppErrorBoundary,
  Layout as AppLayout,
  isNotFoundRouteError,
  RouteLoadErrorStatus,
  StatusPage,
  ToastProvider,
} from "@/shared/ui";
import { CspNonceContext } from "@/shared/seo/CspJsonLd";
import { DEFAULT_SEO_DESCRIPTION, DEFAULT_SEO_TITLE, SITE_NAME } from "@/shared/seo/metadata";
import "@/styles/global.scss";

// Styles are host-allowlisted; browsers hide link nonces and would report a hydration mismatch.
const LINK_NONCE_DISABLED = "";

export function meta() {
  return [
    { title: DEFAULT_SEO_TITLE },
    { name: "description", content: DEFAULT_SEO_DESCRIPTION },
    { property: "og:locale", content: "ko_KR" },
    { property: "og:site_name", content: SITE_NAME },
    { name: "twitter:card", content: "summary_large_image" },
  ];
}

export function links() {
  return [
    { rel: "preconnect", href: "https://cdn.jsdelivr.net", crossOrigin: "anonymous" },
    {
      rel: "stylesheet",
      href: "https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css",
    },
    // 제목용 명조(Hahmlet). 한글은 Google Fonts가 글자 범위별로 나눠 필요한 조각만 받는다.
    { rel: "preconnect", href: "https://fonts.googleapis.com" },
    { rel: "preconnect", href: "https://fonts.gstatic.com", crossOrigin: "anonymous" },
    {
      rel: "stylesheet",
      href: "https://fonts.googleapis.com/css2?family=Hahmlet:wght@500;600;700&display=swap",
    },
  ];
}

export function Layout({ children }: { children: ReactNode }) {
  const nonce = useContext(CspNonceContext);
  return (
    <html lang="ko">
      <head>
        <meta charSet="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1" />
        <meta name="theme-color" content="#FFFFFF" />
        <Meta />
        <Links nonce={LINK_NONCE_DISABLED} />
      </head>
      <body>
        {children}
        <ScrollRestoration nonce={nonce} />
        <Scripts nonce={nonce} />
      </body>
    </html>
  );
}

export default function Root() {
  const [queryClient] = useState(createQueryClient);

  return (
    <AppErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <CustomerAuthProvider>
          <ToastProvider>
            <CartProvider>
              <AppLayout />
            </CartProvider>
          </ToastProvider>
        </CustomerAuthProvider>
      </QueryClientProvider>
    </AppErrorBoundary>
  );
}

export function ErrorBoundary() {
  const error = useRouteError();

  // 루트 오류는 머리글 없이 그려지므로 공방 이름으로 홈 링크를 함께 보여 준다.
  // 404의 홈 링크도 남은 오류 상태를 비우도록 전체 페이지 이동(a href)으로 연다.
  return isNotFoundRouteError(error) ? (
    <StatusPage
      standalone
      title={DEFAULT_NOT_FOUND.title}
      description={DEFAULT_NOT_FOUND.description}
      actions={<a href="/" className="btn btn-primary">홈으로</a>}
    />
  ) : (
    <RouteLoadErrorStatus standalone />
  );
}
