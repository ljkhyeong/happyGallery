import { Outlet, Link, useLocation } from "react-router-dom";
import { Container, Navbar, Nav } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";

const NAV_ITEMS = [
  { path: "/products", label: "상품" },
  { path: "/bookings/new", label: "예약하기" },
  { path: "/bookings/manage", label: "예약 조회" },
  { path: "/passes/purchase", label: "8회권" },
  { path: "/orders/new", label: "주문" },
  { path: "/orders/detail", label: "주문 조회" },
] as const;

function isActive(pathname: string, itemPath: string): boolean {
  if (itemPath === "/") return pathname === "/";
  return pathname === itemPath || pathname.startsWith(itemPath + "/");
}

export function Layout() {
  const { pathname } = useLocation();
  const { user, isAuthenticated, isLoading, logout } = useCustomerAuth();

  return (
    <div className="d-flex flex-column min-vh-100">
      <Navbar expand="md" className="app-navbar border-bottom" data-bs-theme="light">
        <Container>
          <Navbar.Brand as={Link} to="/" className="app-brand">
            HappyGallery
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="main-nav" />
          <Navbar.Collapse id="main-nav">
            <Nav className="ms-auto gap-md-1">
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
            <Nav className="ms-md-3 border-md-start ps-md-3">
              {!isLoading && (
                isAuthenticated ? (
                  <>
                    <Nav.Link as="span" className="app-nav-link text-muted-soft" style={{ cursor: "default" }}>
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
                  <Nav.Link
                    as={Link}
                    to="/login"
                    active={isActive(pathname, "/login")}
                    className="app-nav-link"
                  >
                    로그인
                  </Nav.Link>
                )
              )}
              <Nav.Link
                as={Link}
                to="/admin"
                active={isActive(pathname, "/admin")}
                className="app-nav-link text-muted-soft"
              >
                관리자
              </Nav.Link>
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
