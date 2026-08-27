import { Table } from "react-bootstrap";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime } from "@/shared/lib";
import { fetchOrderHistory } from "./api";

const DECISION_LABELS: Record<string, string> = {
  APPROVE: "승인",
  REJECT: "거절",
  DELAY: "지연 요청",
  AUTO_REFUND: "자동 환불",
  SHIP_DATE_UPDATED: "예상 출고일 변경",
  PRODUCTION_COMPLETE: "제작 완료",
  RESUME_PRODUCTION: "제작 지연 동의 후 처리 계속",
  PICKUP_READY: "매장 수령 준비",
  PICKUP_COMPLETE: "매장 수령 완료",
  PICKUP_EXPIRED: "미수령 환불 요청",
  PICKUP_FORFEITED: "미수령 종료(환불 없음)",
  PREPARE_SHIPPING: "배송 준비",
  SHIP: "배송 출발",
  DELIVER: "배송 완료",
};

interface Props {
  orderId: number;
  adminKey: string;
  onAuthError: () => void;
}

export function OrderHistoryPanel({ orderId, adminKey, onAuthError }: Props) {
  const { data, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: ["admin", "orders", orderId, "history"],
    queryFn: () => fetchOrderHistory(adminKey, orderId),
  });

  return (
    <div className="mt-3 p-3 border rounded">
      <h6>주문 #{orderId} 이력</h6>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {data && data.length === 0 && <small className="text-muted">이력이 없습니다.</small>}
      {data && data.length > 0 && (
        <Table size="sm" bordered>
          <thead>
            <tr>
              <th>처리</th>
              <th>관리자 번호</th>
              <th>사유</th>
              <th>일시</th>
            </tr>
          </thead>
          <tbody>
            {data.map((h) => (
              <tr key={h.id}>
                <td><small>{DECISION_LABELS[h.decision] ?? "처리 내용 확인 필요"}</small></td>
                <td><small>{h.decidedByAdminId ?? "-"}</small></td>
                <td><small>{h.reason ?? "-"}</small></td>
                <td><small>{formatDateTime(h.decidedAt)}</small></td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}
    </div>
  );
}
