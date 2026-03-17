import { useState, useCallback, useEffect, useRef } from "react";
import { useMutation } from "@tanstack/react-query";
import { Container, Card, Button, Badge } from "react-bootstrap";
import { Link, useLocation } from "react-router-dom";
import { cancelBooking, fetchBooking, rescheduleBooking } from "@/features/booking-manage/api";
import { BookingLookupForm } from "@/features/booking-manage/BookingLookupForm";
import { BookingDetail } from "@/features/booking-manage/BookingDetail";
import { RescheduleForm } from "@/features/booking-manage/RescheduleForm";
import { CancelButton } from "@/features/booking-manage/CancelButton";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { trackGuestMemberCta } from "@/features/monitoring/api";
import { ErrorAlert } from "@/shared/ui";
import type { BookingDetailResponse } from "@/shared/types";

interface LocationState {
  bookingId?: number;
  token?: string;
}

export function BookingManagePage() {
  const location = useLocation();
  const navState = location.state as LocationState | null;
  const [booking, setBooking] = useState<BookingDetailResponse | null>(null);
  const [currentToken, setCurrentToken] = useState("");
  const claimLoginHref = buildAuthPageHref("/login", {
    redirectTo: "/my?claim=1",
    claim: true,
  });
  const claimSignupHref = buildAuthPageHref("/signup", {
    redirectTo: "/my?claim=1",
    claim: true,
  });

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

  const autoLookupDone = useRef(false);
  useEffect(() => {
    if (!autoLookupDone.current && navState?.bookingId && navState?.token) {
      autoLookupDone.current = true;
      lookup.mutate({ bookingId: navState.bookingId, token: navState.token });
    }
  }, [navState, lookup]);

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
            이 경로는 이미 완료한 비회원 예약을 조회, 변경, 취소하는 보조 경로입니다.
            회원은 <strong>내 정보</strong>에서 예약 목록과 상세를 바로 확인하고 더 자연스럽게 이어갈 수 있습니다.
          </p>
          <div className="d-flex flex-wrap gap-2">
            <Button as={Link as any} to="/my" variant="dark" size="sm">
              회원 내 정보
            </Button>
            <Button
              as={Link as any}
              to={claimLoginHref}
              variant="outline-secondary"
              size="sm"
              onClick={() => trackGuestMemberCta("guest_booking_lookup", "login")}
            >
              로그인하고 가져오기
            </Button>
            <Button
              as={Link as any}
              to={claimSignupHref}
              variant="outline-secondary"
              size="sm"
              onClick={() => trackGuestMemberCta("guest_booking_lookup", "signup")}
            >
              회원가입
            </Button>
            <Button as={Link as any} to="/bookings/new" variant="outline-secondary" size="sm">
              새 예약 만들기
            </Button>
          </div>
          <div className="guest-route-note mt-3">
            <div className="guest-route-note-title">Guest route policy</div>
            <div className="small text-muted-soft">
              비회원 예약은 토큰으로 바로 관리할 수 있고, 회원 전환 후에는 `/my`에서 같은 번호 기준 claim으로 이어서 볼 수 있습니다.
            </div>
          </div>
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Body>
          <div className="legacy-order-step-label mb-2">예약 ID + 토큰 입력</div>
          <p className="text-muted-soft small mb-3">
            조회가 끝나면 같은 화면에서 슬롯 변경과 취소까지 이어서 진행할 수 있습니다.
          </p>
          <BookingLookupForm
            onLookup={handleLookup}
            isLoading={lookup.isPending}
            initialBookingId={navState?.bookingId ? String(navState.bookingId) : undefined}
            initialToken={navState?.token}
          />
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
