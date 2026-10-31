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
      // 링크 이름이 숫자 배지를 덮으므로 담은 수량을 이름에 함께 넣는다.
      aria-label={itemCount > 0 ? `장바구니 ${itemCount}개` : "장바구니"}
      className="app-nav-link position-relative"
    >
      <ShoppingBag size={21} strokeWidth={1.8} aria-hidden="true" />
      <span>장바구니</span>
      {itemCount > 0 && (
        <Badge
          bg="dark"
          className="position-absolute top-0 start-50 ms-2 translate-middle-y"
          style={{ fontSize: "0.65rem" }}
        >
          {itemCount > 99 ? "99+" : itemCount}
        </Badge>
      )}
    </Nav.Link>
  );
}
