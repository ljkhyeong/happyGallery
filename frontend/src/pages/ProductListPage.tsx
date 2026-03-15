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
      <section className="store-list-header mb-4">
        <p className="store-section-kicker mb-2">Store Catalog</p>
        <div className="d-flex flex-column flex-md-row justify-content-between gap-3 align-items-md-end">
          <div>
            <h4 className="mb-1">작품 스토어</h4>
            <p className="text-muted-soft mb-0">
              바로 판매 가능한 상품과 예약 제작 상품을 한 곳에서 확인하세요.
            </p>
          </div>
          <div className="store-list-meta">
            {products ? `${products.length}개의 상품` : "상품을 불러오는 중"}
          </div>
        </div>
      </section>
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
