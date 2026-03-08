import { useQuery } from "@tanstack/react-query";
import { Table, Badge } from "react-bootstrap";
import { fetchProducts } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";

interface Props {
  adminKey: string;
}

const TYPE_LABEL: Record<string, string> = {
  READY_STOCK: "기존 재고",
  MADE_TO_ORDER: "예약 제작",
};

export function ProductListSection({ adminKey }: Props) {
  const { data: products, isLoading, error } = useQuery({
    queryKey: ["admin", "products"],
    queryFn: () => fetchProducts(adminKey),
  });

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorAlert error={error} />;
  if (!products?.length) return <EmptyState message="등록된 상품이 없습니다." />;

  return (
    <Table responsive hover size="sm">
      <thead>
        <tr>
          <th>ID</th>
          <th>상품명</th>
          <th>유형</th>
          <th className="text-end">가격</th>
          <th className="text-end">수량</th>
          <th>상태</th>
        </tr>
      </thead>
      <tbody>
        {products.map((p) => (
          <tr key={p.id}>
            <td>{p.id}</td>
            <td>{p.name}</td>
            <td>{TYPE_LABEL[p.type] ?? p.type}</td>
            <td className="text-end">{formatKRW(p.price)}</td>
            <td className="text-end">{p.quantity}</td>
            <td>
              <Badge bg={p.available ? "success" : "secondary"}>
                {p.available ? "판매 가능" : "품절"}
              </Badge>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}
