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
import { Button } from "react-bootstrap";
import { CustomerAuthProvider } from "@/features/customer-auth/useCustomerAuth";
import { CartProvider } from "@/features/cart/CartProvider";
import { createQueryClient } from "@/shared/api";
import {
  ErrorBoundary as AppErrorBoundary,
  Layout as AppLayout,
  StatusPage,
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
      href: "https://fonts.googleapis.com/css2?family=Gowun+Dodum&display=swap",
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
        <meta name="theme-color" content="#2F4A3D" />
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

  // 루트 오류는 머리글 없이 그려지므로 공방 이름으로 홈 링크를 함께 보여 준다.
  return notFound ? (
    <StatusPage
      standalone
      kicker="404"
      title="페이지를 찾을 수 없습니다"
      description="주소가 바뀌었거나 없는 페이지입니다. 아래에서 원하는 화면을 찾아 주세요."
      actions={<a href="/" className="btn btn-primary">홈으로</a>}
    />
  ) : (
    <StatusPage
      standalone
      kicker="ERROR"
      title="페이지를 불러오지 못했습니다"
      description="잠시 후 다시 시도해 주세요. 계속 열리지 않으면 공방에 문의해 주세요."
      actions={<Button onClick={() => window.location.reload()}>다시 시도</Button>}
    />
  );
}
