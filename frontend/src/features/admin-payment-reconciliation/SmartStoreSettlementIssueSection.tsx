import { Table } from "react-bootstrap";
import type { SmartStoreSettlementIssueResponse } from "@/generated/api/adminOperations";
import { fetchSmartStoreSettlementIssues } from "./api";
import { ApiError } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function SmartStoreSettlementIssueSection({ adminKey, onAuthError }: Props) {
  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-settlements", "issues"],
    queryFn: () => fetchSmartStoreSettlementIssues(adminKey),
    refetchInterval: 60_000,
  });

  if (query.isLoading) return <LoadingSpinner />;
  if (query.error) {
    if (query.error instanceof ApiError && query.error.status === 401) return null;
    return <ErrorAlert error={query.error} />;
  }
  if (!query.data?.length) {
    return <EmptyState message="스마트스토어 주문과 다른 정산 내역이 없습니다." />;
  }

  return <Table responsive hover size="sm" className="align-middle">
    <thead>
      <tr>
        <th>상품 주문 번호</th>
        <th>상품</th>
        <th className="text-end">결제 정산액</th>
        <th className="text-end">정산 예정액</th>
        <th>확인 필요 사유</th>
        <th>조회일</th>
      </tr>
    </thead>
    <tbody>
      {query.data.map((entry) => <tr key={entry.entryKey}>
        <td className="small">{entry.productOrderId ?? "-"}</td>
        <td>{entry.productName ?? "-"}</td>
        <td className="text-end">{formatKRW(entry.paySettleAmount)}</td>
        <td className="text-end">{formatKRW(entry.settleExpectAmount)}</td>
        <td className="small">
          {settlementStatusLabel(entry.status)}
          {entry.reason && <div className="text-muted-soft mt-1">{entry.reason}</div>}
        </td>
        <td className="small">{formatDateTime(entry.fetchedAt)}</td>
      </tr>)}
    </tbody>
  </Table>;
}

function settlementStatusLabel(status: SmartStoreSettlementIssueResponse["status"]): string {
  if (status === "ORDER_NOT_FOUND") return "채널 주문 원장 없음";
  if (status === "EXPECTED_AMOUNT_MISSING") return "주문 정산 예정액 없음";
  if (status === "AMOUNT_MISMATCH") return "정산 예정액 불일치";
  return "확인 필요";
}
