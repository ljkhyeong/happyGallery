import { useMutation, useQuery } from "@tanstack/react-query";
import { Container, Card, Button } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { executePaymentFlow, fetchPassPaymentPolicy, PaymentErrorAlert, PaymentMethodFields, useCheckoutSelection } from "@/features/payment";
import { formatKRW } from "@/shared/lib";
import { ErrorAlert, LinkButton, LoadingSpinner } from "@/shared/ui";

export function PassPurchasePage() {
  const { isAuthenticated, user } = useCustomerAuth();
  const [checkoutSelection, setCheckoutSelection] = useCheckoutSelection();
  const policyQuery = useQuery({
    queryKey: ["payment", "pass-policy"],
    queryFn: fetchPassPaymentPolicy,
  });

  const purchaseMutation = useMutation({
    mutationFn: async () => {
      if (!user) throw new Error("로그인이 필요합니다.");
      await executePaymentFlow({
        checkoutSelection,
        context: "PASS",
        payload: { type: "PASS", userId: user.id },
        orderName: "8회권",
        customerKey: `member_${user.id}`,
        customerName: user.name,
        customerPhone: user.phone || undefined,
        returnHint: { customerName: user.name, customerPhone: user.phone ?? undefined },
      });
    },
  });

  const loginHref = buildAuthPageHref("/login", { redirectTo: "/passes/purchase" });

  return (
    <Container className="page-container" style={{ maxWidth: 540 }}>
      <h4 className="mb-4">8회권 구매</h4>

      <Card className="mb-3">
        <Card.Body>
          <h6 className="mb-3">정규 공예 8회권</h6>
          {policyQuery.isLoading && <LoadingSpinner text="판매 정책 확인 중..." />}
          <ErrorAlert
            error={policyQuery.error}
            onRetry={() => { void policyQuery.refetch(); }}
            retrying={policyQuery.isFetching}
          />
          {policyQuery.data && (
            <>
              <dl className="row mb-3">
                <dt className="col-6 fw-normal text-muted">결제 금액</dt>
                <dd className="col-6 text-end fw-semibold">{formatKRW(policyQuery.data.totalPrice)}</dd>
                <dt className="col-6 fw-normal text-muted">이용 횟수</dt>
                <dd className="col-6 text-end">{policyQuery.data.totalCredits}회</dd>
                <dt className="col-6 fw-normal text-muted">이용 기간</dt>
                <dd className="col-6 text-end mb-0">결제일 포함 {policyQuery.data.validityDays}일</dd>
              </dl>
              <p className="text-muted-soft small mb-0">
                8회권 사용 가능으로 표시된 비향수 정규 공예 클래스에서만 사용할 수 있습니다.
                예약할 때 이용권을 선택하면 별도 예약금 없이 1회가 차감됩니다.
              </p>
            </>
          )}
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Body>
          <h6 className="mb-3">구매 전 확인</h6>
          <ul className="small text-muted-soft ps-3 mb-3">
            {policyQuery.data ? (
              <li className="mb-2">
                결제일을 포함해 {policyQuery.data.validityDays}일 동안 사용할 수 있으며,
                마지막 사용 가능일 다음 날 00:00부터 남은 횟수는 환불 없이 소멸합니다.
              </li>
            ) : (
              <li className="mb-2">
                이용 기간은 판매 정책을 확인한 뒤 표시합니다.
              </li>
            )}
            <li className="mb-2">
              예약 한 건마다 1회가 차감됩니다. 결석하거나 변경 가능 시각이 지난 뒤 이용하지
              않아도 1회는 소모되며 별도 보강은 제공되지 않습니다.
            </li>
            <li className="mb-2">
              취소 마감 전에는 차감한 1회가 복구되지만, 마감 후 취소하면 복구되지 않습니다.
            </li>
            <li>
              만료 전 환불은 남은 횟수와 자동 취소되는 미래 예약 횟수를 합산해 회당 구매
              단가로 계산합니다. 만료된 이용권은 환불할 수 없습니다.
            </li>
          </ul>
          <p className="text-muted-soft small mb-0">
            표시 금액과 이용 기간은 결제 전에 최신 판매 정책으로 다시 확인합니다.
          </p>
        </Card.Body>
      </Card>

      {isAuthenticated && (
        <PaymentMethodFields value={checkoutSelection} onChange={setCheckoutSelection} disabled={purchaseMutation.isPending} />
      )}
      <PaymentErrorAlert error={purchaseMutation.error} />

      {isAuthenticated ? (
        <Button
          variant="primary" size="lg" className="w-100"
          disabled={purchaseMutation.isPending || !policyQuery.data}
          onClick={() => purchaseMutation.mutate()}
        >
          {purchaseMutation.isPending ? "결제창 여는 중..." : "결제 진행하기"}
        </Button>
      ) : (
        <LinkButton
          to={loginHref}
          variant="primary" size="lg" className="w-100"
        >
          로그인 후 구매하기
        </LinkButton>
      )}
    </Container>
  );
}
