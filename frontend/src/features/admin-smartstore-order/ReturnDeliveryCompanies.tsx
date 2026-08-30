import { useState } from "react";
import { Button, Table } from "react-bootstrap";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { fetchSmartStoreReturnDeliveryCompanies } from "./api";

export function ReturnDeliveryCompanies({ adminKey, onAuthError }: {
  adminKey: string;
  onAuthError: () => void;
}) {
  const [open, setOpen] = useState(false);
  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "smartstore-return-delivery-companies"],
    queryFn: () => fetchSmartStoreReturnDeliveryCompanies(adminKey),
    enabled: open,
  });
  return <section className="mb-3">
    <Button size="sm" variant="outline-secondary" aria-expanded={open}
      onClick={() => setOpen(!open)}>등록된 반품 택배사 계약 {open ? "닫기" : "조회"}</Button>
    {open && <div className="border rounded p-3 mt-2">
      <p className="small text-muted">
        스마트스토어센터에 등록된 반품·교환 택배사 계약입니다.
        계약번호는 발송·수거용 택배사 코드가 아니므로 운송장 입력란에 사용하지 마세요.
      </p>
      {query.isLoading && <LoadingSpinner />}
      <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} retrying={query.isFetching} />
      {query.data && (query.data.length ? (
        <Table size="sm" responsive className="small mb-2">
          <thead><tr><th>계약번호</th><th>택배사</th><th>우선순위</th></tr></thead>
          <tbody>{query.data.map((company) => <tr key={company.id}>
            <td>{company.id}</td><td>{company.name}</td>
            <td>{company.priorityType === "PRIMARY" ? "기본" : company.priorityType ?? "—"}</td>
          </tr>)}</tbody>
        </Table>
      ) : <p className="small mb-2">등록된 반품 택배사 계약이 없습니다.</p>)}
      {query.data && <Button size="sm" variant="link" disabled={query.isFetching}
        onClick={() => { void query.refetch(); }}>새로고침</Button>}
    </div>}
  </section>;
}
