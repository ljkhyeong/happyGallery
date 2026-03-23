import { Badge, Button, Card } from "react-bootstrap";
import { Link } from "react-router-dom";
import type { CustomerUser } from "@/features/customer-auth/useCustomerAuth";
import type { MyBookingSummary } from "./api";
import { formatDateTime } from "@/shared/lib";

interface Props {
  user: CustomerUser;
  nextBooking: MyBookingSummary | undefined;
  onLogout: () => void;
}

export function MyDashboardHero({ user, nextBooking, onLogout }: Props) {
  return (
    <Card className="my-dashboard-hero mb-4 border-0">
      <Card.Body>
        <div className="d-flex flex-column flex-lg-row justify-content-between gap-4">
          <div className="flex-grow-1">
            <div className="my-section-kicker mb-2">Member Self Service</div>
            <h3 className="mb-2">{user.name}님, 다시 오셨네요</h3>
            <p className="text-muted-soft mb-3">
              최근 주문, 예약, 8회권 현황을 이 페이지에서 바로 관리할 수 있습니다.
            </p>
            <div className="d-flex flex-wrap gap-2 align-items-center mb-3">
              <Badge bg={user.phoneVerified ? "success" : "secondary"}>
                {user.phoneVerified ? "휴대폰 인증 완료" : "휴대폰 재확인 필요"}
              </Badge>
              <span className="text-muted-soft small">{user.email}</span>
              <span className="text-muted-soft small">{user.phone}</span>
            </div>
            {nextBooking && (
              <div className="my-dashboard-note">
                다음 예약: <strong>{nextBooking.className}</strong> · {formatDateTime(nextBooking.startAt)}
              </div>
            )}
          </div>
          <div className="d-flex flex-wrap align-content-start gap-2">
            <Button as={Link as any} to="/products" variant="dark" size="sm">
              상품 보러가기
            </Button>
            <Button as={Link as any} to="/bookings/new" variant="outline-primary" size="sm">
              체험 예약
            </Button>
            <Button as={Link as any} to="/passes/purchase" variant="outline-primary" size="sm">
              8회권 구매
            </Button>
            <Button variant="outline-secondary" size="sm" onClick={onLogout}>
              로그아웃
            </Button>
          </div>
        </div>
      </Card.Body>
    </Card>
  );
}
