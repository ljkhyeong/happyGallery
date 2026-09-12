import { Button } from "react-bootstrap";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { buildAuthPageHref } from "@/features/customer-auth/navigation";
import { captureCustomerSession } from "@/shared/api";
import { ErrorAlert, LinkButton } from "@/shared/ui";
import { productDetailHref } from "./navigation";
import { useRestockAlerts } from "./useRestockAlerts";

interface Props { productId: number; productVariantId: number | null }

export function RestockAlertButton(props: Props) {
  const { sessionVersion } = useCustomerAuth();
  return <RestockAlertButtonContent key={`${sessionVersion}:${props.productId}:${props.productVariantId}`} {...props} />;
}

function RestockAlertButtonContent({ productId, productVariantId }: Props) {
  const { user, isAuthenticated, isLoading } = useCustomerAuth();
  const { query, mutation } = useRestockAlerts();
  const registration = query.data?.find((alert) => alert.productId === productId
    && alert.productVariantId === productVariantId && ["WAITING", "QUEUED"].includes(alert.status));
  const busy = mutation.isPending || query.isFetching;
  const needsStatusCheck = mutation.isSuccess && query.isError;
  if (isLoading) return null;
  if (!isAuthenticated) return <LinkButton to={buildAuthPageHref("/login", { redirectTo: productDetailHref(productId, productVariantId) })} variant="outline-primary">로그인하고 재입고 알림 받기</LinkButton>;
  if (!user?.phoneVerified) return <LinkButton to="/my" variant="outline-primary">휴대폰 인증 후 재입고 알림 받기</LinkButton>;
  return (
    <div className="my-2">
      <Button variant="outline-primary" disabled={busy || query.isPending || query.isError} onClick={() => {
        const customerSession = captureCustomerSession();
        mutation.mutate(registration
          ? { action: "cancel", id: registration.id, customerSession }
          : { action: "register", request: { productId, productVariantId }, customerSession });
      }}>
        {mutation.isPending ? "처리 중..." : needsStatusCheck ? "신청 상태 확인 필요" : registration ? "재입고 알림 해지" : "재입고 알림 받기"}
      </Button>
      <p className="small text-muted mt-1 mb-0">선택한 상품·옵션이 입고되면 한 번 안내합니다. 알림은 재고 예약을 보장하지 않습니다.</p>
      {needsStatusCheck && <p className="small mt-2 mb-2" role="status">요청을 처리했습니다. 최신 신청 상태를 확인해 주세요.</p>}
      <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }}
        retrying={busy} retryLabel="신청 상태 다시 확인" />
      <ErrorAlert error={mutation.error}
        onRetry={() => { if (mutation.variables) mutation.mutate(mutation.variables); }}
        retrying={busy} retryLabel={mutation.variables?.action === "cancel" ? "알림 해지 다시 시도" : "알림 신청 다시 시도"} />
    </div>
  );
}
