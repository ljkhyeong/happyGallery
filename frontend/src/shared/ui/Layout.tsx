import { useState } from "react";
import { Outlet, Link, useLocation } from "react-router";
import { Container, Navbar, Nav } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { CartBadge } from "@/features/cart/CartBadge";
import { NotificationBell } from "@/features/notification/NotificationBell";
import { useToast } from "./ToastContainer";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import { CustomerSessionChangedError } from "@/shared/api";
import { ErrorAlert } from "./ErrorAlert";

const NAV_ITEMS = [
  { path: "/classes", label: "클래스" },
  { path: "/group-classes", label: "단체수업" },
  { path: "/products", label: "작품" },
  { path: "/passes/purchase", label: "8회권" },
] as const;

function isActive(pathname: string, itemPath: string): boolean {
  if (itemPath === "/") return pathname === "/";
  return pathname === itemPath || pathname.startsWith(itemPath + "/");
}

function isMainNavActive(pathname: string, itemPath: string): boolean {
  return isActive(pathname, itemPath)
    || (itemPath === "/classes" && isActive(pathname, "/bookings/new"));
}

export function Layout() {
  const { pathname } = useLocation();
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
    isFetching: workshopFetching,
    refetch: refetchWorkshop,
  } = useWorkshopProfile();

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
    } catch (error) {
      if (error instanceof CustomerSessionChangedError) return;
      toast.show(
        "로그아웃 완료를 확인하지 못해 현재 로그인 상태를 유지합니다. 잠시 후 다시 시도해 주세요.",
        "danger",
      );
    } finally {
      setLoggingOut(false);
    }
  };

  return (
    <div className="d-flex flex-column min-vh-100">
      <div className="app-utility-bar">
        <Container className="d-flex flex-wrap justify-content-between align-items-center gap-2 py-2" style={{ maxWidth: 1100 }}>
          <div className="app-utility-copy">충주 해피갤러리 · 공예 클래스와 핸드메이드 작품</div>
          <div className="d-flex flex-wrap align-items-center gap-3">
            {!isLoading && (
              isAuthenticated ? (
                <Link to="/my" className="app-utility-link">내 정보</Link>
              ) : (
                <>
                  <Link to="/login" className="app-utility-link">로그인</Link>
                  <Link to="/signup" className="app-utility-link">회원가입</Link>
                </>
              )
            )}
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
        <Container className="pt-3" style={{ maxWidth: 1100 }}>
          <ErrorAlert
            error={authError}
            onRetry={() => { void refreshAuth().catch(() => undefined); }}
            retrying={authRefreshing}
          />
        </Container>
      )}

      <Navbar expand="md" className="app-navbar" data-bs-theme="light">
        <Container style={{ maxWidth: 1100 }}>
          <Navbar.Brand as={Link} to="/" className="app-brand d-flex flex-column">
            <span className="app-brand-mark">해피갤러리</span>
            <span className="app-brand-subtitle">CHUNGJU CRAFT ATELIER</span>
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="main-nav" />
          <Navbar.Collapse id="main-nav">
            <Nav className="ms-auto align-items-md-center gap-md-1">
              {NAV_ITEMS.map(({ path, label }) => (
                <Nav.Link
                  key={path}
                  as={Link}
                  to={path}
                  active={isMainNavActive(pathname, path)}
                  className="app-nav-link"
                >
                  {label}
                </Nav.Link>
              ))}
            </Nav>
            <Nav className="ms-md-4 border-md-start ps-md-4 align-items-md-center gap-md-2">
              <CartBadge />
              <NotificationBell />
              {!isLoading && (
                isAuthenticated ? (
                  <>
                    <Nav.Link
                      as={Link}
                      to="/my"
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
                      active={isActive(pathname, "/login")}
                      className="app-nav-link"
                    >
                      로그인
                    </Nav.Link>
                    <Nav.Link
                      as={Link}
                      to="/signup"
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

      <main className="flex-grow-1">
        <Outlet />
      </main>

      <footer className="app-footer py-4 small">
        <Container style={{ maxWidth: 1100 }}>
          {workshopError && !workshop && (
            <ErrorAlert
              error={workshopError}
              onRetry={() => { void refetchWorkshop(); }}
              retrying={workshopFetching}
            />
          )}
          <div className="app-footer-grid">
            <div>
              <div className="app-footer-brand">happyGallery</div>
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
    </div>
  );
}
