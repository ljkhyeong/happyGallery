import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Alert, Card, Col, ListGroup, Row } from "react-bootstrap";
import { Link } from "react-router";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import {
  captureCustomerSession,
  runForCurrentCustomer,
  type CustomerSessionSnapshot,
} from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { ErrorAlert, StatusBadge } from "@/shared/ui";
import { recoverGuestRecords } from "./api";
import {
  clearGuestRecordRecovery,
  loadGuestRecordRecovery,
  saveGuestRecordRecovery,
  type GuestRecordRecoverySession,
} from "./session";

interface RecoveryView {
  customerSession: CustomerSessionSnapshot;
  storage: GuestRecordRecoverySession;
}

function loadRecoveryView(): RecoveryView | null {
  const storage = loadGuestRecordRecovery();
  return storage
    ? { customerSession: captureCustomerSession(), storage }
    : null;
}

export function GuestRecordRecoverySection() {
  const [recoveryView, setRecoveryView] = useState(loadRecoveryView);
  const recovery = useMutation({
    mutationFn: ({ phone, code }: { phone: string; code: string }) =>
      runForCurrentCustomer(
        () => recoverGuestRecords(phone, code),
        (result, requireCurrent) => {
          requireCurrent();
          const customerSession = captureCustomerSession();
          const storage = saveGuestRecordRecovery(result, customerSession);
          requireCurrent();
          if (storage) {
            setRecoveryView({ customerSession, storage });
          }
          return result;
        },
      ),
  });

  const result = recoveryView?.storage.value ?? null;
  const hasRecords = result && (result.orders.length > 0 || result.bookings.length > 0);

  return (
    <Card className="mb-4">
      <Card.Body className="p-4">
        <PhoneVerificationStep
          purpose="GUEST_RECORD_RECOVERY"
          title="주문·예약 조회 정보 복구"
          description="주문 번호나 조회 코드를 잃어버렸다면 결제·예약 때 사용한 휴대폰 번호를 인증하세요. 기존 조회 코드는 폐기되고 새 코드가 발급됩니다."
          confirmLabel="조회 정보 복구"
          confirming={recovery.isPending}
          onReset={() => {
            recovery.reset();
            clearGuestRecordRecovery(recoveryView?.storage);
            setRecoveryView(null);
          }}
          onVerified={(phone, code) => recovery.mutate({ phone, code })}
        />

        <ErrorAlert error={recovery.error} />

        {result && !hasRecords && (
          <Alert variant="light" className="mt-4 mb-0">
            인증한 번호로 확인할 수 있는 비회원 주문이나 예약이 없습니다.
          </Alert>
        )}

        {hasRecords && recoveryView && (
          <div className="mt-4">
            <Alert variant="success">
              조회 정보를 복구했습니다. 새 조회 코드는 {formatDateTime(result.expiresAt)}까지 사용할 수 있습니다.
            </Alert>

            <Row className="g-4">
              {result.orders.length > 0 && (
                <Col xs={12} lg={6}>
                  <h6>비회원 주문</h6>
                  <ListGroup>
                    {result.orders.map((order) => (
                      <ListGroup.Item
                        key={order.orderId}
                        as={Link}
                        to={`/guest/orders?orderId=${order.orderId}`}
                        state={{
                          orderId: order.orderId,
                          token: result.accessToken,
                          customerSession: recoveryView.customerSession,
                        }}
                        action
                        className="d-flex justify-content-between align-items-start gap-3"
                      >
                        <span>
                          <strong className="d-block">주문 #{order.orderId}</strong>
                          <small className="text-muted-soft">
                            {formatKRW(order.totalAmount)} · {formatDateTime(order.createdAt)}
                          </small>
                        </span>
                        <StatusBadge status={order.status} />
                      </ListGroup.Item>
                    ))}
                  </ListGroup>
                </Col>
              )}

              {result.bookings.length > 0 && (
                <Col xs={12} lg={6}>
                  <h6>비회원 예약</h6>
                  <ListGroup>
                    {result.bookings.map((booking) => (
                      <ListGroup.Item
                        key={booking.bookingId}
                        as={Link}
                        to={`/guest/bookings?bookingId=${booking.bookingId}`}
                        state={{
                          bookingId: booking.bookingId,
                          token: result.accessToken,
                          customerSession: recoveryView.customerSession,
                        }}
                        action
                        className="d-flex justify-content-between align-items-start gap-3"
                      >
                        <span>
                          <strong className="d-block">{booking.className}</strong>
                          <small className="text-muted-soft">
                            예약 #{booking.bookingId} · {formatDateTime(booking.startAt)}
                          </small>
                        </span>
                        <StatusBadge status={booking.status} />
                      </ListGroup.Item>
                    ))}
                  </ListGroup>
                </Col>
              )}
            </Row>
          </div>
        )}
      </Card.Body>
    </Card>
  );
}
