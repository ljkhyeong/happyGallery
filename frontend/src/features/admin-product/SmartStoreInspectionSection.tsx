import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Table } from "react-bootstrap";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { EmptyState, ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import {
  fetchSmartStoreInspections,
  requestSmartStoreInspectionRestore,
} from "./api";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function SmartStoreInspectionSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [page, setPage] = useState(1);
  const queryKey = ["admin", "smartstore-inspections"] as const;
  const query = useAdminQuery(onAuthError, {
    queryKey: [...queryKey, page],
    queryFn: () => fetchSmartStoreInspections(adminKey, page),
  });
  const restore = useAdminMutation(onAuthError, {
    mutationFn: (channelProductNo: number) =>
      requestSmartStoreInspectionRestore(adminKey, channelProductNo),
    onSuccess: async () => {
      toast.show("스마트스토어에 상품 복원 요청을 보냈습니다.");
      if (query.data?.products.length === 1 && page > 1) setPage(page - 1);
      await queryClient.invalidateQueries({ queryKey });
    },
  });

  return <>
    {query.isLoading && <LoadingSpinner />}
    <ErrorAlert error={query.error} onRetry={() => { void query.refetch(); }} retrying={query.isFetching} />
    <ErrorAlert error={restore.error} />
    {query.data && (!query.data.products.length
      ? <EmptyState message="검수 조치가 필요한 스마트스토어 상품이 없습니다." />
      : <Table responsive hover size="sm" className="align-middle">
      <thead><tr>
        <th>채널상품 번호</th><th>반려 사유</th><th>필요한 조치</th><th></th>
      </tr></thead>
      <tbody>{query.data.products.map((product) => <tr key={product.channelProductNo}>
        <td className="small">{product.channelProductNo}</td>
        <td>{product.reason}</td>
        <td>{product.action}</td>
        <td className="text-end">
          {product.restorationRequestAvailable ? <Button
            size="sm"
            variant="outline-primary"
            disabled={restore.isPending}
            onClick={() => restore.mutate(product.channelProductNo)}
          >수정 반영 후 복원 요청</Button> : <Badge bg="secondary">복원 요청 불가</Badge>}
        </td>
      </tr>)}</tbody>
    </Table>)}
    {(page > 1 || (query.data?.totalPages ?? 0) > 1) && (
      <nav aria-label="스마트스토어 검수 페이지" className="d-flex flex-wrap align-items-center gap-2 mt-3">
        <Button size="sm" variant="outline-secondary"
          disabled={page === 1 || query.isFetching || restore.isPending}
          onClick={() => setPage(page - 1)}>이전 페이지</Button>
        <span className="small text-muted-soft">
          {page}페이지{query.data && ` · 총 ${query.data.totalElements}건`}
        </span>
        <Button size="sm" variant="outline-secondary"
          disabled={!query.data || query.isError || query.isFetching || restore.isPending
            || page >= query.data.totalPages}
          onClick={() => setPage(page + 1)}>다음 페이지</Button>
      </nav>
    )}
  </>;
}
