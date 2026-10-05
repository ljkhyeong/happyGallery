import { useState } from "react";
import { skipToken, useQuery } from "@tanstack/react-query";
import { Alert, Button, Container } from "react-bootstrap";
import { Link, useLocation, useParams } from "react-router";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  fetchPaymentStatus,
  PaymentCompletionNext,
  PaymentResultCard,
  PaymentStatusNotice,
  readPaymentStatusToken,
  shouldPollPaymentStatus,
} from "@/features/payment";
import {
  captureCustomerSession,
  isCurrentCustomerSession,
  isCurrentCustomerSessionState,
  runForCustomerSession,
  type CustomerSessionOwnedState,
} from "@/shared/api";
import { ErrorAlert, LinkButton, LoadingSpinner } from "@/shared/ui";
import { NotFoundPage } from "@/pages/NotFoundPage";

const POLL_INTERVAL_MS = 3_000;

interface LocationState extends CustomerSessionOwnedState {
  orderId?: string;
  statusToken?: string;
}

function readNavigationStatusToken(state: unknown, orderId: string): string | null {
  if (!isCurrentCustomerSessionState(state)) return null;
  const navigationState = state as LocationState;
  const statusToken = navigationState.statusToken?.trim() ?? "";
  return navigationState.orderId === orderId
    && statusToken.length > 0
    && statusToken.length <= 500
    ? statusToken
    : null;
}

export function GuestPaymentStatusPage() {
  const { orderId: routeOrderId } = useParams<{ orderId: string }>();
  const location = useLocation();
  const { sessionVersion } = useCustomerAuth();
  const [customerSession] = useState(captureCustomerSession);
  const orderId = routeOrderId?.trim() ?? "";
  const validOrderId = orderId.length > 0 && orderId.length <= 100;
  const sessionChanged = sessionVersion !== customerSession.version
    || !isCurrentCustomerSession(customerSession);
  const statusToken = validOrderId && !sessionChanged
    ? readNavigationStatusToken(location.state, orderId)
      ?? readPaymentStatusToken(orderId, customerSession)
    : null;
  const {
    data: status,
    error,
    isLoading,
    isFetching,
    refetch,
  } = useQuery({
    queryKey: [
      "guest",
      "payment-status",
      customerSession.version,
      customerSession.boundaryEpoch,
      customerSession.boundaryCustomerId,
      orderId,
      statusToken,
    ],
    queryFn: statusToken && !sessionChanged
      ? () => runForCustomerSession(
          customerSession,
          () => fetchPaymentStatus(orderId, statusToken),
        )
      : skipToken,
    gcTime: 0,
    refetchInterval: ({ state }) =>
      shouldPollPaymentStatus(state.data?.status) ? POLL_INTERVAL_MS : false,
  });

  if (!validOrderId) return <NotFoundPage />;

  if (sessionChanged) {
    return (
      <Container className="page-container payment-result-page">
        <PaymentResultCard
          tone="notice"
          title="회원 계정이 변경되었습니다"
          lead="이전 계정에서 조회하던 결제 상태는 이 화면에 표시하지 않습니다."
          actions={<LinkButton to="/guest" variant="primary">비회원 조회로 이동</LinkButton>}
        />
      </Container>
    );
  }

  if (!statusToken) {
    return (
      <Container className="page-container payment-result-page">
        <PaymentResultCard
          tone="notice"
          title="휴대폰 인증 후 결제 결과를 확인해 주세요"
          lead="결제 때 사용한 휴대폰 번호를 인증하면 결제 상태를 볼 수 있습니다."
          actions={<LinkButton to="/guest" variant="primary">휴대폰 인증하기</LinkButton>}
        />
      </Container>
    );
  }

  if (isLoading) {
    return (
      <Container className="page-container payment-result-page">
        <LoadingSpinner text="결제 상태를 확인하는 중..." />
      </Container>
    );
  }

  if (error && !status) {
    return (
      <Container className="page-container payment-result-page">
        <PaymentResultCard
          tone="danger"
          title="결제 상태를 불러오지 못했습니다"
          actions={<LinkButton to="/guest" variant="primary">휴대폰 인증으로 다시 조회</LinkButton>}
        >
          <ErrorAlert error={error} />
        </PaymentResultCard>
      </Container>
    );
  }

  if (!status) return null;

  const completedResult = status.status === "COMPLETED" && status.domainId != null
    ? {
        context: status.context,
        domainId: status.domainId,
        accessToken: status.accessToken,
        accessRecoveryRequired: status.accessRecoveryRequired,
        receiptUrl: status.receiptUrl,
      }
    : null;

  return (
    <Container className="page-container payment-result-page">
      <Link to="/guest" className="page-back-link">&larr; 조회한 결제 목록</Link>
      <PaymentResultCard
        tone={completedResult ? "success" : "notice"}
        title="결제 상태"
        lead={<span className="text-break">결제번호 {orderId}</span>}
        actions={(
          <>
            {completedResult && <PaymentCompletionNext result={completedResult} />}
            {status.status === "COMPLETED" && status.receiptUrl && (
              <a
                className="btn btn-outline-secondary"
                href={status.receiptUrl}
                target="_blank"
                rel="noreferrer"
              >
                결제 영수증 보기
              </a>
            )}
            <Button
              variant={completedResult ? "outline-secondary" : "primary"}
              disabled={isFetching}
              onClick={() => void refetch()}
            >
              {isFetching ? "확인 중..." : "상태 새로고침"}
            </Button>
            <LinkButton to="/guest" variant="outline-secondary">목록으로</LinkButton>
          </>
        )}
      >
        <PaymentStatusNotice status={status} />
        {error && <ErrorAlert error={error} />}
        {status.status === "COMPLETED" && !completedResult && (
          <Alert variant="warning" className="mb-0">
            결제는 완료됐지만 연결된 주문·예약 또는 이용권 정보를 확인하지 못했습니다. 해피갤러리로 문의해 주세요.
          </Alert>
        )}
      </PaymentResultCard>
    </Container>
  );
}
