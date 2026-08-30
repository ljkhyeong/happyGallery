import { Card, Row, Col } from "react-bootstrap";
import { StatusBadge } from "@/shared/ui";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { RefundProgressAlert } from "@/features/refund/RefundProgressAlert";
import { AddBookingToCalendarButton } from "./AddBookingToCalendarButton";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
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
            <small className="text-muted-soft d-block">예약 인원</small>
            <span>{booking.participantCount}명</span>
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
        <div className="mt-3">
          <AddBookingToCalendarButton
            className={booking.className}
            startAt={booking.startAt}
            endAt={booking.endAt}
          />
        </div>
        <RefundProgressAlert refund={booking.refund} />
        <div className="mt-4">
          <WorkshopVisitInfo compact />
        </div>
      </Card.Body>
    </Card>
  );
}
