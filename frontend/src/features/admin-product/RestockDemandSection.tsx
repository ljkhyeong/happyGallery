import { useState } from "react";
import { Button, Table } from "react-bootstrap";
import { Link } from "react-router";
import { listAdminRestockDemand } from "@/generated/api/adminCatalog";
import { adminHeaders } from "@/shared/api";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";

export function RestockDemandSection({ adminKey, onAuthError }: { adminKey: string; onAuthError: () => void }) {
  const [page, setPage] = useState(0);
  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "restock-demand", page],
    queryFn: () => listAdminRestockDemand({ page, size: 20 }, { headers: adminHeaders(adminKey) }),
  });
  return <div id="admin-restock-demand">
    <p className="small text-muted">알림 대기 인원이 많은 순서입니다. 구매 예약 수량은 아닙니다.</p>
    <Button size="sm" variant="outline-secondary" disabled={query.isFetching} onClick={() => { void query.refetch(); }}>대기 인원 새로고침</Button>
    {query.isLoading && <LoadingSpinner />}
    <ErrorAlert error={query.error} />
    {query.data?.content.length === 0 && <EmptyState message="재입고 알림을 기다리는 고객이 없습니다." />}
    {query.data && query.data.content.length > 0 && <Table responsive size="sm" className="mt-2">
      <thead><tr><th>상품</th><th>신청 옵션</th><th>대기 인원</th><th>재고 관리</th></tr></thead>
      <tbody>{query.data.content.map((row) => <tr key={`${row.productId}:${row.productVariantId ?? 0}`}>
        <td>{row.productName}</td><td>{row.optionLabel}</td><td>{row.waitingCount}명</td>
        <td><Link to={`/admin?view=products&productId=${row.productId}${row.productVariantId ? `&variantId=${row.productVariantId}` : ""}`}>재고 확인</Link></td>
      </tr>)}</tbody>
    </Table>}
    {query.data && query.data.totalPages > 1 && <div className="d-flex align-items-center gap-2">
      <Button size="sm" disabled={page === 0 || query.isFetching} onClick={() => setPage(page - 1)}>이전</Button>
      <span>{page + 1} / {query.data.totalPages}</span>
      <Button size="sm" disabled={page + 1 >= query.data.totalPages || query.isFetching} onClick={() => setPage(page + 1)}>다음</Button>
    </div>}
  </div>;
}
