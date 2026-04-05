import { Outlet, Link, useLocation } from "react-router-dom";
import { Container, Navbar, Nav } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { CartBadge } from "@/features/cart/CartBadge";
import { NotificationBell } from "@/features/notification/NotificationBell";

const NAV_ITEMS = [
  { path: "/products", label: "STORE" },
  { path: "/bookings/new", label: "WORKSHOP" },
  { path: "/passes/purchase", label: "PASS" },
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
      <div className="app-utility-bar">
        <Container className="d-flex flex-wrap justify-content-between align-items-center gap-2 py-2" style={{ maxWidth: 1100 }}>
          <div className="app-utility-copy">FREE SHIPPING ON ORDERS OVER ₩50,000</div>
          <div className="d-flex flex-wrap align-items-center gap-3">
            {!isLoading && (
              isAuthenticated ? (
                <Link to="/my" className="app-utility-link">MY PAGE</Link>
              ) : (
                <>
                  <Link to="/login" className="app-utility-link">LOGIN</Link>
                  <Link to="/signup" className="app-utility-link">JOIN</Link>
                </>
              )
            )}
            <Link
              to="/guest"
              state={{ monitoringSource: "layout_utility" }}
              className="app-utility-link"
            >
              ORDER LOOKUP
            </Link>
            <Link to="/admin" className="app-utility-link">ADMIN</Link>
          </div>
        </Container>
      </div>

      <Navbar expand="md" className="app-navbar" data-bs-theme="light">
        <Container style={{ maxWidth: 1100 }}>
          <Navbar.Brand as={Link} to="/" className="app-brand d-flex flex-column">
            <span className="app-brand-mark">HAPPYGALLERY</span>
            <span className="app-brand-subtitle">Handmade Store &amp; Workshop</span>
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
                      onClick={() => logout()}
                    >
                      LOGOUT
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
                      LOGIN
                    </Nav.Link>
                    <Nav.Link
                      as={Link}
                      to="/signup"
                      active={isActive(pathname, "/signup")}
                      className="app-signup-link"
                    >
                      JOIN US
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
          <span>
            &copy; {new Date().getFullYear()} HAPPYGALLERY &middot; Handmade with care
          </span>
        </Container>
      </footer>
    </div>
  );
}
