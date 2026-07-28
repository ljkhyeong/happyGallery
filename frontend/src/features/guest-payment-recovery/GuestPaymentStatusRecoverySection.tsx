import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { Alert, Card, ListGroup } from "react-bootstrap";
import { Link } from "react-router";
import { PhoneVerificationStep } from "@/features/booking-create/PhoneVerificationStep";
import {
  captureCustomerSession,
  runForCurrentCustomer,
} from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { ErrorAlert, StatusBadge } from "@/shared/ui";
import { recoverGuestPaymentStatuses } from "./api";
import {
  clearGuestPaymentStatusRecovery,
  loadGuestPaymentStatusRecovery,
  saveGuestPaymentStatusRecovery,
  type GuestPaymentStatusRecoverySession,
} from "./session";

const CONTEXT_LABELS = {
  ORDER: "주문",
  BOOKING: "예약",
  PASS: "8회권",
} as const;

export function GuestPaymentStatusRecoverySection() {
  const [storedRecovery, setStoredRecovery] =
    useState<GuestPaymentStatusRecoverySession | null>(
      loadGuestPaymentStatusRecovery,
    );
  const recovery = useMutation({
    mutationFn: ({ phone, code }: { phone: string; code: string }) =>
      runForCurrentCustomer(
        () => recoverGuestPaymentStatuses(phone, code),
        (result, requireCurrent) => {
          requireCurrent();
          const customerSession = captureCustomerSession();
          const storage = saveGuestPaymentStatusRecovery(
            result,
            customerSession,
          );
          requireCurrent();
          if (storage) {
            setStoredRecovery(storage);
          }
          return result;
        },
      ),
  });

  const result = storedRecovery?.value ?? null;

  return (
    <Card className="mb-4">
      <Card.Body className="p-4">
        <PhoneVerificationStep
          purpose="GUEST_PAYMENT_STATUS_RECOVERY"
          title="처리 중인 결제 결과 복구"
          description="결제 완료 화면을 닫았거나 결제 상태 조회 정보가 없다면 결제 때 사용한 휴대폰 번호를 인증하세요. 해당 번호의 결제 목록과 상태 조회 권한을 복구합니다."
          confirmLabel="결제 결과 복구"
          confirming={recovery.isPending}
          onReset={() => {
            recovery.reset();
            clearGuestPaymentStatusRecovery(storedRecovery ?? undefined);
            setStoredRecovery(null);
          }}
          onVerified={(phone, code) => recovery.mutate({ phone, code })}
        />

        <ErrorAlert error={recovery.error} />

        {result && result.payments.length === 0 && (
          <Alert variant="light" className="mt-4 mb-0">
            인증한 번호로 확인할 수 있는 결제가 없습니다.
          </Alert>
        )}

        {result && result.payments.length > 0 && (
          <div className="mt-4">
            <Alert variant="success">
              결제 상태 조회 정보를 복구했습니다. {formatDateTime(result.expiresAt)}까지 확인할 수 있습니다.
            </Alert>
            <ListGroup>
              {result.payments.map((payment) => (
                <ListGroup.Item
                  key={payment.orderId}
                  as={Link}
                  to={`/guest/payments/${encodeURIComponent(payment.orderId)}`}
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
