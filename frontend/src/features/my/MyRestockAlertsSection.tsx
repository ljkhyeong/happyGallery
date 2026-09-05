import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Card } from "react-bootstrap";
import { Link } from "react-router";
import { listMyRestockAlerts, cancelMyRestockAlert } from "@/generated/api/customerStore";
import { restockAlertsKey } from "@/features/product/RestockAlertButton";
import { runForCurrentCustomer } from "@/shared/api";
import { ErrorAlert, EmptyState, LoadingSpinner } from "@/shared/ui";

const STATUS_LABELS = { WAITING: "입고 대기", QUEUED: "알림 접수", NOTIFIED: "알림 완료", CANCELED: "해지" };

export function MyRestockAlertsSection() {
  const client = useQueryClient();
  const query = useQuery({
    queryKey: restockAlertsKey,
    queryFn: ({ signal }) => runForCurrentCustomer(() => listMyRestockAlerts({ signal })),
  });
  const cancel = useMutation({
    mutationFn: (id: number) => runForCurrentCustomer(() => cancelMyRestockAlert(id),
      () => client.invalidateQueries({ queryKey: restockAlertsKey })),
  });
  return (
    <section id="my-restock-alerts" className="mb-4">
      <h6>내 재입고 알림</h6>
      {query.isLoading && <LoadingSpinner />}
      <ErrorAlert error={query.error ?? cancel.error} onRetry={() => { void query.refetch(); }} />
      {query.data?.length === 0 && <EmptyState message="신청한 재입고 알림이 없습니다." />}
      {query.data?.map((alert) => (
        <Card key={alert.id} className="mb-2 border-0 my-list-card">
          <Card.Body className="d-flex justify-content-between align-items-center gap-2 py-3">
            <div>
              <Link to={`/products/${alert.productId}`}>{alert.productName}</Link>
              <div className="small text-muted">{alert.optionLabel}</div>
              <Badge bg="secondary">{STATUS_LABELS[alert.status]}</Badge>
            </div>
            {["WAITING", "QUEUED"].includes(alert.status) && <Button size="sm" variant="outline-secondary" disabled={cancel.isPending} onClick={() => cancel.mutate(alert.id)}>알림 해지</Button>}
          </Card.Body>
        </Card>
      ))}
    </section>
  );
}
