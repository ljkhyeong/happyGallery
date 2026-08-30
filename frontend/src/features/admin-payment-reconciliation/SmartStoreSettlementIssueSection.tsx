import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Alert, Button, Form, Table } from "react-bootstrap";
import type { SmartStoreSettlementIssueResponse } from "@/generated/api/adminOperations";
import { fetchSmartStoreSettlementIssues, synchronizeSmartStoreSettlementRange } from "./api";
import { ApiError } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function SmartStoreSettlementIssueSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const [from, setFrom] = useState(shiftDate(-6));
  const [to, setTo] = useState(shiftDate(0));
  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-settlements", "issues"],
    queryFn: () => fetchSmartStoreSettlementIssues(adminKey),
    refetchInterval: 60_000,
  });
  const synchronize = useAdminMutation(onAuthError, {
    mutationFn: () => synchronizeSmartStoreSettlementRange(adminKey, from, to),
    onSuccess: async () => {
      await queryClient.invalidateQueries({
        queryKey: ["admin", "smartstore-settlements", "issues"],
      });
    },
  });

  if (query.isLoading) return <LoadingSpinner />;
  if (query.error) {
    if (query.error instanceof ApiError && query.error.status === 401) return null;
    return <ErrorAlert error={query.error} />;
  }
  return <>
    <Form className="d-flex flex-wrap align-items-end gap-2 mb-3" onSubmit={(event) => {
      event.preventDefault();
      synchronize.mutate();
    }}>
      <Form.Group><Form.Label className="small">시작일</Form.Label>
        <Form.Control type="date" required value={from} onChange={(event) => setFrom(event.target.value)} />
      </Form.Group>
      <Form.Group><Form.Label className="small">종료일</Form.Label>
        <Form.Control type="date" required value={to} onChange={(event) => setTo(event.target.value)} />
      </Form.Group>
      <Button type="submit" variant="outline-primary" disabled={synchronize.isPending || from > to}>
        {synchronize.isPending ? "조회 중..." : "기간 다시 조회"}
      </Button>
    </Form>
    <ErrorAlert error={synchronize.error} />
    {synchronize.data && <Alert variant={synchronize.data.issueCount ? "warning" : "success"}>
      정상 {synchronize.data.successCount}건, 확인 필요 {synchronize.data.issueCount}건을 반영했습니다.
    </Alert>}
    {!query.data?.length ? (
      <EmptyState message="스마트스토어 주문과 다른 정산 내역이 없습니다." />
    ) : <Table responsive hover size="sm" className="align-middle">
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
    </Table>}
  </>;
}

function shiftDate(days: number): string {
  const value = new Date();
  value.setDate(value.getDate() + days);
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function settlementStatusLabel(status: SmartStoreSettlementIssueResponse["status"]): string {
  if (status === "ORDER_NOT_FOUND") return "채널 주문 원장 없음";
  if (status === "EXPECTED_AMOUNT_MISSING") return "주문 정산 예정액 없음";
  if (status === "AMOUNT_MISMATCH") return "정산 예정액 불일치";
  return "확인 필요";
}
