import { useCallback, useEffect, useRef, useState } from "react";
import { Alert, Container, Spinner, Button } from "react-bootstrap";
import { useNavigate, useSearchParams } from "react-router";
import {
  confirmPayment,
  consumePaymentReturnHint,
  fetchPaymentStatus,
  isTerminalPaymentStatus,
  PaymentCompletionNext,
  PaymentStatusNotice,
  readPaymentConfirmSession,
  readPaymentReturnHint,
  readPaymentStatusToken,
  removePaymentConfirmRequest,
  shouldPollPaymentStatus,
  storePaymentConfirmRequest,
  type ConfirmPaymentResponse,
  type PaymentConfirmRequest,
  type PaymentSessionHandle,
  type PaymentStatusResponse,
} from "@/features/payment";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { isPositiveSafeIntegerString } from "@/shared/lib";
import { ErrorAlert } from "@/shared/ui";
import {
  ApiError,
  captureCustomerSession,
  CustomerSessionChangedError,
  isCurrentCustomerSession,
  requireCurrentCustomerSession,
  runForCustomerSession,
} from "@/shared/api";

interface PaymentConfirmSession {
  request: PaymentConfirmRequest;
  handle: PaymentSessionHandle<PaymentConfirmRequest> | null;
}

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
  const { sessionVersion } = useCustomerAuth();
  const [customerSession] = useState(captureCustomerSession);
  const [error, setError] = useState<unknown>(null);
  const [result, setResult] = useState<ConfirmPaymentResponse | null>(null);
  const [paymentStatus, setPaymentStatus] = useState<PaymentStatusResponse | null>(null);
  const [statusError, setStatusError] = useState("");
  const [confirming, setConfirming] = useState(true);
  const [sessionChanged, setSessionChanged] = useState(false);
  const calledRef = useRef(false);
  const latestStatusRequestRef = useRef(0);
  const completedRef = useRef(false);

  const [returnHintSession] = useState(() =>
    readPaymentReturnHint(customerSession));
  const [confirmSession] = useState<PaymentConfirmSession | null>(() => {
    const callbackPresent = params.has("paymentKey")
      || params.has("orderId")
      || params.has("amount");
    if (!callbackPresent) {
      const stored = readPaymentConfirmSession(customerSession);
      return stored ? { request: stored.value, handle: stored } : null;
    }

    const paymentKey = params.get("paymentKey")?.trim() ?? "";
    const orderId = params.get("orderId")?.trim() ?? "";
    const amountValue = params.get("amount");
    if (!paymentKey || !orderId || !isPositiveSafeIntegerString(amountValue)) {
      const stored = readPaymentConfirmSession(customerSession);
      if (stored) removePaymentConfirmRequest(stored);
      return null;
    }

    const request = { paymentKey, orderId, amount: Number(amountValue) };
    return {
      request,
      handle: storePaymentConfirmRequest(request, customerSession),
    };
  });
  const confirmRequest = confirmSession?.request ?? null;
  const paymentKey = confirmRequest?.paymentKey ?? "";
  const orderId = confirmRequest?.orderId ?? "";
  const amount = confirmRequest?.amount ?? 0;

  const abandonChangedSession = useCallback(() => {
    latestStatusRequestRef.current += 1;
    completedRef.current = true;
    setResult(null);
    setPaymentStatus(null);
    setError(null);
    setStatusError("");
    setConfirming(false);
    setSessionChanged(true);
  }, []);

  const requireCurrentCustomer = useCallback(() => {
    requireCurrentCustomerSession(customerSession);
  }, [customerSession]);

  const checkStatus = useCallback(async () => {
    requireCurrentCustomer();
    if (!orderId) throw new Error("결제 주문번호가 없습니다.");
    const requestId = ++latestStatusRequestRef.current;
    const status = await runForCustomerSession(
      customerSession,
      () => fetchPaymentStatus(
        orderId,
        readPaymentStatusToken(orderId, customerSession),
      ),
    );
    if (requestId !== latestStatusRequestRef.current || completedRef.current) {
      return status;
    }
    requireCurrentCustomer();
    setStatusError("");
    setPaymentStatus(status);
    if (isTerminalPaymentStatus(status.status) && confirmSession?.handle) {
      removePaymentConfirmRequest(confirmSession.handle);
    }
    if (status.status === "COMPLETED" && status.domainId != null) {
      requireCurrentCustomer();
      completedRef.current = true;
      setResult({
        context: status.context,
        domainId: status.domainId,
        accessToken: status.accessToken,
        accessRecoveryRequired: status.accessRecoveryRequired,
      });
      setError(null);
      if (returnHintSession) consumePaymentReturnHint(returnHintSession);
    }
    return status;
  }, [
    confirmSession,
    customerSession,
    orderId,
    requireCurrentCustomer,
    returnHintSession,
  ]);

  const runConfirm = useCallback(async () => {
    try {
      requireCurrentCustomer();
    } catch (requestError) {
      if (requestError instanceof CustomerSessionChangedError) {
        abandonChangedSession();
        return;
      }
      throw requestError;
    }
    if (!confirmRequest) {
      setError(new Error("결제 정보가 올바르지 않습니다."));
      setConfirming(false);
      return;
    }

    setError(null);
    setStatusError("");
    setPaymentStatus(null);
    setConfirming(true);
    try {
      const response = await runForCustomerSession(
        customerSession,
        () => confirmPayment(
          { paymentKey, orderId, amount },
          readPaymentStatusToken(orderId, customerSession),
        ),
      );
      requireCurrentCustomer();
      completedRef.current = true;
      setResult(response);
      if (returnHintSession) consumePaymentReturnHint(returnHintSession);
      if (confirmSession?.handle) {
        removePaymentConfirmRequest(confirmSession.handle);
      }
    } catch (requestError) {
      if (requestError instanceof CustomerSessionChangedError) {
        abandonChangedSession();
        return;
      }
      requireCurrentCustomer();
      setError(requestError);
      try {
        await checkStatus();
      } catch (statusRequestError) {
        if (statusRequestError instanceof CustomerSessionChangedError) {
          abandonChangedSession();
          return;
        }
        // 과거 결제나 유실된 비회원 토큰은 기존 confirm 오류 안내를 유지한다.
      }
    } finally {
      if (isCurrentCustomerSession(customerSession)) {
        setConfirming(false);
      } else {
        abandonChangedSession();
      }
    }
  }, [
    abandonChangedSession,
    amount,
    checkStatus,
    confirmRequest,
    confirmSession,
    customerSession,
    orderId,
    paymentKey,
    requireCurrentCustomer,
    returnHintSession,
  ]);

  useEffect(() => {
    if (
      sessionVersion !== customerSession.version
      || !isCurrentCustomerSession(customerSession)
    ) {
      abandonChangedSession();
    }
  }, [abandonChangedSession, customerSession, sessionVersion]);

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
        if (
          active
          && isCurrentCustomerSession(customerSession)
        ) {
          setStatusError("상태를 새로 확인하지 못했습니다. 잠시 후 자동으로 다시 확인합니다.");
          timer = window.setTimeout(() => void poll(), 3_000);
        } else if (active) {
          abandonChangedSession();
        }
      }
    };

    timer = window.setTimeout(() => void poll(), 3_000);
    return () => {
      active = false;
      if (timer !== undefined) window.clearTimeout(timer);
    };
  }, [
    abandonChangedSession,
    checkStatus,
    customerSession,
    paymentStatus?.status,
  ]);

  if (sessionChanged) {
    return (
      <Container className="page-container" style={{ maxWidth: 540 }}>
        <Alert variant="warning">
          <Alert.Heading className="fs-5">회원 계정이 변경되었습니다</Alert.Heading>
          <p className="mb-0">
            이전 계정에서 시작한 결제 결과는 이 화면에 표시하지 않습니다.
          </p>
        </Alert>
        <Button variant="primary" onClick={() => navigate("/")}>홈으로</Button>
      </Container>
    );
  }

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
            <Button variant="primary" onClick={() => void checkStatus().catch((requestError) => {
              if (requestError instanceof CustomerSessionChangedError) {
                abandonChangedSession();
                return;
              }
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
