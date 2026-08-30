import { Card, Row, Col } from "react-bootstrap";
import { StatusBadge } from "@/shared/ui";
import { BOOKING_BALANCE_STATUS_LABEL, formatDateTime, formatKRW } from "@/shared/lib";
import { RefundProgressAlert } from "@/features/refund/RefundProgressAlert";
import { AddBookingToCalendarButton } from "@/features/booking-manage/AddBookingToCalendarButton";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
import type { MyBookingDetailResponse } from "@/shared/types";

interface Props {
  booking: MyBookingDetailResponse;
}

export function MyBookingDetailCard({ booking }: Props) {
  return (
    <Card>
      <Card.Header className="d-flex justify-content-between align-items-center">
        <span>예약 #{booking.bookingId}</span>
        <StatusBadge status={booking.status} />
      </Card.Header>
      <Card.Body>
        <Row className="g-3">
          <Col xs={12}>
            <small className="text-muted-soft d-block">클래스</small>
            <span>{booking.className}</span>
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
          <Col xs={6}>
            <small className="text-muted-soft d-block">잔금 상태</small>
            <span>{BOOKING_BALANCE_STATUS_LABEL[booking.balanceStatus] ?? "확인 필요"}</span>
          </Col>
          <Col xs={6}>
            <small className="text-muted-soft d-block">결제 경로</small>
            <span>{booking.passBooking ? "8회권 사용" : "예약금 결제"}</span>
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
