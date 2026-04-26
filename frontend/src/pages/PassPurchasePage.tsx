import { useMutation } from "@tanstack/react-query";
import { Container, Card, Button } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { preparePayment, requestTossPayment, storePaymentReturnHint } from "@/features/payment";
import { ErrorAlert } from "@/shared/ui";

export function PassPurchasePage() {
  const { isAuthenticated, user } = useCustomerAuth();

  const purchaseMutation = useMutation({
    mutationFn: async () => {
      if (!user) throw new Error("로그인이 필요합니다.");
      const prep = await preparePayment("PASS", { type: "PASS", userId: user.id });
      storePaymentReturnHint({ customerName: user.name, customerPhone: user.phone });
      await requestTossPayment({
        orderId: prep.orderId,
        amount: prep.amount,
        orderName: "8회권",
        customerKey: `member_${user.id}`,
        customerName: user.name,
        customerMobilePhone: user.phone || undefined,
      });
    },
  });

  const loginHref = buildAuthPageHref("/login", { redirectTo: "/passes/purchase" });

  return (
    <Container className="page-container" style={{ maxWidth: 540 }}>
      <h4 className="mb-4">8회권 구매</h4>

      <Card className="mb-3">
        <Card.Body>
          <p className="text-muted-soft small mb-0">
            8회권을 구매하면 90일간 8회 수업을 이용할 수 있습니다.
            예약 시 8회권을 선택하면 예약금 없이 횟수가 차감됩니다.
          </p>
        </Card.Body>
      </Card>

      <Card className="mb-4">
        <Card.Body>
          <h6 className="mb-2">결제 정보</h6>
          <p className="text-muted-soft small mb-0">
            결제 금액은 서버에서 확정되며, 다음 단계에서 토스 결제창으로 이동합니다.
            환불 시 잔여 횟수 기준으로 정산됩니다.
          </p>
        </Card.Body>
      </Card>

      <ErrorAlert error={purchaseMutation.error} />

      {isAuthenticated ? (
        <Button
          variant="primary" size="lg" className="w-100"
          disabled={purchaseMutation.isPending}
          onClick={() => purchaseMutation.mutate()}
        >
          {purchaseMutation.isPending ? "결제창 여는 중..." : "결제 진행하기"}
        </Button>
      ) : (
        <Button
          as={"a" as any} href={loginHref}
          variant="primary" size="lg" className="w-100"
        >
          로그인 후 구매하기
        </Button>
      )}
    </Container>
  );
}
