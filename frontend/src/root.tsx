import { useContext, useState, type ReactNode } from "react";
import { QueryClientProvider } from "@tanstack/react-query";
import {
  Links,
  Meta,
  Scripts,
  ScrollRestoration,
  isRouteErrorResponse,
  useRouteError,
} from "react-router";
import { Container } from "react-bootstrap";
import { CustomerAuthProvider } from "@/features/customer-auth/useCustomerAuth";
import { CartProvider } from "@/features/cart/CartProvider";
import { createQueryClient } from "@/shared/api";
import {
  ErrorBoundary as AppErrorBoundary,
  Layout as AppLayout,
  ToastProvider,
} from "@/shared/ui";
import { CspNonceContext } from "@/shared/seo/CspJsonLd";
import "@/styles/global.scss";

const DEFAULT_TITLE = "해피갤러리 | 충주 공예 클래스와 핸드메이드 공방";
const DEFAULT_DESCRIPTION =
  "충주 해피갤러리의 공예 원데이클래스, 정규 과정, 단체수업과 공방 작품을 만나보세요.";
// Styles are host-allowlisted; browsers hide link nonces and would report a hydration mismatch.
const LINK_NONCE_DISABLED = "";

export function meta() {
  return [
    { title: DEFAULT_TITLE },
    { name: "description", content: DEFAULT_DESCRIPTION },
    { property: "og:locale", content: "ko_KR" },
    { property: "og:site_name", content: "해피갤러리" },
    { name: "twitter:card", content: "summary_large_image" },
  ];
}

export function links() {
  return [
    { rel: "preconnect", href: "https://cdn.jsdelivr.net", crossOrigin: "anonymous" },
    { rel: "preconnect", href: "https://fonts.googleapis.com" },
    { rel: "preconnect", href: "https://fonts.gstatic.com", crossOrigin: "anonymous" },
    {
      rel: "stylesheet",
      href: "https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css",
    },
    {
      rel: "stylesheet",
      href: "https://fonts.googleapis.com/css2?family=Cormorant+Garamond:wght@500;600;700&family=Gowun+Batang:wght@400;700&display=swap",
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
        <meta name="theme-color" content="#56675d" />
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
  const notFound = isRouteErrorResponse(error) && error.status === 404;

  return (
    <Container className="page-container text-center py-5">
      <h1 className="h3 mb-3">
        {notFound ? "페이지를 찾을 수 없습니다" : "페이지를 불러오지 못했습니다"}
      </h1>
      <p className="text-muted-soft mb-0">
        {notFound
          ? "주소를 다시 확인하거나 홈에서 원하는 내용을 찾아보세요."
          : "잠시 후 다시 시도해 주세요."}
      </p>
    </Container>
  );
}
