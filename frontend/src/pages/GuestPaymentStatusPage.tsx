import { useState } from "react";
import { skipToken, useQuery } from "@tanstack/react-query";
import { Alert, Button, Container } from "react-bootstrap";
import { Link, useParams } from "react-router";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  fetchPaymentStatus,
  PaymentCompletionNext,
  PaymentStatusNotice,
  readPaymentStatusToken,
  shouldPollPaymentStatus,
} from "@/features/payment";
import {
  captureCustomerSession,
  isCurrentCustomerSession,
  runForCustomerSession,
} from "@/shared/api";
import { ErrorAlert, LinkButton, LoadingSpinner } from "@/shared/ui";
import { NotFoundPage } from "@/pages/NotFoundPage";

const POLL_INTERVAL_MS = 3_000;

export function GuestPaymentStatusPage() {
  const { orderId: routeOrderId } = useParams<{ orderId: string }>();
  const { sessionVersion } = useCustomerAuth();
  const [customerSession] = useState(captureCustomerSession);
  const orderId = routeOrderId?.trim() ?? "";
  const validOrderId = orderId.length > 0 && orderId.length <= 100;
  const sessionChanged = sessionVersion !== customerSession.version
    || !isCurrentCustomerSession(customerSession);
  const statusToken = validOrderId && !sessionChanged
    ? readPaymentStatusToken(orderId, customerSession)
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
      <Container className="page-container" style={{ maxWidth: 640 }}>
        <Alert variant="warning">
          <Alert.Heading className="fs-5">회원 계정이 변경되었습니다</Alert.Heading>
          <p className="mb-0">
            이전 계정에서 조회하던 결제 상태는 이 화면에 표시하지 않습니다.
          </p>
        </Alert>
        <LinkButton to="/guest" variant="primary">비회원 조회로 이동</LinkButton>
      </Container>
    );
  }

  if (!statusToken) {
    return (
      <Container className="page-container" style={{ maxWidth: 640 }}>
        <Alert variant="warning">
          <Alert.Heading className="fs-5">결제 상태 조회 정보가 필요합니다</Alert.Heading>
          <p className="mb-0">결제 때 사용한 휴대폰 번호를 인증해 조회 정보를 복구해 주세요.</p>
        </Alert>
        <LinkButton to="/guest" variant="primary">휴대폰 인증하기</LinkButton>
      </Container>
    );
  }

  if (isLoading) {
    return (
      <Container className="page-container" style={{ maxWidth: 640 }}>
        <LoadingSpinner text="결제 상태를 확인하는 중..." />
      </Container>
    );
  }

  if (error && !status) {
    return (
      <Container className="page-container" style={{ maxWidth: 640 }}>
        <Link to="/guest" className="text-decoration-none small d-inline-block mb-3">
          &larr; 비회원 조회
        </Link>
        <ErrorAlert error={error} />
        <LinkButton to="/guest" variant="primary">조회 정보 다시 복구하기</LinkButton>
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
      }
    : null;

  return (
    <Container className="page-container" style={{ maxWidth: 640 }}>
      <Link to="/guest" className="text-decoration-none small d-inline-block mb-3">
        &larr; 복구한 결제 목록
      </Link>
      <div className="mb-4">
        <div className="my-section-kicker mb-2">결제 처리 현황</div>
        <h4 className="mb-2">결제 상태</h4>
        <p className="text-muted-soft small mb-0 text-break">결제번호 {orderId}</p>
      </div>

      <PaymentStatusNotice status={status} />

      {error && (
        <div className="mt-3">
          <ErrorAlert error={error} />
        </div>
      )}

      {status.status === "COMPLETED" && !completedResult && (
        <Alert variant="warning" className="mt-3 mb-0">
          결제는 완료됐지만 연결된 주문 또는 예약 정보를 확인하지 못했습니다. 해피갤러리로 문의해 주세요.
        </Alert>
      )}

      <div className="d-flex flex-wrap gap-2 mt-3">
        {completedResult && <PaymentCompletionNext result={completedResult} />}
        <Button
          variant={completedResult ? "outline-secondary" : "primary"}
          disabled={isFetching}
          onClick={() => void refetch()}
        >
          {isFetching ? "확인 중..." : "상태 새로고침"}
        </Button>
        <LinkButton to="/guest" variant="outline-secondary">목록으로</LinkButton>
      </div>
    </Container>
  );
}
