import { useCallback, useEffect, useRef, useState } from "react";
import { Alert, Container, Spinner, Button } from "react-bootstrap";
import { useNavigate, useSearchParams } from "react-router-dom";
import {
  confirmPayment,
  consumePaymentReturnHint,
  fetchPaymentStatus,
  PaymentCompletionNext,
  PaymentStatusNotice,
  readPaymentStatusToken,
  shouldPollPaymentStatus,
  type ConfirmPaymentResponse,
  type PaymentStatusResponse,
} from "@/features/payment";
import { isPositiveSafeIntegerString } from "@/shared/lib";
import { ErrorAlert } from "@/shared/ui";
import { ApiError } from "@/shared/api";

function canRetryConfirm(error: unknown): boolean {
  if (error instanceof ApiError) {
    return error.code === "PAYMENT_CONFIRM_IN_PROGRESS"
      || error.code === "PAYMENT_CONFIRM_RETRYABLE"
      || error.code === "SERVICE_UNAVAILABLE";
  }
  return error instanceof TypeError
    || (error instanceof Error && error.name === "AbortError");
}

function requiresPaymentReconciliation(error: unknown): boolean {
  return error instanceof ApiError
    && error.code === "PAYMENT_RECONCILIATION_REQUIRED";
}

export function PaymentSuccessPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [error, setError] = useState<unknown>(null);
  const [result, setResult] = useState<ConfirmPaymentResponse | null>(null);
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatusResponse | null>(null);
  const [statusError, setStatusError] = useState("");
  const [confirming, setConfirming] = useState(true);
  const calledRef = useRef(false);
  const latestStatusRequestRef = useRef(0);
  const completedRef = useRef(false);

  const callbackRef = useRef({
    paymentKey: params.get("paymentKey")?.trim() ?? "",
    orderId: params.get("orderId")?.trim() ?? "",
    amount: params.get("amount"),
  });
  const paymentKey = callbackRef.current.paymentKey;
  const orderId = callbackRef.current.orderId;
  const amountStr = callbackRef.current.amount;
  const amount = Number(amountStr);
  const validAmount = isPositiveSafeIntegerString(amountStr);

  const checkStatus = useCallback(async () => {
    if (!orderId) throw new Error("결제 주문번호가 없습니다.");
    const requestId = ++latestStatusRequestRef.current;
    const status = await fetchPaymentStatus(orderId, readPaymentStatusToken(orderId));
    if (requestId !== latestStatusRequestRef.current || completedRef.current) {
      return status;
    }
    setStatusError("");
    setPaymentStatus(status);
    if (status.status === "COMPLETED" && status.domainId != null) {
      completedRef.current = true;
      setResult({
        context: status.context,
        domainId: status.domainId,
        accessToken: status.accessToken,
        accessRecoveryRequired: status.accessRecoveryRequired,
      });
      setError(null);
      consumePaymentReturnHint();
    }
    return status;
  }, [orderId]);

  const runConfirm = useCallback(async () => {
    if (!paymentKey || !orderId || !validAmount) {
      setError(new Error("결제 정보가 올바르지 않습니다."));
      setConfirming(false);
      return;
    }

    setError(null);
    setStatusError("");
    setPaymentStatus(null);
    setConfirming(true);
    try {
      const response = await confirmPayment(
        { paymentKey, orderId, amount },
        readPaymentStatusToken(orderId),
      );
      completedRef.current = true;
      setResult(response);
      consumePaymentReturnHint();
    } catch (requestError) {
      setError(requestError);
      try {
        await checkStatus();
      } catch {
        // 과거 결제나 유실된 비회원 토큰은 기존 confirm 오류 안내를 유지한다.
      }
    } finally {
      setConfirming(false);
    }
  }, [paymentKey, orderId, amount, validAmount, checkStatus]);

  useEffect(() => {
    if (calledRef.current) return;
    calledRef.current = true;
    void runConfirm();
  }, [runConfirm]);

  useEffect(() => {
    window.history.replaceState(window.history.state, "", "/payments/success");
  }, []);

  useEffect(() => {
    const polling = shouldPollPaymentStatus(paymentStatus?.status);
    if (!polling) return;
    let active = true;
    let timer: number | undefined;

    const poll = async () => {
      try {
        const status = await checkStatus();
        const shouldContinue = shouldPollPaymentStatus(status.status);
        if (active && shouldContinue) {
          timer = window.setTimeout(() => void poll(), 3_000);
        }
      } catch {
        if (active) {
          setStatusError("상태를 새로 확인하지 못했습니다. 잠시 후 자동으로 다시 확인합니다.");
          timer = window.setTimeout(() => void poll(), 3_000);
        }
      }
    };

    timer = window.setTimeout(() => void poll(), 3_000);
    return () => {
      active = false;
      if (timer !== undefined) window.clearTimeout(timer);
    };
  }, [checkStatus, paymentStatus?.status]);

  if (confirming) {
    return (
      <Container className="page-container text-center" style={{ maxWidth: 540 }}>
        <Spinner animation="border" role="status" className="mb-3" />
        <p className="text-muted-soft">결제를 확정하고 있습니다...</p>
      </Container>
    );
  }

  if (paymentStatus && paymentStatus.status !== "COMPLETED") {
    return (
      <Container className="page-container" style={{ maxWidth: 540 }}>
        <PaymentStatusNotice status={paymentStatus} />
        {statusError && <Alert variant="warning" className="mt-3 mb-0">{statusError}</Alert>}
        <div className="d-flex gap-2 mt-3">
          {(paymentStatus.status === "READY" || paymentStatus.status === "RETRYABLE") && (
            <Button variant="primary" onClick={() => void runConfirm()}>
              결제 결과 다시 확인
            </Button>
          )}
          {(paymentStatus.status === "REVIEW_REQUIRED"
            || paymentStatus.status === "SUPPORT_REQUIRED") && (
            <Button variant="primary" onClick={() => void checkStatus().catch(() => {
              setStatusError("상태를 새로 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.");
            })}>
              상태 새로고침
            </Button>
          )}
          <Button variant="outline-secondary" onClick={() => navigate("/")}>홈으로</Button>
        </div>
      </Container>
    );
  }

  if (error) {
    const retryable = canRetryConfirm(error);
    const reconciliationRequired = requiresPaymentReconciliation(error);
    const statusCheckRequired = retryable || reconciliationRequired;
    return (
      <Container className="page-container" style={{ maxWidth: 540 }}>
        <h4 className="mb-4">{statusCheckRequired ? "결제 상태 확인 필요" : "결제 확정 실패"}</h4>
        <ErrorAlert error={error} />
        <div className="d-flex gap-2 mt-3">
          {retryable && (
            <Button variant="primary" onClick={() => void runConfirm()}>
              다시 확인
            </Button>
          )}
          <Button variant="outline-secondary" onClick={() => navigate("/")}>홈으로</Button>
          {!retryable && !reconciliationRequired && (
            <Button variant="primary" onClick={() => navigate(-1)}>이전으로</Button>
          )}
        </div>
      </Container>
    );
  }

  if (!result) {
    return null;
  }

  return (
    <Container className="page-container" style={{ maxWidth: 540 }}>
      <h4 className="mb-4">결제 완료</h4>
      <p className="text-muted-soft mb-4">결제가 정상 처리되었습니다.</p>
      <PaymentCompletionNext result={result} />
    </Container>
  );
}
