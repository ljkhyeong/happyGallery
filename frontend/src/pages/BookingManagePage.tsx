import { useState, useCallback } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card } from "react-bootstrap";
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
      <h4 className="mb-4">예약 조회 <small className="text-muted-soft">(비회원)</small></h4>
      <p className="text-muted-soft small mb-3">
        회원이신가요? <a href="/my">내 정보</a>에서 예약을 확인하세요.
      </p>

      <Card className="mb-4">
        <Card.Body>
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
                <RescheduleForm
                  currentSlotId={booking.slotId}
                  onReschedule={(newSlotId) => rescheduleBooking(booking.bookingId, newSlotId, currentToken)}
                  onSuccess={refetch}
                />
              </Card.Body>
            </Card>
          )}

          {isBooked && (
            <div className="mt-3">
              <CancelButton
                onCancel={() => cancelBooking(booking.bookingId, currentToken)}
                onSuccess={refetch}
              />
            </div>
          )}
        </>
      )}
    </Container>
  );
}
