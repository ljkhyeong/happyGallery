import { Button, Card } from "react-bootstrap";
import { Link } from "react-router";
import { useRestockAlerts } from "@/features/product/useRestockAlerts";
import { productDetailHref } from "@/features/product/navigation";
import { captureCustomerSession } from "@/shared/api";
import { ErrorAlert, EmptyState, LoadingSpinner, ToneBadge, type CustomerBadgeTone } from "@/shared/ui";

interface AlertStatusBadge {
  label: string;
  tone: CustomerBadgeTone;
}

// 입고를 기다리는 신청은 먹색, 끝난 신청(알림 완료·해지)은 회색 면, 해지 결과를 다시 확인할 신청은 노랑이다.
const STATUS_BADGES: Record<string, AlertStatusBadge> = {
  WAITING: { label: "입고 대기", tone: "progress" },
  QUEUED: { label: "알림 접수", tone: "progress" },
  NOTIFIED: { label: "알림 완료", tone: "done" },
  CANCELED: { label: "해지", tone: "done" },
};
const STATUS_CHECK_BADGE: AlertStatusBadge = { label: "상태 확인 필요", tone: "action" };

export function MyRestockAlertsSection() {
  const { query, mutation } = useRestockAlerts();
  const busy = mutation.isPending || query.isFetching;
  const needsStatusCheck = mutation.isSuccess && query.isError;
  return (
    <section id="my-restock-alerts" className="mb-4">
      {query.isLoading && <LoadingSpinner />}
      {needsStatusCheck && <p role="status">해지 요청을 처리했습니다. 최신 신청 상태를 확인해 주세요.</p>}
      <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }}
        retrying={busy} retryLabel="신청 상태 다시 확인" />
      <ErrorAlert error={mutation.error}
        onRetry={() => { if (mutation.variables) mutation.mutate(mutation.variables); }}
        retrying={busy} retryLabel="알림 해지 다시 시도" />
      {query.data?.length === 0 && <EmptyState message="신청한 재입고 알림이 없습니다." />}
      {query.data?.map((alert) => {
        const badge = needsStatusCheck && mutation.variables?.action === "cancel" && mutation.variables.id === alert.id
          ? STATUS_CHECK_BADGE
          : STATUS_BADGES[alert.status] ?? STATUS_CHECK_BADGE;
        return (
          <Card key={alert.id} className="mb-2 my-list-card">
            <Card.Body className="d-flex justify-content-between align-items-center gap-2 py-3">
              <div>
                <Link to={productDetailHref(alert.productId, alert.productVariantId)}>{alert.productName}</Link>
                <div className="small text-muted">{alert.optionLabel}</div>
                <ToneBadge tone={badge.tone}>{badge.label}</ToneBadge>
              </div>
              {["WAITING", "QUEUED"].includes(alert.status) && <Button size="sm" variant="outline-secondary" disabled={busy || query.isError}
                onClick={() => mutation.mutate({ action: "cancel", id: alert.id, customerSession: captureCustomerSession() })}>알림 해지</Button>}
            </Card.Body>
          </Card>
        );
      })}
    </section>
  );
}
