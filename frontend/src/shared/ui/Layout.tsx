import { useState } from "react";
import { Outlet, Link, useLocation } from "react-router-dom";
import { Container, Navbar, Nav } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { CartBadge } from "@/features/cart/CartBadge";
import { NotificationBell } from "@/features/notification/NotificationBell";
import { useToast } from "./ToastContainer";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";

const NAV_ITEMS = [
  { path: "/products", label: "작품" },
  { path: "/bookings/new", label: "클래스" },
  { path: "/passes/purchase", label: "8회권" },
] as const;

function isActive(pathname: string, itemPath: string): boolean {
  if (itemPath === "/") return pathname === "/";
  return pathname === itemPath || pathname.startsWith(itemPath + "/");
}

export function Layout() {
  const { pathname } = useLocation();
  const { user, isAuthenticated, isLoading, logout } = useCustomerAuth();
  const [loggingOut, setLoggingOut] = useState(false);
  const toast = useToast();
  const { data: workshop } = useWorkshopProfile();

  const handleLogout = async () => {
    setLoggingOut(true);
    try {
      await logout();
    } catch {
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
          <div className="app-utility-copy">작품 주문 · 클래스 예약 · 8회권을 한곳에서</div>
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
            <Link to="/admin" className="app-utility-link">ADMIN</Link>
          </div>
        </Container>
      </div>

      <Navbar expand="md" className="app-navbar" data-bs-theme="light">
        <Container style={{ maxWidth: 1100 }}>
          <Navbar.Brand as={Link} to="/" className="app-brand d-flex flex-column">
            <span className="app-brand-mark">happyGallery</span>
            <span className="app-brand-subtitle">작품과 시간이 머무는 공방</span>
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="main-nav" />
          <Navbar.Collapse id="main-nav">
            <Nav className="ms-auto align-items-md-center gap-md-1">
              {NAV_ITEMS.map(({ path, label }) => (
                <Nav.Link
                  key={path}
                  as={Link}
                  to={path}
                  active={isActive(pathname, path)}
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

      <footer className="app-footer text-center py-4 small">
        <Container style={{ maxWidth: 1100 }}>
          <div className="app-footer-brand">happyGallery</div>
          {workshop?.addressLine1 && (
            <div className="app-footer-contact">
              {[workshop.addressLine1, workshop.addressLine2].filter(Boolean).join(" ")}
              {workshop.phone && ` · ${workshop.phone}`}
            </div>
          )}
          <span>&copy; {new Date().getFullYear()} 손으로 만든 작품과 공방의 시간을 전합니다.</span>
        </Container>
      </footer>
    </div>
  );
}
