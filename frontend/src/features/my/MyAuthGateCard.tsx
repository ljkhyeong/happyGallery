import { LinkButton } from "@/shared/ui/LinkButton";
import { Card } from "react-bootstrap";
import { Link, useLocation } from "react-router";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";

interface Props {
  title: string;
  description: string;
  showGuestLinks?: boolean;
}

export function MyAuthGateCard({ title, description, showGuestLinks = false }: Props) {
  const location = useLocation();
  const redirectTo = `${location.pathname}${location.search}`;
  const loginHref = buildAuthPageHref("/login", { redirectTo });
  const signupHref = buildAuthPageHref("/signup", { redirectTo });

  return (
    <Card className="my-gate-card border-0">
      <Card.Body className="p-4">
        <h5 className="mb-2">{title}</h5>
        <p className="text-muted-soft small mb-3">{description}</p>
        <div className="d-flex flex-wrap gap-2">
          <LinkButton to={loginHref} variant="primary">
            로그인
          </LinkButton>
          <LinkButton to={signupHref} variant="outline-dark">
            회원가입
          </LinkButton>
        </div>
        {showGuestLinks && (
          <div className="d-flex flex-wrap gap-3 mt-4 small">
            <Link to="/guest/orders" className="my-inline-link">비회원 주문 조회</Link>
            <Link to="/guest/bookings" className="my-inline-link">비회원 예약 조회</Link>
          </div>
        )}
      </Card.Body>
    </Card>
  );
}
