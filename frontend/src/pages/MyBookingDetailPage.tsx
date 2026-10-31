import { LinkButton } from "@/shared/ui/LinkButton";
import { Link, useParams, useLocation } from "react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Card } from "react-bootstrap";
import { CancelButton } from "@/features/booking-manage/CancelButton";
import { RescheduleForm } from "@/features/booking-manage/RescheduleForm";
import { ReduceParticipantsForm } from "@/features/booking-manage/ReduceParticipantsForm";
import { myNavLabel } from "@/features/my/myNavigation";
import { MyBookingDetailCard } from "@/features/my-booking/MyBookingDetailCard";
import {
  cancelMyBooking,
  fetchMyBooking,
  reduceMyBookingParticipants,
  rescheduleMyBooking,
} from "@/features/my-booking/api";
import { LoadingSpinner, ErrorAlert, PageHeader } from "@/shared/ui";
import { customerRefundRefetchInterval, isPositiveSafeIntegerString } from "@/shared/lib";
import { NotFoundPage } from "@/pages/NotFoundPage";
import { queryKeys, runForCurrentCustomer } from "@/shared/api";
import { BookingReviewSection } from "@/features/review/BookingReviewSection";
import { bookingCreateHref } from "@/features/booking-create/upcomingSlots";

export function MyBookingDetailPage() {
  const { search } = useLocation();
  const { id } = useParams<{ id: string }>();
  const bookingId = Number(id);
  const validBookingId = isPositiveSafeIntegerString(id);
  const queryClient = useQueryClient();

  const {
    data: booking,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useQuery({
    queryKey: queryKeys.member.bookings.detail(bookingId),
    queryFn: () => runForCurrentCustomer(() => fetchMyBooking(bookingId)),
    enabled: validBookingId,
    refetchInterval: customerRefundRefetchInterval((record) => record.refund?.status),
  });

  if (!validBookingId) return <NotFoundPage />;

  const isBooked = booking?.status === "BOOKED";

  return (
    <>
      <Link to={{ pathname: "/my/bookings", search }} className="page-back-link">
        &larr; {myNavLabel("/my/bookings")}
      </Link>
      <PageHeader
        title="예약 상세"
        actions={(
          <LinkButton
            to={booking ? bookingCreateHref(booking.classId) : "/bookings/new"}
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

          {isBooked && (
            <Card className="mt-4 border-0 my-action-card">
              <Card.Header as="h2">예약 변경</Card.Header>
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

          {/* 잔금까지 낸 예약은 고객이 인원을 바꿀 수 없어 취소 칸의 공방 문의 안내로 모은다. */}
          {isBooked && booking.participantCount > 1 && booking.cancelPolicy.cancellable && (
            <Card className="mt-3 border-0 my-action-card">
              <Card.Header as="h2">예약 인원 변경</Card.Header>
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

          {/* 다가오는 예약은 변경·취소가 먼저 필요하므로 후기는 그 아래에 둔다. */}
          <BookingReviewSection
            bookingId={booking.bookingId}
            className={booking.className}
          />
        </>
      )}
    </>
  );
}
