import { LinkButton } from "@/shared/ui/LinkButton";
import { Link, useParams, useLocation } from "react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Card } from "react-bootstrap";
import { CancelButton } from "@/features/booking-manage/CancelButton";
import { RescheduleForm } from "@/features/booking-manage/RescheduleForm";
import { ReduceParticipantsForm } from "@/features/booking-manage/ReduceParticipantsForm";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { myNavLabel } from "@/features/my/myNavigation";
import { MyBookingDetailCard } from "@/features/my-booking/MyBookingDetailCard";
import { PaymentReceiptLink } from "@/features/payment/PaymentReceiptLink";
import {
  cancelMyBooking,
  fetchMyBooking,
  reduceMyBookingParticipants,
  rescheduleMyBooking,
} from "@/features/my-booking/api";
import { LoadingSpinner, ErrorAlert, PageHeader } from "@/shared/ui";
import { customerRefundPollingInterval, isPositiveSafeIntegerString } from "@/shared/lib";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { queryKeys } from "@/shared/api";
import { BookingReviewSection } from "@/features/review/BookingReviewSection";

export function MyBookingDetailPage() {
  const { search } = useLocation();
  const { id } = useParams<{ id: string }>();
  const bookingId = Number(id);
  const validBookingId = isPositiveSafeIntegerString(id);
  const { isAuthenticated } = useCustomerAuth();
  const queryClient = useQueryClient();

  const {
    data: booking,
    isLoading,
    isFetching,
    error,
    refetch,
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

  const isBooked = booking?.status === "BOOKED";

  return (
    <>
      <Link to={{ pathname: "/my/bookings", search }} className="my-back-link">
        &larr; {myNavLabel("/my/bookings")}
      </Link>
      <PageHeader
        kicker="My page"
        title="예약 상세"
        description="예약 상태를 확인하고, 가능한 경우 날짜·시간을 변경하거나 취소할 수 있습니다."
        actions={(
          <LinkButton
            to={booking ? `/bookings/new?classId=${booking.classId}` : "/bookings/new"}
            variant="outline-dark" size="sm"
          >
            {booking ? "같은 수업 예약" : "새 예약 만들기"}
          </LinkButton>
        )}
      />

      <ErrorAlert error={error} onRetry={() => void refetch()} retrying={isFetching} />
      {isLoading && <LoadingSpinner />}
      {booking && (
        <>
          <MyBookingDetailCard booking={booking} />
          {booking.receiptUrl && <div className="mb-3"><PaymentReceiptLink receiptUrl={booking.receiptUrl} /></div>}

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

          {isBooked && booking.participantCount > 1 && (
            <Card className="mt-3 border-0 my-action-card">
              <Card.Header>예약 인원 변경</Card.Header>
              <Card.Body>
                <p className="text-muted-soft small">
                  취소 마감 전에는 한 명 이상을 남겨 일부 인원만 취소할 수 있습니다.
                </p>
                <ReduceParticipantsForm
                  participantCount={booking.participantCount}
                  depositAmount={booking.depositAmount}
                  cancelPolicy={booking.cancelPolicy}
                  passBooking={booking.passBooking}
                  onReduce={(participantCount) =>
                    reduceMyBookingParticipants(booking.bookingId, participantCount)}
                  onSuccess={() =>
                    queryClient.invalidateQueries({
                      queryKey: queryKeys.member.bookings.all,
                    })}
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
                depositAmount={booking.depositAmount}
              />
            </div>
          )}
        </>
      )}
    </>
  );
}
