import { LinkButton } from "@/shared/ui/LinkButton";
import { Link, useParams } from "react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Card, Container } from "react-bootstrap";
import { CancelButton } from "@/features/booking-manage/CancelButton";
import { RescheduleForm } from "@/features/booking-manage/RescheduleForm";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { MyBookingDetailCard } from "@/features/my-booking/MyBookingDetailCard";
import { cancelMyBooking, fetchMyBooking, rescheduleMyBooking } from "@/features/my-booking/api";
import { LoadingSpinner, ErrorAlert } from "@/shared/ui";
import { customerRefundPollingInterval, isPositiveSafeIntegerString } from "@/shared/lib";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { queryKeys } from "@/shared/api";
import { BookingReviewSection } from "@/features/review/BookingReviewSection";

export function MyBookingDetailPage() {
  const { id } = useParams<{ id: string }>();
  const bookingId = Number(id);
  const validBookingId = isPositiveSafeIntegerString(id);
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
  const queryClient = useQueryClient();

  const {
    data: booking,
    isLoading,
    error,
  } = useQuery({
    queryKey: queryKeys.member.bookings.detail(bookingId),
    queryFn: () => fetchMyBooking(bookingId),
    enabled: isAuthenticated && validBookingId,
    refetchInterval: ({ state }) =>
      customerRefundPollingInterval(
        state.data?.refund?.status,
        state.dataUpdateCount + state.fetchFailureCount,
      ),
  });

  if (!validBookingId) return <NotFoundPage />;

  if (authLoading || isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (!isAuthenticated) {
    return (
      <Container className="page-container" style={{ maxWidth: 640 }}>
        <MyAuthGateCard
          title="로그인이 필요합니다"
          description="회원 예약 상세와 변경/취소는 로그인 후 내 정보에서 진행합니다."
        />
      </Container>
    );
  }

  if (error && !booking) {
    return <Container className="page-container"><ErrorAlert error={error} /></Container>;
  }

  if (!booking) return null;

  const isBooked = booking.status === "BOOKED";

  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      <div className="my-detail-header">
        <div className="d-flex flex-wrap justify-content-between gap-2 align-items-start mb-3">
          <Link to="/my/bookings" className="text-decoration-none small">
            &larr; 내 예약
          </Link>
          <LinkButton to="/bookings/new" variant="outline-secondary" size="sm">
            새 예약 만들기
          </LinkButton>
        </div>
        <div className="my-section-kicker mb-2">My Booking</div>
        <h4 className="mb-2">예약 상세</h4>
        <p className="text-muted-soft small mb-0">
          예약 상태를 확인하고, 변경 가능한 경우 아래에서 바로 재예약 또는 취소할 수 있습니다.
        </p>
      </div>

      <MyBookingDetailCard booking={booking} />

      {error && <ErrorAlert error={error} />}

      <BookingReviewSection
        bookingId={booking.bookingId}
        className={booking.className}
      />

      {isBooked && (
        <Card className="mt-4 border-0 my-action-card">
          <Card.Header>예약 변경</Card.Header>
          <Card.Body>
            <p className="text-muted-soft small">
              예약 가능한 다른 날짜와 시간으로 바로 변경합니다. 변경 후에는 예약 상세에 새 일정이 표시됩니다.
            </p>
            <RescheduleForm
              classId={booking.classId}
              className={booking.className}
              currentSlotId={booking.slotId}
              currentStartAt={booking.startAt}
              participantCount={booking.participantCount}
              onReschedule={(newSlotId) =>
                rescheduleMyBooking(booking.bookingId, newSlotId)}
              onSuccess={() =>
                queryClient.invalidateQueries({
                  queryKey: queryKeys.member.bookings.all,
                })}
              successMessage="회원 예약이 변경되었습니다."
            />
          </Card.Body>
        </Card>
      )}

      {isBooked && (
        <div className="mt-3">
          <CancelButton
            onCancel={() => cancelMyBooking(booking.bookingId)}
            onSuccess={() =>
              queryClient.invalidateQueries({
                queryKey: queryKeys.member.bookings.all,
              })}
            cancelPolicy={booking.cancelPolicy}
          />
        </div>
      )}
    </Container>
  );
}
