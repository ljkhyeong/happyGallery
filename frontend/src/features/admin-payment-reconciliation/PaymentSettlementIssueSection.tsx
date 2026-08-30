import { Table } from "react-bootstrap";
import { fetchPaymentSettlementIssues } from "./api";
import { ApiError } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import type { PaymentSettlementIssueResponse } from "@/shared/types";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

const queryKey = ["admin", "payment-settlements", "issues"];

export function PaymentSettlementIssueSection({ adminKey, onAuthError }: Props) {
  const { data, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey,
    queryFn: () => fetchPaymentSettlementIssues(adminKey),
    refetchInterval: 60_000,
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    return <ErrorAlert error={error} />;
  }
  if (!data?.length) return <EmptyState message="PG 정산과 다른 결제·환불 내역이 없습니다." />;

  return (
    <Table responsive hover size="sm">
      <thead>
        <tr>
          <th>거래키</th>
          <th>구분</th>
          <th className="text-end">거래금액</th>
          <th className="text-end">지급예정액</th>
          <th>확인 필요 사유</th>
          <th>조회일</th>
        </tr>
      </thead>
      <tbody>
        {data.map((settlement) => (
          <tr key={settlement.transactionKey}>
            <td className="small text-break">{settlement.transactionKey}</td>
            <td>{settlement.cancelTransaction ? "취소" : "승인"}</td>
            <td className="text-end">{formatKRW(settlement.amount)}</td>
            <td className="text-end">{formatKRW(settlement.payOutAmount)}</td>
            <td className="small">
              {statusLabel(settlement.status)}
              {settlement.reason && <div className="text-muted-soft mt-1">{settlement.reason}</div>}
            </td>
            <td className="small">{formatDateTime(settlement.fetchedAt)}</td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}

function statusLabel(status: PaymentSettlementIssueResponse["status"]): string {
  if (status === "LOCAL_PAYMENT_NOT_FOUND") return "로컬 결제 승인 없음";
  if (status === "LOCAL_REFUND_NOT_FOUND") return "로컬 환불 완료 없음";
  if (status === "IDENTIFIER_MISMATCH") return "결제번호 불일치";
  if (status === "AMOUNT_MISMATCH") return "금액 불일치";
  return "확인 필요";
}
