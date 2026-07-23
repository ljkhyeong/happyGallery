import { useMutation, useQuery } from "@tanstack/react-query";
import { Container, Card, Button } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { executePaymentFlow, fetchPassPaymentPolicy } from "@/features/payment";
import { formatKRW } from "@/shared/lib";
import { ErrorAlert, LinkButton, LoadingSpinner } from "@/shared/ui";

export function PassPurchasePage() {
  const { isAuthenticated, user } = useCustomerAuth();
  const policyQuery = useQuery({
    queryKey: ["payment", "pass-policy"],
    queryFn: fetchPassPaymentPolicy,
  });

  const purchaseMutation = useMutation({
    mutationFn: async () => {
      if (!user) throw new Error("로그인이 필요합니다.");
      await executePaymentFlow({
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
          <ErrorAlert error={policyQuery.error} />
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
                8회권 사용이 허용된 정규 클래스에서만 사용할 수 있습니다. 예약할 때
                8회권을 선택하면 별도 예약금 없이 1회가 차감됩니다.
              </p>
            </>
          )}
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Body>
          <h6 className="mb-2">결제 정보</h6>
          <p className="text-muted-soft small mb-0">
            표시 금액은 서버 판매 정책이며 prepare 단계에서 다시 확정됩니다.
            환불 금액은 사용한 횟수를 제외한 잔여 횟수를 기준으로 계산합니다.
          </p>
        </Card.Body>
      </Card>

      <ErrorAlert error={purchaseMutation.error} />

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
