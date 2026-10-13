import { Alert, Card, ListGroup } from "react-bootstrap";
import { Link } from "react-router";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import { useOwnedRecoveryResult } from "@/features/guest-recovery/useOwnedRecoveryResult";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { ErrorAlert, StatusBadge } from "@/shared/ui";
import { recoverGuestPaymentStatuses } from "./api";
import {
  clearGuestPaymentStatusRecovery,
  loadGuestPaymentStatusRecovery,
  saveGuestPaymentStatusRecovery,
} from "./session";

const CONTEXT_LABELS = {
  ORDER: "주문",
  BOOKING: "예약",
  PASS: "이용권",
} as const;

export function GuestPaymentStatusRecoverySection() {
  const { view: recoveryView, mutation: recovery, reset } = useOwnedRecoveryResult({
    load: loadGuestPaymentStatusRecovery,
    save: saveGuestPaymentStatusRecovery,
    clear: clearGuestPaymentStatusRecovery,
    request: recoverGuestPaymentStatuses,
  });

  const result = recoveryView?.storage.value ?? null;

  return (
    <Card className="mb-4">
      <Card.Body className="p-4">
        <PhoneVerificationStep
          purpose="GUEST_PAYMENT_STATUS_RECOVERY"
          title="결제 결과 다시 확인"
          description="결제에 쓴 휴대폰 번호를 인증하면 결제 내역을 볼 수 있습니다."
          confirmLabel="결제 결과 다시 확인"
          confirming={recovery.isPending}
          onReset={reset}
          onVerified={(phone, code) => recovery.mutate({ phone, code })}
        />

        <ErrorAlert error={recovery.error} />

        {result && result.payments.length === 0 && (
          <Alert variant="light" className="mt-4 mb-0">
            인증한 번호로 확인할 수 있는 결제가 없습니다.
          </Alert>
        )}

        {result && result.payments.length > 0 && recoveryView && (
          <div className="mt-4">
            <Alert variant="success">
              결제 내역을 찾았습니다. {formatDateTime(result.expiresAt)}까지 확인할 수 있습니다.
            </Alert>
            <ListGroup>
              {result.payments.map((payment) => (
                <ListGroup.Item
                  key={payment.orderId}
                  as={Link}
                  to={`/guest/payments/${encodeURIComponent(payment.orderId)}`}
                  state={{
                    orderId: payment.orderId,
                    statusToken: result.statusToken,
                    customerSession: recoveryView.customerSession,
                  }}
                  action
                  className="d-flex justify-content-between align-items-start gap-3"
                >
                  <span style={{ minWidth: 0 }}>
                    <strong className="d-block">
                      {CONTEXT_LABELS[payment.context]} 결제
                    </strong>
                    <small className="text-muted-soft d-block text-break">
                      {formatKRW(payment.amount)} · {payment.orderId}
                    </small>
                  </span>
                  <StatusBadge status={payment.status} />
                </ListGroup.Item>
              ))}
            </ListGroup>
          </div>
        )}
      </Card.Body>
    </Card>
  );
}
