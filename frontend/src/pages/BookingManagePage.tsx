import { LinkButton } from "@/shared/ui/LinkButton";
import { useState } from "react";
import { skipToken, useQuery } from "@tanstack/react-query";
import { Button, Container, Card } from "react-bootstrap";
import { useLocation, useSearchParams } from "react-router";
import {
  cancelBooking,
  fetchBooking,
  reduceBookingParticipants,
  rescheduleBooking,
} from "@/features/booking-manage/api";
import { GuestLookupPanel } from "@/features/guest-lookup/GuestLookupPanel";
import { GuestMemberGuide } from "@/features/guest-lookup/GuestMemberGuide";
import { BookingLookupForm } from "@/features/booking-manage/BookingLookupForm";
import { BookingDetail } from "@/features/booking-manage/BookingDetail";
import { RescheduleForm } from "@/features/booking-manage/RescheduleForm";
import { CancelButton } from "@/features/booking-manage/CancelButton";
import { ReduceParticipantsForm } from "@/features/booking-manage/ReduceParticipantsForm";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { ErrorAlert } from "@/shared/ui";
import { customerRefundPollingInterval } from "@/shared/lib";
import { loadGuestRecordRecovery } from "@/features/guest-recovery/session";
import {
  isCurrentCustomerSessionState,
  runForCurrentCustomer,
  type CustomerSessionOwnedState,
} from "@/shared/api";

interface LocationState extends CustomerSessionOwnedState {
  bookingId?: number;
  token?: string;
}

interface BookingLookup {
  credentials: {
    bookingId: number;
    token: string;
  };
  requestId: string;
}

export function BookingManagePage() {
  const { sessionVersion } = useCustomerAuth();
  return <BookingManageContent key={sessionVersion} />;
}

function BookingManageContent() {
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const navState = isCurrentCustomerSessionState(location.state)
    ? location.state as LocationState
    : null;
  const [initialCredentials] = useState(() => {
    const queryBookingId = Number(searchParams.get("bookingId"));
    const bookingId = navState?.bookingId
      ?? (Number.isSafeInteger(queryBookingId) && queryBookingId > 0 ? queryBookingId : undefined);
    const token = navState?.token
      ?? loadGuestRecordRecovery()?.value.accessToken
      ?? "";
    return { bookingId, token: token.trim() };
  });
  const [lookup, setLookup] = useState<BookingLookup | null>(() =>
    initialCredentials.bookingId && initialCredentials.token
      ? {
          credentials: {
            bookingId: initialCredentials.bookingId,
            token: initialCredentials.token,
          },
          requestId: crypto.randomUUID(),
        }
      : null,
  );
  const {
    data: booking,
    error,
    isFetching,
    refetch: refetchBooking,
  } = useQuery({
    queryKey: ["guest", "booking", lookup?.credentials.bookingId, lookup?.requestId],
    queryFn: lookup
      ? () => runForCurrentCustomer(
          () => fetchBooking(
            lookup.credentials.bookingId,
            lookup.credentials.token,
          ),
        )
      : skipToken,
    gcTime: 0,
    refetchInterval: ({ state }) =>
      customerRefundPollingInterval(
        state.data?.refund?.status,
        state.dataUpdateCount + state.fetchFailureCount,
      ),
  });

  const [relookupOpen, setRelookupOpen] = useState(false);

  function handleLookup(bookingId: number, token: string) {
    setRelookupOpen(false);
    if (
      lookup?.credentials.bookingId === bookingId &&
      lookup.credentials.token === token
    ) {
      void refetchBooking();
      return;
    }
    setLookup({
      credentials: { bookingId, token },
      requestId: crypto.randomUUID(),
    });
  }

  async function refreshBooking() {
    await refetchBooking();
  }

  const isBooked = booking?.status === "BOOKED";
  const currentToken = lookup?.credentials.token ?? "";

  return (
    <Container className="page-container guest-lookup-page">
      {Boolean(booking) && (
        <header className="guest-detail-header">
          <div>
            <p className="store-section-kicker mb-1">Guest booking</p>
            <h1>비회원 예약 조회</h1>
            <p>예약 일정과 변경·취소 가능 여부를 확인하세요. 조회 코드는 다른 사람과 공유하지 마세요.</p>
          </div>
          <div className="guest-detail-actions">
            <Button variant="outline-dark" size="sm" disabled={isFetching} onClick={() => void refetchBooking()}>
              {isFetching ? "확인 중..." : "최신 상태 확인"}
            </Button>
            <Button
              variant="outline-dark"
              size="sm"
              aria-expanded={relookupOpen}
              aria-controls="guest-relookup"
              onClick={() => setRelookupOpen((open) => !open)}
            >
              {relookupOpen ? "다른 번호 입력 닫기" : "다른 번호로 찾기"}
            </Button>
          </div>
        </header>
      )}
      {/* 조회 후에도 입력값은 유지해야 하므로 지우지 않고 숨긴다. */}
      <div id="guest-relookup" hidden={Boolean(booking) && !relookupOpen}>
        <GuestLookupPanel title="비회원 예약 조회" kind="bookings">
          <BookingLookupForm
            onLookup={handleLookup}
            isLoading={isFetching}
            initialBookingId={initialCredentials.bookingId
              ? String(initialCredentials.bookingId)
              : undefined}
            initialToken={initialCredentials.token || undefined}
          />
        </GuestLookupPanel>
      </div>

      <ErrorAlert error={booking ? null : error} />

      {booking && (
        <>
          <BookingDetail booking={booking} />
          <LinkButton to={`/bookings/new?classId=${booking.classId}`}
            variant="outline-dark" className="mt-3">같은 수업 예약</LinkButton>

          {isBooked && (
            <Card className="mt-4 my-action-card border-0">
              <Card.Header>예약 변경</Card.Header>
              <Card.Body>
                <p className="text-muted-soft small mb-3">
                  같은 클래스의 예약 가능한 날짜와 시간으로 변경할 수 있습니다.
                </p>
                <RescheduleForm
                  classId={booking.classId}
                  className={booking.className}
                  currentSlotId={booking.slotId}
                  currentStartAt={booking.startAt}
                  participantCount={booking.participantCount}
                  onReschedule={(newSlotId) => rescheduleBooking(booking.bookingId, newSlotId, currentToken)}
                  onSuccess={refreshBooking}
                />
              </Card.Body>
            </Card>
          )}

          {isBooked && booking.participantCount > 1 && (
            <Card className="mt-3 my-action-card border-0">
              <Card.Header>예약 인원 변경</Card.Header>
              <Card.Body>
                <p className="text-muted-soft small mb-3">
                  취소 마감 전에는 한 명 이상을 남겨 일부 인원만 취소할 수 있습니다.
                </p>
                <ReduceParticipantsForm
                  participantCount={booking.participantCount}
                  depositAmount={booking.depositAmount}
                  cancelPolicy={booking.cancelPolicy}
                  onReduce={(participantCount) => reduceBookingParticipants(
                    booking.bookingId,
                    participantCount,
                    currentToken,
                  )}
                  onSuccess={refreshBooking}
                />
              </Card.Body>
            </Card>
          )}

          {isBooked && (
            <Card className="mt-3 my-action-card border-0">
              <Card.Header>예약 취소</Card.Header>
              <Card.Body>
                <p className="text-muted-soft small mb-3">
                  취소 결과는 이 화면에 표시됩니다.
                </p>
                <CancelButton
                  onCancel={() => cancelBooking(booking.bookingId, currentToken)}
                  onSuccess={refreshBooking}
                  cancelPolicy={booking.cancelPolicy}
                  depositAmount={booking.depositAmount}
                />
              </Card.Body>
            </Card>
          )}
        </>
      )}
      <GuestMemberGuide source="guest_booking_lookup" />
    </Container>
  );
}
