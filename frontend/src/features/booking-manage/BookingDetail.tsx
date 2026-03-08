import { Card, Row, Col } from "react-bootstrap";
import { StatusBadge } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";
import type { BookingDetailResponse } from "@/shared/types";

interface Props {
  booking: BookingDetailResponse;
}

export function BookingDetail({ booking }: Props) {
  return (
    <Card>
      <Card.Header className="d-flex justify-content-between align-items-center">
        <span>{booking.bookingNumber}</span>
        <StatusBadge status={booking.status} />
      </Card.Header>
      <Card.Body>
        <Row className="g-3">
          <Col xs={6}>
            <small className="text-muted-soft d-block">클래스</small>
            <span>{booking.className}</span>
          </Col>
          <Col xs={6}>
            <small className="text-muted-soft d-block">예약자</small>
            <span>{booking.guestName} ({booking.guestPhone})</span>
          </Col>
          <Col xs={6}>
            <small className="text-muted-soft d-block">시작</small>
            <span>{formatDateTime(booking.startAt)}</span>
          </Col>
          <Col xs={6}>
            <small className="text-muted-soft d-block">종료</small>
            <span>{formatDateTime(booking.endAt)}</span>
          </Col>
          <Col xs={6}>
            <small className="text-muted-soft d-block">예약금</small>
            <span>{formatKRW(booking.depositAmount)}</span>
          </Col>
          <Col xs={6}>
            <small className="text-muted-soft d-block">잔금</small>
            <span>{formatKRW(booking.balanceAmount)}</span>
          </Col>
        </Row>
      </Card.Body>
    </Card>
  );
}
