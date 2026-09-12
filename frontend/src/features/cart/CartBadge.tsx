import { Link } from "react-router";
import { Nav, Badge } from "react-bootstrap";
import { ShoppingBag } from "lucide-react";
import { useCart } from "./useCart";

export function CartBadge() {
  const { itemCount } = useCart();

  return (
    <Nav.Link
      as={Link}
      to="/cart"
      eventKey="/cart"
      aria-label="장바구니"
      className="app-nav-link position-relative align-self-start d-inline-flex align-items-center gap-2"
    >
      <ShoppingBag size={20} aria-hidden="true" />
      <span className="d-lg-none">장바구니</span>
      {itemCount > 0 && (
        <Badge
          bg="danger"
          pill
          className="position-absolute top-0 start-100 translate-middle"
          style={{ fontSize: "0.65rem" }}
        >
          {itemCount > 99 ? "99+" : itemCount}
        </Badge>
      )}
    </Nav.Link>
  );
}
