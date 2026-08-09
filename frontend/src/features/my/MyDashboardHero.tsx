import { LinkButton } from "@/shared/ui/LinkButton";
import { Badge, Button, Card } from "react-bootstrap";
import type { CustomerUser } from "@/features/customer-auth/useCustomerAuth";
import type { MyBookingSummary } from "./api";
import { formatDateTime } from "@/shared/lib";

interface Props {
  user: CustomerUser;
  nextBooking: MyBookingSummary | undefined;
  onLogout: () => void;
  loggingOut: boolean;
}

export function MyDashboardHero({ user, nextBooking, onLogout, loggingOut }: Props) {
  return (
    <Card className="my-dashboard-hero mb-4 border-0">
      <Card.Body>
        <div className="d-flex flex-column flex-lg-row justify-content-between gap-4">
          <div className="flex-grow-1">
            <div className="my-section-kicker mb-2">내 정보</div>
            <h3 className="mb-2">{user.name}님, 다시 오셨네요</h3>
            <p className="text-muted-soft mb-3">
              최근 주문, 예약, 8회권과 쿠폰·적립금 현황을 내 정보에서 관리할 수 있습니다.
            </p>
            <div className="d-flex flex-wrap gap-2 align-items-center mb-3">
              <Badge bg={user.phoneVerified ? "success" : "secondary"}>
                {user.phoneVerified
                  ? "휴대폰 인증 완료"
                  : user.phone
                    ? "휴대폰 재확인 필요"
                    : "휴대폰 등록 필요"}
              </Badge>
              <span className="text-muted-soft small">{user.email}</span>
              {user.phone && <span className="text-muted-soft small">{user.phone}</span>}
            </div>
            {nextBooking && (
              <div className="my-dashboard-note">
                최근 조회에서 확인한 다음 예약: <strong>{nextBooking.className}</strong> · {formatDateTime(nextBooking.startAt)}
              </div>
            )}
          </div>
          <div className="d-flex flex-wrap align-content-start gap-2">
            <LinkButton to="/products" variant="dark" size="sm">
              상품 보러가기
            </LinkButton>
            <LinkButton to="/bookings/new" variant="outline-primary" size="sm">
              체험 예약
            </LinkButton>
            <LinkButton to="/passes/purchase" variant="outline-primary" size="sm">
              8회권 구매
            </LinkButton>
            <LinkButton to="/my/benefits" variant="outline-primary" size="sm">
              쿠폰·적립금
            </LinkButton>
            <LinkButton to="/my/reviews" variant="outline-primary" size="sm">
              내 후기
            </LinkButton>
            <Button variant="outline-secondary" size="sm" onClick={onLogout} disabled={loggingOut}>
              {loggingOut ? "로그아웃 중..." : "로그아웃"}
            </Button>
          </div>
        </div>
      </Card.Body>
    </Card>
  );
}
