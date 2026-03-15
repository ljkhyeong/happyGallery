import { useState, useCallback } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card, Button, Badge } from "react-bootstrap";
import { Link } from "react-router-dom";
import { cancelBooking, fetchBooking, rescheduleBooking } from "@/features/booking-manage/api";
import { BookingLookupForm } from "@/features/booking-manage/BookingLookupForm";
import { BookingDetail } from "@/features/booking-manage/BookingDetail";
import { RescheduleForm } from "@/features/booking-manage/RescheduleForm";
import { CancelButton } from "@/features/booking-manage/CancelButton";
import { ErrorAlert } from "@/shared/ui";
import type { BookingDetailResponse } from "@/shared/types";

export function BookingManagePage() {
  const [booking, setBooking] = useState<BookingDetailResponse | null>(null);
  const [currentToken, setCurrentToken] = useState("");

  const lookup = useMutation({
    mutationFn: ({ bookingId, token }: { bookingId: number; token: string }) =>
      fetchBooking(bookingId, token),
    onSuccess: (data, variables) => {
      setBooking(data);
      setCurrentToken(variables.token);
    },
    onError: () => {
      setBooking(null);
      setCurrentToken("");
    },
  });

  const handleLookup = useCallback(
    (bookingId: number, token: string) => lookup.mutate({ bookingId, token }),
    [lookup],
  );

  const refetch = useCallback(() => {
    if (booking && currentToken) {
      lookup.mutate({ bookingId: booking.bookingId, token: currentToken });
    }
  }, [booking, currentToken, lookup]);

  const isBooked = booking?.status === "BOOKED";

  return (
    <Container className="page-container">
      <Card className="legacy-order-banner mb-4 border-0">
        <Card.Body className="p-4">
          <Badge bg="light" text="dark" className="mb-2">Guest Lookup</Badge>
          <h4 className="mb-2">비회원 예약 조회</h4>
          <p className="text-muted-soft mb-3">
            예약 완료 후 받은 예약 ID와 access token으로 조회하고, 같은 token으로 변경이나 취소까지 이어집니다.
            회원은 <strong>내 정보</strong>에서 예약을 바로 확인할 수 있습니다.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <Button as={Link as any} to="/my" variant="dark" size="sm">
              회원 내 정보
            </Button>
            <Button as={Link as any} to="/signup" variant="outline-secondary" size="sm">
              회원가입
            </Button>
            <Button as={Link as any} to="/bookings/new" variant="outline-secondary" size="sm">
              새 예약 만들기
            </Button>
          </div>
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Body>
          <div className="legacy-order-step-label mb-2">예약 ID + 토큰 입력</div>
          <p className="text-muted-soft small mb-3">
            조회가 끝나면 같은 화면에서 슬롯 변경과 취소까지 이어서 진행할 수 있습니다.
          </p>
          <BookingLookupForm onLookup={handleLookup} isLoading={lookup.isPending} />
        </Card.Body>
      </Card>

      <ErrorAlert error={lookup.error} />

      {booking && (
        <>
          <BookingDetail booking={booking} />

          {isBooked && (
            <Card className="mt-4">
              <Card.Header>예약 변경</Card.Header>
              <Card.Body>
                <p className="text-muted-soft small mb-3">
                  새 슬롯 ID를 입력하면 동일한 비회원 토큰으로 예약 시간을 다시 잡을 수 있습니다.
                </p>
                <RescheduleForm
                  currentSlotId={booking.slotId}
                  onReschedule={(newSlotId) => rescheduleBooking(booking.bookingId, newSlotId, currentToken)}
                  onSuccess={refetch}
                />
              </Card.Body>
            </Card>
          )}

          {isBooked && (
            <Card className="mt-3 my-action-card border-0">
              <Card.Header>예약 취소</Card.Header>
              <Card.Body>
                <p className="text-muted-soft small mb-3">
                  예약 취소 후 상태를 이 화면에서 바로 다시 확인할 수 있습니다.
                </p>
                <CancelButton
                  onCancel={() => cancelBooking(booking.bookingId, currentToken)}
                  onSuccess={refetch}
                />
              </Card.Body>
            </Card>
          )}
        </>
      )}
    </Container>
  );
}
