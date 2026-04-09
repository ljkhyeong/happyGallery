import { useQuery } from "@tanstack/react-query";
import { Table } from "react-bootstrap";
import { LoadingSpinner } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import { fetchOrderHistory } from "./api";

const DECISION_LABELS: Record<string, string> = {
  APPROVE: "승인",
  REJECT: "거절",
  DELAY: "지연 요청",
  AUTO_REFUND: "자동 환불",
  PRODUCTION_COMPLETE: "제작 완료",
  RESUME_PRODUCTION: "제작 재개",
  PREPARE_SHIPPING: "배송 준비",
  SHIP: "배송 출발",
  DELIVER: "배송 완료",
};

function decisionLabel(decision: string): string {
  return DECISION_LABELS[decision] ?? decision;
}

interface Props {
  orderId: number;
  adminKey: string;
}

export function OrderHistoryPanel({ orderId, adminKey }: Props) {
  const { data, isLoading } = useQuery({
    queryKey: ["admin", "orders", orderId, "history"],
    queryFn: () => fetchOrderHistory(adminKey, orderId),
  });

  return (
    <div className="mt-3 p-3 border rounded">
      <h6>주문 #{orderId} 이력</h6>
      {isLoading && <LoadingSpinner />}
      {data && data.length === 0 && <small className="text-muted">이력이 없습니다.</small>}
      {data && data.length > 0 && (
        <Table size="sm" bordered>
          <thead>
            <tr>
              <th>결정</th>
              <th>관리자 ID</th>
              <th>사유</th>
              <th>일시</th>
            </tr>
          </thead>
          <tbody>
            {data.map((h) => (
              <tr key={h.id}>
                <td><small>{decisionLabel(h.decision)}</small></td>
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
