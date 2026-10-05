import { useMutation, useQuery } from "@tanstack/react-query";
import { Container, Button } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { executePaymentFlow, fetchPassPaymentPolicy, PaymentErrorAlert, PaymentMethodFields, useCheckoutSelection } from "@/features/payment";
import { formatKRW } from "@/shared/lib";
import {
  CheckoutAmounts,
  CheckoutLayout,
  CheckoutPanel,
  CheckoutSummary,
  ErrorAlert,
  LinkButton,
  LoadingSpinner,
  PageHeader,
} from "@/shared/ui";

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
        orderName: "4회권",
        customerKey: `member_${user.id}`,
        customerName: user.name,
        customerPhone: user.phone || undefined,
        returnHint: {
          customerName: user.name, customerPhone: user.phone ?? undefined,
          returnPath: "/passes/purchase",
        },
      });
    },
  });

  const loginHref = buildAuthPageHref("/login", { redirectTo: "/passes/purchase" });

  const policy = policyQuery.data;
  const submitButton = isAuthenticated ? (
    <Button
      variant="primary" size="lg" className="w-100"
      disabled={purchaseMutation.isPending || !policy}
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
  );

  return (
    <Container className="page-container checkout-page">
      <PageHeader
        kicker="Pass"
        title="4회권 구매"
        description="원데이 다음 단계로, 정규 공예 수업을 원하는 날짜에 네 번 예약할 수 있습니다."
      />

      <CheckoutLayout
        summary={(
          <CheckoutSummary
            label="결제 요약"
            action={submitButton}
            mobileTotal={policy ? { label: "결제 금액", amount: formatKRW(policy.totalPrice) } : undefined}
            note="결제 전 금액과 이용 기간을 확인해 주세요."
          >
            <p className="checkout-summary-kicker">정규 공예 클래스</p>
            <h2 className="checkout-summary-title">정규 공예 4회권</h2>
            {policy && (
              <CheckoutAmounts
                rows={[
                  { label: "이용 횟수", value: `${policy.totalCredits}회` },
                  { label: "이용 기간", value: `결제일 포함 ${policy.validityDays}일` },
                ]}
                total={{ label: "결제 금액", value: formatKRW(policy.totalPrice) }}
              />
            )}
            <PaymentErrorAlert error={purchaseMutation.error} />
          </CheckoutSummary>
        )}
      >
        <CheckoutPanel title="이용권 안내">
          {policyQuery.isLoading && <LoadingSpinner text="가격·이용 기간 확인 중..." />}
          <ErrorAlert
            error={policyQuery.error}
            onRetry={() => { void policyQuery.refetch(); }}
            retrying={policyQuery.isFetching}
          />
          <p className="text-muted-soft mb-3">
            향수를 제외한 정규 공예 클래스 중 ‘이용권 사용 가능’으로 표시된 수업에서 사용할 수 있습니다.
            예약할 때 이용권을 선택하면 별도 예약금 없이 1회가 차감됩니다.
          </p>
          <LinkButton to="/classes?passEligible=true" variant="outline-primary">
            이용권 사용 가능 수업 보기
          </LinkButton>
        </CheckoutPanel>

        <CheckoutPanel title="구매 전 확인">
          <ul className="checkout-notice-list">
            {policy ? (
              <li>
                결제일 포함 {policy.validityDays}일간 사용할 수 있습니다.
                마지막 사용 가능일 다음 날 00:00에 남은 횟수는 환불 없이 소멸합니다.
              </li>
            ) : (
              <li>이용 기간을 확인하고 있습니다.</li>
            )}
            <li>예약마다 1회가 차감됩니다. 결석해도 차감한 횟수는 복구되지 않으며, 보강 수업은 없습니다.</li>
            <li>취소 마감 전 취소하면 1회가 복구됩니다. 마감 후에는 복구되지 않습니다.</li>
            <li>
              만료 전 환불 시, 남은 횟수와 자동 취소되는 예약 횟수를 합쳐 회당 구매 금액으로 환불합니다.
              만료된 이용권은 환불할 수 없습니다.
            </li>
          </ul>
        </CheckoutPanel>

        {isAuthenticated && (
          <CheckoutPanel title="결제 수단">
            <PaymentMethodFields
              value={checkoutSelection}
              onChange={setCheckoutSelection}
              disabled={purchaseMutation.isPending}
              showLegend={false}
            />
          </CheckoutPanel>
        )}
      </CheckoutLayout>
    </Container>
  );
}
