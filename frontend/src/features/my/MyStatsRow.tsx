import { Card, Col, Row } from "react-bootstrap";
import type { MyBookingSummary, MyOrderSummary } from "./api";
import { formatDateTime } from "@/shared/lib";

interface Props {
  orderCount: number;
  bookingCount: number;
  remainingCredits: number;
  activePassCount: number;
  latestOrder: MyOrderSummary | undefined;
  nextBooking: MyBookingSummary | undefined;
}

export function MyStatsRow({
  orderCount,
  bookingCount,
  remainingCredits,
  activePassCount,
  latestOrder,
  nextBooking,
}: Props) {
  return (
    <Row className="g-3 mb-4">
      <Col md={4}>
        <Card className="my-stat-card h-100 border-0">
          <Card.Body>
            <div className="my-section-kicker mb-2">Orders</div>
            <div className="my-stat-value">{orderCount}</div>
            <div className="text-muted-soft small">
              {latestOrder
                ? `최근 조회 주문 ${orderCount}건 · 최근 주문 ${formatDateTime(latestOrder.createdAt)}`
                : `최근 조회 주문 ${orderCount}건`}
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
              {nextBooking
                ? `최근 조회 예약 ${bookingCount}건 · 다음 일정 ${formatDateTime(nextBooking.startAt)}`
                : `최근 조회 예약 ${bookingCount}건 · 예정된 예약 없음`}
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
              {activePassCount > 0
                ? `최근 조회한 활성 8회권 ${activePassCount}건의 잔여 횟수`
                : "최근 조회한 활성 8회권 0건의 잔여 횟수"}
            </div>
          </Card.Body>
        </Card>
      </Col>
    </Row>
  );
}
