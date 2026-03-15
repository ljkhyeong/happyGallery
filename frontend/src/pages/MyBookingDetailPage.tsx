import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Button, Card, Container } from "react-bootstrap";
import { CancelButton } from "@/features/booking-manage/CancelButton";
import { RescheduleForm } from "@/features/booking-manage/RescheduleForm";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { MyBookingDetailCard } from "@/features/my-booking/MyBookingDetailCard";
import { cancelMyBooking, fetchMyBooking, rescheduleMyBooking } from "@/features/my-booking/api";
import { LoadingSpinner, ErrorAlert } from "@/shared/ui";

export function MyBookingDetailPage() {
  const { id } = useParams<{ id: string }>();
  const bookingId = Number(id);
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();

  const {
    data: booking,
    isLoading,
    error,
    refetch,
  } = useQuery({
    queryKey: ["my", "bookings", bookingId],
    queryFn: () => fetchMyBooking(bookingId),
    enabled: isAuthenticated && bookingId > 0,
  });

  if (authLoading || isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (error) {
    return <Container className="page-container"><ErrorAlert error={error} /></Container>;
  }

  if (!isAuthenticated) {
    return (
      <Container className="page-container text-center" style={{ maxWidth: 480 }}>
        <h5 className="mb-3">로그인이 필요합니다</h5>
        <Button as={Link as any} to="/login" variant="primary">로그인</Button>
      </Container>
    );
  }

  if (!booking) return null;

  const isBooked = booking.status === "BOOKED";

  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      <Link to="/my" className="text-decoration-none small d-block mb-3">
        &larr; 내 정보
      </Link>

      <MyBookingDetailCard booking={booking} />

      {isBooked && (
        <Card className="mt-4">
          <Card.Header>예약 변경</Card.Header>
          <Card.Body>
            <RescheduleForm
              currentSlotId={booking.slotId}
              onReschedule={(newSlotId) => rescheduleMyBooking(booking.bookingId, newSlotId)}
              onSuccess={() => {
                void refetch();
              }}
              successMessage="회원 예약이 변경되었습니다."
            />
          </Card.Body>
        </Card>
      )}

      {isBooked && (
        <div className="mt-3">
          <CancelButton
            onCancel={() => cancelMyBooking(booking.bookingId)}
            onSuccess={() => {
              void refetch();
            }}
          />
        </div>
      )}
    </Container>
  );
}
