import { useState } from "react";
import { Outlet, Link, useLocation, useMatches } from "react-router";
import { Container, Nav } from "react-bootstrap";
import { CalendarDays, ClipboardList, Heart, House, Palette, UserRound } from "lucide-react";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { CartBadge } from "@/features/cart/CartBadge";
import { NotificationBell } from "@/features/notification/NotificationBell";
import { ProductSearchForm } from "@/features/product/ProductSearchForm";
import { SHOP_CATEGORIES } from "@/features/product/shopCategories";
import { useToast } from "./ToastContainer";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import { CustomerSessionChangedError } from "@/shared/api";
import { ErrorAlert } from "./ErrorAlert";
import type { WorkshopProfile } from "@/shared/types";

const CLASS_NAV_ITEMS = [
  { path: "/classes", label: "원데이 클래스" },
  { path: "/passes/purchase", label: "정규반·4회권" },
  { path: "/group-classes", label: "단체·출강" },
  { path: "/events", label: "이벤트" },
] as const;

/** 둘러보기 화면에서만 모바일 하단 탭바를 보여 주고, 상세·결제 화면의 하단 버튼과 겹치지 않게 한다. */
const TAB_BAR_PATHS = new Set(["/", "/classes", "/products", "/events", "/group-classes", "/guest", "/my"]);

function isActive(pathname: string, itemPath: string): boolean {
  if (itemPath === "/") return pathname === "/";
  return pathname === itemPath || pathname.startsWith(itemPath + "/");
}

function isMainNavActive(pathname: string, itemPath: string): boolean {
  return isActive(pathname, itemPath)
    || (itemPath === "/classes" && isActive(pathname, "/bookings/new"));
}

function initialWorkshopFromMatches(
  matches: ReturnType<typeof useMatches>,
): WorkshopProfile | undefined {
  const match = matches.find(({ loaderData }) =>
    typeof loaderData === "object"
    && loaderData !== null
    && "workshop" in loaderData);
  return (match?.loaderData as { workshop?: WorkshopProfile } | undefined)?.workshop;
}

export function Layout() {
  const { pathname, search } = useLocation();
  const searchParams = new URLSearchParams(search);
  const onProducts = pathname === "/products";
  const activeCategory = onProducts ? searchParams.get("category")?.toUpperCase() : undefined;
  const madeToOrderOnly = onProducts && searchParams.get("type") === "MADE_TO_ORDER";
  const matches = useMatches();
  const {
    user,
    status: authStatus,
    error: authError,
    isAuthenticated,
    isLoading,
    isRefreshing: authRefreshing,
    refresh: refreshAuth,
    logout,
  } = useCustomerAuth();
  const [loggingOut, setLoggingOut] = useState(false);
  const toast = useToast();
  const {
    data: workshop,
    error: workshopError,
    query: {
      isFetching: workshopFetching,
      refetch: refetchWorkshop,
    },
  } = useWorkshopProfile(initialWorkshopFromMatches(matches));

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
    } catch (error) {
      if (error instanceof CustomerSessionChangedError) return;
      toast.show(
        "로그아웃 여부를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        "danger",
      );
    } finally {
      setLoggingOut(false);
    }
  };

  const showTabBar = TAB_BAR_PATHS.has(pathname);
  const tabItems = [
    { to: "/", label: "홈", icon: House, active: pathname === "/" },
    { to: "/classes", label: "클래스", icon: CalendarDays, active: isMainNavActive(pathname, "/classes") },
    { to: "/products", label: "작품", icon: Palette, active: isActive(pathname, "/products") },
    {
      to: isAuthenticated ? "/my/bookings" : "/guest",
      label: "예약 조회",
      icon: ClipboardList,
      active: pathname === "/guest" || isActive(pathname, "/my/bookings"),
    },
    { to: "/my", label: "내 정보", icon: UserRound, active: pathname === "/my" },
  ];

  return (
    <div className={`d-flex flex-column min-vh-100${showTabBar ? " has-tab-bar" : ""}`}>
      {/* 키보드 사용자가 머리글 링크를 건너뛰고 본문으로 바로 가게 한다. */}
      <a
        href="#main-content"
        className="skip-link visually-hidden-focusable"
        onClick={(event) => {
          // 주소에 해시를 남기지 않고 본문으로 포커스만 옮긴다(내 정보 화면은 해시로 화면을 바꾼다).
          event.preventDefault();
          document.getElementById("main-content")?.focus();
        }}
      >
        본문 바로가기
      </a>
      <div className="app-utility-bar">
        <Container className="app-utility-inner">
          <div className="app-utility-copy">충주 계명대로 공예 공방 · 작품 판매와 공예 클래스</div>
          <div className="app-utility-links">
            {!isLoading && isAuthenticated && (
              <Link to="/my/benefits" className="app-utility-link">쿠폰·적립금</Link>
            )}
            <Link to="/my/inquiries" className="app-utility-link">1:1 문의</Link>
            <Link
              to="/guest"
              state={{ monitoringSource: "layout_utility" }}
              className="app-utility-link"
            >
              비회원 조회
            </Link>
            {workshop?.phone && (
              <a href={`tel:${workshop.phone.replace(/\D/g, "")}`} className="app-utility-link">
                고객센터 {workshop.phone}
              </a>
            )}
          </div>
        </Container>
      </div>

      {authStatus === "error" && (
        <Container className="pt-3">
          <ErrorAlert
            error={authError}
            onRetry={() => { void refreshAuth().catch(() => undefined); }}
            retrying={authRefreshing}
          />
        </Container>
      )}

      <header className="app-navbar">
        <Container className="app-header-inner">
          <Link to="/" className="app-brand">해피갤러리</Link>
          <ProductSearchForm />
          <Nav as="nav" className="app-account" aria-label="내 메뉴">
            <Link to="/my/favorites" className="app-icon-link">
              <Heart size={21} strokeWidth={1.8} aria-hidden="true" />
              <span>찜</span>
            </Link>
            <CartBadge />
            <NotificationBell />
            {!isLoading && (
              isAuthenticated ? (
                <>
                  <Link
                    to="/my"
                    className={`app-icon-link${isActive(pathname, "/my") ? " active" : ""}`}
                  >
                    <UserRound size={21} strokeWidth={1.8} aria-hidden="true" />
                    <span>{user!.name}</span>
                  </Link>
                  <button
                    type="button"
                    className="app-logout-button"
                    onClick={handleLogout}
                    disabled={loggingOut}
                  >
                    {loggingOut ? "로그아웃 중..." : "로그아웃"}
                  </button>
                </>
              ) : (
                <>
                  <Link
                    to="/login"
                    className={`app-icon-link${isActive(pathname, "/login") ? " active" : ""}`}
                  >
                    <UserRound size={21} strokeWidth={1.8} aria-hidden="true" />
                    <span>로그인</span>
                  </Link>
                  <Link to="/signup" className="app-signup-link">회원가입</Link>
                </>
              )
            )}
          </Nav>
        </Container>
        <nav className="app-category-nav" aria-label="카테고리 메뉴">
          <Container className="app-category-inner">
            <Link
              to="/products"
              className={onProducts && !activeCategory && !madeToOrderOnly ? "active" : undefined}
              aria-current={onProducts && !activeCategory && !madeToOrderOnly ? "page" : undefined}
            >
              전체 작품
            </Link>
            {SHOP_CATEGORIES.map((category) => (
              <Link
                key={category}
                to={`/products?${new URLSearchParams({ category })}`}
                className={activeCategory === category ? "active" : undefined}
                aria-current={activeCategory === category ? "page" : undefined}
              >
                {category}
              </Link>
            ))}
            <Link
              to="/products?type=MADE_TO_ORDER"
              className={madeToOrderOnly ? "active" : undefined}
              aria-current={madeToOrderOnly ? "page" : undefined}
            >
              주문 제작
            </Link>
            <span className="app-category-divider" aria-hidden="true" />
            {CLASS_NAV_ITEMS.map(({ path, label }) => (
              <Link
                key={path}
                to={path}
                className={`is-class${isMainNavActive(pathname, path) ? " active" : ""}`}
                aria-current={isMainNavActive(pathname, path) ? "page" : undefined}
              >
                {label}
              </Link>
            ))}
          </Container>
        </nav>
      </header>

      <main id="main-content" tabIndex={-1} className="flex-grow-1">
        <Outlet />
      </main>

      <footer className="app-footer">
        <Container>
          {workshopError && !workshop && (
            <ErrorAlert
              error={workshopError}
              onRetry={() => { void refetchWorkshop(); }}
              retrying={workshopFetching}
            />
          )}
          <div className="app-footer-grid">
            <div className="app-footer-cs">
              <strong>고객센터</strong>
              {workshop?.phone && (
                <a href={`tel:${workshop.phone.replace(/\D/g, "")}`} className="app-footer-phone">{workshop.phone}</a>
              )}
              <div className="app-footer-contact">
                {workshop?.kakaoTalkId && <span>카카오톡 {workshop.kakaoTalkId}</span>}
                {workshop?.email && <a href={`mailto:${workshop.email}`}>{workshop.email}</a>}
                {workshop?.naverTalkUrl ? (
                  <a href={workshop.naverTalkUrl} target="_blank" rel="noreferrer">네이버톡톡 문의</a>
                ) : null}
              </div>
            </div>
            <div className="app-footer-business">
              <strong>{workshop?.name ?? "해피갤러리"}</strong>
              <span>
                {[
                  workshop?.representativeName && `대표자 ${workshop.representativeName}`,
                  workshop?.addressLine1 && [workshop.addressLine1, workshop.addressLine2].filter(Boolean).join(" "),
                ].filter(Boolean).join(" · ")}
              </span>
              <span>
                {[
                  workshop?.businessRegistrationNumber && `사업자등록번호 ${workshop.businessRegistrationNumber}`,
                  workshop?.mailOrderRegistrationNumber && `통신판매업 신고번호 ${workshop.mailOrderRegistrationNumber}`,
                ].filter(Boolean).join(" · ")}
              </span>
              <nav className="app-footer-links" aria-label="정책 및 사업자 정보">
                <Link to="/terms">이용약관</Link>
                <Link to="/privacy" className="fw-bold">개인정보처리방침</Link>
                <Link to="/business-info">사업자 정보</Link>
                {workshop?.naverBlogUrl && (
                  <a href={workshop.naverBlogUrl} target="_blank" rel="noreferrer">네이버 블로그</a>
                )}
                {workshop?.instagramUrl && (
                  <a href={workshop.instagramUrl} target="_blank" rel="noreferrer">인스타그램</a>
                )}
                {workshop?.smartStoreUrl && (
                  <a href={workshop.smartStoreUrl} target="_blank" rel="noreferrer">스마트스토어</a>
                )}
              </nav>
            </div>
          </div>
          <div className="app-footer-copyright">
            &copy; {new Date().getFullYear()} {workshop?.name ?? "해피갤러리"}
          </div>
        </Container>
      </footer>

      {showTabBar && (
        <nav className="app-tab-bar" aria-label="주요 메뉴">
          {tabItems.map(({ to, label, icon: Icon, active }) => (
            <Link key={label} to={to} className={active ? "is-active" : undefined} aria-current={active ? "page" : undefined}>
              <Icon size={21} strokeWidth={1.8} aria-hidden="true" />
              <span>{label}</span>
            </Link>
          ))}
        </nav>
      )}
    </div>
  );
}
