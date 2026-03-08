import { Outlet, Link, useLocation } from "react-router-dom";
import { Container, Navbar, Nav } from "react-bootstrap";

const NAV_ITEMS = [
  { path: "/", label: "홈" },
  { path: "/products", label: "상품" },
  { path: "/bookings/new", label: "예약하기" },
  { path: "/bookings/manage", label: "예약 조회" },
  { path: "/passes/purchase", label: "8회권" },
  { path: "/admin", label: "관리자" },
] as const;

export function Layout() {
  const { pathname } = useLocation();

  return (
    <div className="d-flex flex-column min-vh-100">
      <Navbar bg="white" expand="md" className="border-bottom">
        <Container>
          <Navbar.Brand as={Link} to="/" className="fw-bold">
            HappyGallery
          </Navbar.Brand>
          <Navbar.Toggle aria-controls="main-nav" />
          <Navbar.Collapse id="main-nav">
            <Nav className="ms-auto">
              {NAV_ITEMS.map(({ path, label }) => (
                <Nav.Link
                  key={path}
                  as={Link}
                  to={path}
                  active={pathname === path}
                >
                  {label}
                </Nav.Link>
              ))}
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>

      <main className="flex-grow-1">
        <Outlet />
      </main>

      <footer className="text-center py-3 text-muted-soft small border-top">
        HappyGallery
      </footer>
    </div>
  );
}
