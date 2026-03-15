import { Link, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Button, Card, Container } from "react-bootstrap";
import { CancelButton } from "@/features/booking-manage/CancelButton";
import { RescheduleForm } from "@/features/booking-manage/RescheduleForm";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
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
      <Container className="page-container" style={{ maxWidth: 640 }}>
        <MyAuthGateCard
          title="로그인이 필요합니다"
          description="회원 예약 상세와 변경/취소는 로그인 후 내 정보에서 진행합니다."
        />
      </Container>
    );
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
          <Button as={Link as any} to="/bookings/new" variant="outline-secondary" size="sm">
            새 예약 만들기
          </Button>
        </div>
        <div className="my-section-kicker mb-2">My Booking</div>
        <h4 className="mb-2">예약 상세</h4>
        <p className="text-muted-soft small mb-0">
          예약 상태를 확인하고, 변경 가능한 경우 아래에서 바로 재예약 또는 취소할 수 있습니다.
        </p>
      </div>

      <MyBookingDetailCard booking={booking} />

      {isBooked && (
        <Card className="mt-4 border-0 my-action-card">
          <Card.Header>예약 변경</Card.Header>
          <Card.Body>
            <p className="text-muted-soft small">
              가능한 다른 슬롯으로 즉시 변경합니다. 변경 후에는 현재 예약 상세가 새 슬롯 기준으로 갱신됩니다.
            </p>
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
