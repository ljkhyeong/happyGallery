import { Card, Col, Row } from "react-bootstrap";
import type { MyBookingSummary, MyOrderSummary } from "./api";
import { formatDateTime } from "@/shared/lib";

interface Props {
  orderCount: number;
  bookingCount: number;
  remainingCredits: number;
  passCount: number;
  latestOrder: MyOrderSummary | undefined;
  nextBooking: MyBookingSummary | undefined;
  activePass: boolean;
}

export function MyStatsRow({
  orderCount,
  bookingCount,
  remainingCredits,
  passCount,
  latestOrder,
  nextBooking,
  activePass,
}: Props) {
  return (
    <Row className="g-3 mb-4">
      <Col md={4}>
        <Card className="my-stat-card h-100 border-0">
          <Card.Body>
            <div className="my-section-kicker mb-2">Orders</div>
            <div className="my-stat-value">{orderCount}</div>
            <div className="text-muted-soft small">
              {latestOrder ? `최근 주문 ${formatDateTime(latestOrder.createdAt)}` : "아직 주문이 없습니다."}
            </div>
          </Card.Body>
        </Card>
      </Col>
      <Col md={4}>
        <Card className="my-stat-card h-100 border-0">
          <Card.Body>
            <div className="my-section-kicker mb-2">Bookings</div>
            <div className="my-stat-value">{bookingCount}</div>
            <div className="text-muted-soft small">
              {nextBooking ? `다음 일정 ${formatDateTime(nextBooking.startAt)}` : "예정된 예약이 없습니다."}
            </div>
          </Card.Body>
        </Card>
      </Col>
      <Col md={4}>
        <Card className="my-stat-card h-100 border-0">
          <Card.Body>
            <div className="my-section-kicker mb-2">Passes</div>
            <div className="my-stat-value">{remainingCredits}</div>
            <div className="text-muted-soft small">
              {activePass ? `활성 8회권 ${passCount}건` : "보유한 8회권이 없습니다."}
            </div>
          </Card.Body>
        </Card>
      </Col>
    </Row>
  );
}
