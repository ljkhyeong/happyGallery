import { useState } from "react";
import { Outlet, Link, useLocation, useMatches } from "react-router";
import { Container, Navbar, Nav } from "react-bootstrap";
import { CalendarDays, ClipboardList, House, Palette, UserRound } from "lucide-react";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { CartBadge } from "@/features/cart/CartBadge";
import { NotificationBell } from "@/features/notification/NotificationBell";
import { useToast } from "./ToastContainer";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import { CustomerSessionChangedError } from "@/shared/api";
import { ErrorAlert } from "./ErrorAlert";
import type { WorkshopProfile } from "@/shared/types";

const NAV_ITEMS = [
  { path: "/classes", label: "클래스" },
  { path: "/group-classes", label: "단체수업" },
  { path: "/products", label: "작품" },
  { path: "/events", label: "이벤트" },
  { path: "/passes/purchase", label: "4회권" },
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
  const { pathname } = useLocation();
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
        <Container className="d-flex flex-wrap justify-content-between align-items-center gap-2 py-2">
          <div className="app-utility-copy">충주 해피갤러리 · 공예 클래스와 핸드메이드 작품</div>
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

      <Navbar expand="lg" collapseOnSelect className="app-navbar" data-bs-theme="light">
        <Container>
          <Navbar.Brand as={Link} to="/" className="app-brand d-flex">
            <span className="app-brand-dots" aria-hidden="true" />
            <span className="app-brand-text">
              <span className="app-brand-mark">해피갤러리</span>
              <span className="app-brand-subtitle">CHUNGJU CRAFT ATELIER</span>
            </span>
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="main-nav" label="메뉴 열기/닫기" />
          <Navbar.Collapse id="main-nav">
            <Nav className="ms-auto align-items-lg-center gap-lg-1">
              {NAV_ITEMS.map(({ path, label }) => (
                <Nav.Link
                  key={path}
                  eventKey={path}
                  as={Link}
                  to={path}
                  active={isMainNavActive(pathname, path)}
                  className="app-nav-link"
                >
                  {label}
                </Nav.Link>
              ))}
            </Nav>
            <Nav className="ms-lg-4 border-lg-start ps-lg-4 align-items-lg-center gap-lg-2">
              <CartBadge />
              <NotificationBell />
              {!isLoading && (
                isAuthenticated ? (
                  <>
                    <Nav.Link
                      as={Link}
                      to="/my"
                      eventKey="/my"
                      active={isActive(pathname, "/my")}
                      className="app-nav-link app-member-link"
                    >
                      {user!.name}
                    </Nav.Link>
                    <Nav.Link
                      as="button"
                      className="app-nav-link text-muted-soft btn btn-link p-0 border-0"
                      onClick={handleLogout}
                      disabled={loggingOut}
                    >
                      {loggingOut ? "로그아웃 중..." : "로그아웃"}
                    </Nav.Link>
                  </>
                ) : (
                  <>
                    <Nav.Link
                      as={Link}
                      to="/login"
                      eventKey="/login"
                      active={isActive(pathname, "/login")}
                      className="app-nav-link"
                    >
                      로그인
                    </Nav.Link>
                    <Nav.Link
                      as={Link}
                      to="/signup"
                      eventKey="/signup"
                      active={isActive(pathname, "/signup")}
                      className="app-signup-link"
                    >
                      회원가입
                    </Nav.Link>
                  </>
                )
              )}
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>

      <main id="main-content" tabIndex={-1} className="flex-grow-1">
        <Outlet />
      </main>

      <footer className="app-footer py-4 small">
        <Container>
          {workshopError && !workshop && (
            <ErrorAlert
              error={workshopError}
              onRetry={() => { void refetchWorkshop(); }}
              retrying={workshopFetching}
            />
          )}
          <div className="app-footer-grid">
            <div>
              <div className="app-footer-brand">{workshop?.name ?? "해피갤러리"}</div>
              {workshop?.introduction && (
                <p className="app-footer-introduction">{workshop.introduction}</p>
              )}
            </div>
            <div className="app-footer-business">
              <strong>{workshop?.name ?? "해피갤러리"}</strong>
              {workshop?.businessRegistrationNumber && (
                <span>사업자등록번호 {workshop.businessRegistrationNumber}</span>
              )}
              {workshop?.representativeName && <span>대표자 {workshop.representativeName}</span>}
              {workshop?.mailOrderRegistrationNumber && (
                <span>통신판매업 신고번호 {workshop.mailOrderRegistrationNumber}</span>
              )}
              {workshop?.addressLine1 && (
                <span>{[workshop.addressLine1, workshop.addressLine2].filter(Boolean).join(" ")}</span>
              )}
              <div className="app-footer-contact">
                {workshop?.phone && (
                  <a href={`tel:${workshop.phone.replace(/\D/g, "")}`}>{workshop.phone}</a>
                )}
                {workshop?.email && <a href={`mailto:${workshop.email}`}>{workshop.email}</a>}
                {workshop?.kakaoTalkId && <span>카카오톡 {workshop.kakaoTalkId}</span>}
                {workshop?.naverTalkUrl ? (
                  <a href={workshop.naverTalkUrl} target="_blank" rel="noreferrer">네이버톡톡 문의</a>
                ) : null}
              </div>
            </div>
            <nav className="app-footer-links" aria-label="정책 및 사업자 정보">
              {workshop?.naverBlogUrl && (
                <a href={workshop.naverBlogUrl} target="_blank" rel="noreferrer">네이버 블로그</a>
              )}
              {workshop?.instagramUrl && (
                <a href={workshop.instagramUrl} target="_blank" rel="noreferrer">인스타그램</a>
              )}
              {workshop?.smartStoreUrl && (
                <a href={workshop.smartStoreUrl} target="_blank" rel="noreferrer">스마트스토어</a>
              )}
              <Link to="/terms">이용약관</Link>
              <Link to="/privacy">개인정보처리방침</Link>
              <Link to="/business-info">사업자 정보</Link>
            </nav>
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
