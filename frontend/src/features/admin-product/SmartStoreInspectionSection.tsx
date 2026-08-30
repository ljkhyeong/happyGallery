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
  const queryKey = ["admin", "smartstore-inspections"] as const;
  const query = useAdminQuery(onAuthError, {
    queryKey,
    queryFn: () => fetchSmartStoreInspections(adminKey),
  });
  const restore = useAdminMutation(onAuthError, {
    mutationFn: (channelProductNo: number) =>
      requestSmartStoreInspectionRestore(adminKey, channelProductNo),
    onSuccess: async () => {
      toast.show("스마트스토어에 상품 복원 요청을 보냈습니다.");
      await queryClient.invalidateQueries({ queryKey });
    },
  });

  if (query.isLoading) return <LoadingSpinner />;
  if (query.error) return <ErrorAlert error={query.error} />;
  if (!query.data?.products.length) {
    return <EmptyState message="검수 조치가 필요한 스마트스토어 상품이 없습니다." />;
  }
  return <>
    <ErrorAlert error={restore.error} />
    <Table responsive hover size="sm" className="align-middle">
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
    </Table>
  </>;
}
