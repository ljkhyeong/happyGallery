import { Link } from "react-router-dom";
import { Nav, Badge } from "react-bootstrap";
import { useCart } from "./useCart";

export function CartBadge() {
  const { itemCount } = useCart();

  return (
    <Nav.Link as={Link} to="/cart" className="app-nav-link position-relative">
      <span aria-label="장바구니">&#128722;</span>
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
