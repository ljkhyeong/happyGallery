import { Outlet, Link, useLocation } from "react-router-dom";
import { Container, Navbar, Nav } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";

const NAV_ITEMS = [
  { path: "/products", label: "상품" },
  { path: "/bookings/new", label: "체험 예약" },
  { path: "/passes/purchase", label: "8회권" },
] as const;

function isActive(pathname: string, itemPath: string): boolean {
  if (itemPath === "/") return pathname === "/";
  return pathname === itemPath || pathname.startsWith(itemPath + "/");
}

export function Layout() {
  const { pathname } = useLocation();
  const { user, isAuthenticated, isLoading, logout } = useCustomerAuth();
  const utilityCopy = isAuthenticated
    ? "회원은 내 정보에서 주문·예약·8회권을 바로 확인하고, 비회원 이력도 가져올 수 있습니다."
    : "회원은 내 정보에서, 비회원은 조회 경로에서 주문과 예약을 확인합니다.";

  return (
    <div className="d-flex flex-column min-vh-100">
      <div className="app-utility-bar border-bottom">
        <Container className="d-flex flex-wrap justify-content-between align-items-center gap-2 py-2">
          <div className="app-utility-copy">{utilityCopy}</div>
          <div className="d-flex flex-wrap align-items-center gap-3">
            {!isLoading && (
              isAuthenticated ? (
                <Link to="/my" className="app-utility-link">회원 내 정보</Link>
              ) : (
                <>
                  <Link to="/login" className="app-utility-link">회원 로그인</Link>
                  <Link to="/signup" className="app-utility-link">회원가입</Link>
                </>
              )
            )}
            <Link to="/guest/orders" className="app-utility-link">비회원 주문 조회</Link>
            <Link to="/guest/bookings" className="app-utility-link">비회원 예약 조회</Link>
            <Link to="/admin" className="app-utility-link">관리자</Link>
          </div>
        </Container>
      </div>

      <Navbar expand="md" className="app-navbar border-bottom" data-bs-theme="light">
        <Container>
          <Navbar.Brand as={Link} to="/" className="app-brand d-flex flex-column">
            <span className="app-brand-mark">HappyGallery</span>
            <span className="app-brand-subtitle">Handmade store & reservation studio</span>
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="main-nav" />
          <Navbar.Collapse id="main-nav">
            <Nav className="ms-auto align-items-md-center gap-md-2">
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
                      onClick={() => logout()}
                    >
                      로그아웃
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

      <main className="flex-grow-1 py-4">
        <Outlet />
      </main>

      <footer className="app-footer text-center py-3 small border-top">
        <Container>
          <span className="text-muted-soft">
            &copy; {new Date().getFullYear()} HappyGallery &middot; 핸드메이드 공방
          </span>
        </Container>
      </footer>
    </div>
  );
}
