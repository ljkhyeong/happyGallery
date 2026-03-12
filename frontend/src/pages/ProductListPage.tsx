import { useQuery } from "@tanstack/react-query";
import { Container, Row, Col } from "react-bootstrap";
import { fetchProducts } from "@/features/product/api";
import { ProductCard } from "@/features/product/ProductCard";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";

export function ProductListPage() {
  const { data: products, isLoading, error } = useQuery({
    queryKey: ["products"],
    queryFn: fetchProducts,
  });

  return (
    <Container className="page-container">
      <h4 className="mb-4">상품</h4>
      {isLoading && <LoadingSpinner />}
      <ErrorAlert error={error} />
      {products && products.length === 0 && <EmptyState message="등록된 상품이 없습니다." />}
      {products && products.length > 0 && (
        <Row xs={1} sm={2} md={3} className="g-3">
          {products.map((p) => (
            <Col key={p.id}>
              <ProductCard product={p} />
            </Col>
          ))}
        </Row>
      )}
    </Container>
  );
}
