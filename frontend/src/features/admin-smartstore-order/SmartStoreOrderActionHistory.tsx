import { Badge } from "react-bootstrap";
import type { SmartStoreOrderActionHistoryResponse } from "@/generated/api/adminOrder";
import { formatDateTime } from "@/shared/lib";
import { ACTION_LABELS, ACTION_STATUS_LABELS } from "./labels";

export function SmartStoreOrderActionHistory({
  history,
  loading,
}: {
  history: SmartStoreOrderActionHistoryResponse[];
  loading: boolean;
}) {
  return (
    <section className="border rounded p-3 mb-3" aria-label="주문 처리 이력">
      <div className="fw-semibold mb-2">최근 처리 이력</div>
      {loading ? (
        <div className="small text-muted-soft">처리 이력을 불러오는 중입니다.</div>
      ) : !history.length ? (
        <div className="small text-muted-soft">저장된 처리 이력이 없습니다.</div>
      ) : (
        <div className="d-grid gap-2">
          {history.map((item) => (
            <div key={item.id} className="small border-bottom pb-2">
              <div className="d-flex flex-wrap justify-content-between gap-2">
                <span className="fw-semibold">{ACTION_LABELS[item.action] ?? item.action}</span>
                <Badge bg={actionStatusColor(item.status)}>
                  {ACTION_STATUS_LABELS[item.status] ?? item.status}
                </Badge>
              </div>
              <div className="text-muted-soft">
                {item.changedBy} · {formatDateTime(item.requestedAt)}
              </div>
              {item.requestSummary && <div>{item.requestSummary}</div>}
              {item.resultMessage && (
                <div className={item.status === "SUCCEEDED" ? "" : "text-danger"}>
                  {item.resultMessage}{item.resultCode ? ` (${item.resultCode})` : ""}
                </div>
              )}
              {item.reconciliationOutcome && (
                <div className="mt-1 text-primary-emphasis">
                  반영 여부 확인 결과: {item.reconciliationOutcome === "APPLIED"
                    ? "네이버에 반영됨"
                    : "네이버에 반영되지 않음"}
                  {item.reconciledBy && ` · ${item.reconciledBy}`}
                  {item.reconciledAt && ` · ${formatDateTime(item.reconciledAt)}`}
                  {item.reconciliationNote && <div>{item.reconciliationNote}</div>}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}

function actionStatusColor(status: SmartStoreOrderActionHistoryResponse["status"]): string {
  if (status === "SUCCEEDED") return "success";
  if (status === "REJECTED") return "danger";
  if (status === "NOT_SENT") return "secondary";
  if (status === "RESULT_UNKNOWN") return "warning";
  return "secondary";
}
