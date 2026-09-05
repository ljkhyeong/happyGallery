import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Button } from "react-bootstrap";
import { listMyRestockAlerts, registerMyRestockAlert, cancelMyRestockAlert } from "@/generated/api/customerStore";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, LinkButton, useToast } from "@/shared/ui";

export const restockAlertsKey = ["me", "restock-alerts"] as const;

export function RestockAlertButton({ productId, productVariantId }: { productId: number; productVariantId: number | null }) {
  const { user, isAuthenticated, isLoading } = useCustomerAuth();
  const client = useQueryClient();
  const toast = useToast();
  const query = useQuery({
    queryKey: restockAlertsKey,
    queryFn: ({ signal }) => runForCurrentCustomer(() => listMyRestockAlerts({ signal })),
    enabled: isAuthenticated,
  });
  const registration = query.data?.find((alert) => alert.productId === productId
    && alert.productVariantId === productVariantId && ["WAITING", "QUEUED"].includes(alert.status));
  const mutation = useMutation({
    mutationFn: () => runForCurrentCustomer(
      () => registration ? cancelMyRestockAlert(registration.id) : registerMyRestockAlert({ productId, productVariantId }),
      async (_, requireCurrent) => {
        await client.invalidateQueries({ queryKey: restockAlertsKey });
        requireCurrent();
        toast.show(registration ? "재입고 알림 신청을 해지했습니다." : "재입고 알림을 신청했습니다.");
      },
    ),
  });
  if (isLoading) return null;
  if (!isAuthenticated) return <LinkButton to={buildAuthPageHref("/login", { redirectTo: `/products/${productId}` })} variant="outline-primary">로그인하고 재입고 알림 받기</LinkButton>;
  if (!user?.phoneVerified) return <LinkButton to="/my" variant="outline-primary">휴대폰 인증 후 재입고 알림 받기</LinkButton>;
  return (
    <div className="my-2">
      <Button variant="outline-primary" disabled={mutation.isPending || query.isLoading || query.isError} onClick={() => mutation.mutate()}>
        {mutation.isPending ? "처리 중..." : registration ? "재입고 알림 해지" : "재입고 알림 받기"}
      </Button>
      <p className="small text-muted mt-1 mb-0">선택한 상품·옵션이 입고되면 한 번 안내합니다. 알림은 재고 예약을 보장하지 않습니다.</p>
      <ErrorAlert error={query.error ?? mutation.error} onRetry={() => { void query.refetch(); }} />
    </div>
  );
}
