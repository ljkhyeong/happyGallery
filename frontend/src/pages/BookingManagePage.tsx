import { LinkButton } from "@/shared/ui/LinkButton";
import { Container, Card } from "react-bootstrap";
import {
  cancelBooking,
  fetchBooking,
  reduceBookingParticipants,
  rescheduleBooking,
} from "@/features/booking-manage/api";
import { GuestMemberGuide } from "@/features/guest-lookup/GuestMemberGuide";
import { GuestRecordDetailHeader } from "@/features/guest-lookup/GuestRecordDetailHeader";
import { useGuestRecordLookup } from "@/features/guest-lookup/useGuestRecordLookup";
import { BookingDetail } from "@/features/booking-manage/BookingDetail";
import { RescheduleForm } from "@/features/booking-manage/RescheduleForm";
import { CancelButton } from "@/features/booking-manage/CancelButton";
import { ReduceParticipantsForm } from "@/features/booking-manage/ReduceParticipantsForm";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { ErrorAlert } from "@/shared/ui";
import { bookingCreateHref } from "@/features/booking-create/upcomingSlots";

export function BookingManagePage() {
  const { sessionVersion } = useCustomerAuth();
  return <BookingManageContent key={sessionVersion} />;
}

function BookingManageContent() {
  const bookingLookup = useGuestRecordLookup("booking", fetchBooking);
  const { lookup, query: { data: booking, error, refetch: refetchBooking } } = bookingLookup;

  async function refreshBooking() {
    await refetchBooking();
  }

  const isBooked = booking?.status === "BOOKED";
  const currentToken = lookup?.credentials.token ?? "";

  return (
    <Container className="page-container guest-lookup-page">
      <GuestRecordDetailHeader
        lookup={bookingLookup}
        found={Boolean(booking)}
        description="조회 코드는 다른 사람과 공유하지 마세요."
      />

      <ErrorAlert error={booking ? null : error} />

      {booking && (
        <>
          <BookingDetail booking={booking} />
          <LinkButton to={bookingCreateHref(booking.classId)}
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
      <GuestMemberGuide />
    </Container>
  );
}
