import { useParams, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Container, Card, Badge } from "react-bootstrap";
import { fetchProduct } from "@/features/product/api";
import { LoadingSpinner, ErrorAlert } from "@/shared/ui";
import { formatKRW } from "@/shared/lib";

const TYPE_LABEL: Record<string, string> = {
  READY_STOCK: "기존 재고",
  MADE_TO_ORDER: "예약 제작",
};

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const productId = Number(id);

  const { data: product, isLoading, error } = useQuery({
    queryKey: ["products", productId],
    queryFn: () => fetchProduct(productId),
    enabled: productId > 0,
  });

  if (isLoading) return <Container className="page-container"><LoadingSpinner /></Container>;
  if (error) return <Container className="page-container"><ErrorAlert error={error} /></Container>;
  if (!product) return null;

  return (
    <Container className="page-container" style={{ maxWidth: 600 }}>
      <Link to="/products" className="text-decoration-none small d-block mb-3">
        &larr; 상품 목록
      </Link>
      <Card>
        <Card.Body>
          <div className="d-flex justify-content-between align-items-start mb-3">
            <h4 className="mb-0">{product.name}</h4>
            <Badge bg={product.available ? "success" : "secondary"} className="badge-status">
              {product.available ? "구매 가능" : "품절"}
            </Badge>
          </div>
          <p className="text-muted-soft mb-2">{TYPE_LABEL[product.type] ?? product.type}</p>
          <h5>{formatKRW(product.price)}</h5>
        </Card.Body>
      </Card>
    </Container>
  );
}
