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
                ? `표시 중인 주문 ${orderCount}건 · 최근 주문 ${formatDateTime(latestOrder.createdAt)}`
                : `표시 중인 주문 ${orderCount}건`}
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
                ? `표시 중인 예약 ${bookingCount}건 · 다음 일정 ${formatDateTime(nextBooking.startAt)}`
                : `표시 중인 예약 ${bookingCount}건 · 예정된 예약 없음`}
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
                ? `사용 가능한 8회권 ${activePassCount}건의 남은 횟수`
                : "사용 가능한 8회권이 없습니다."}
            </div>
          </Card.Body>
        </Card>
      </Col>
    </Row>
  );
}
