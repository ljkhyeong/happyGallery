import { Container, Row, Col, Button } from "react-bootstrap";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchProducts } from "@/features/product/api";
import { ProductCard } from "@/features/product/ProductCard";
import { LoadingSpinner } from "@/shared/ui";

export function HomePage() {
  const { data: products, isLoading } = useQuery({
    queryKey: ["products"],
    queryFn: fetchProducts,
  });

  const featured = products?.filter((p) => p.available).slice(0, 6) ?? [];

  return (
    <>
      <section className="home-hero">
        <Container>
          <h1 className="display-6 mb-3">HappyGallery</h1>
          <p className="lead">
            손으로 만드는 즐거움을 나누는 핸드메이드 공방입니다.
            <br />
            체험 클래스 예약부터 작품 구매까지 한 곳에서 만나보세요.
          </p>
        </Container>
      </section>

      <Container className="page-container">
        {/* 상품 섹션 */}
        <section className="mb-5">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h5 className="mb-0">인기 상품</h5>
            <Link to="/products" className="text-decoration-none small">
              전체 보기 &rarr;
            </Link>
          </div>
          {isLoading && <LoadingSpinner />}
          {featured.length > 0 && (
            <Row xs={1} sm={2} md={3} className="g-3">
              {featured.map((p) => (
                <Col key={p.id}>
                  <ProductCard product={p} />
                </Col>
              ))}
            </Row>
          )}
          {!isLoading && featured.length === 0 && (
            <p className="text-muted-soft">등록된 상품이 없습니다.</p>
          )}
        </section>

        {/* 체험 + 8회권 섹션 */}
        <Row xs={1} sm={2} className="g-3">
          <Col>
            <Link to="/bookings/new" className="home-card h-100">
              <div className="home-card-title">체험 예약</div>
              <p className="home-card-desc">
                원하는 클래스와 시간을 골라 예약하세요.
                <br />
                향수, 우드, 니트 등 다양한 체험이 준비되어 있습니다.
              </p>
              <span className="home-card-cta">예약하기 &rarr;</span>
            </Link>
          </Col>
          <Col>
            <Link to="/passes/purchase" className="home-card h-100">
              <div className="home-card-title">8회권</div>
              <p className="home-card-desc">
                8회 이용권으로 더 합리적으로 체험하세요.
                <br />
                구매일로부터 90일간 사용 가능합니다.
              </p>
              <span className="home-card-cta">구매하기 &rarr;</span>
            </Link>
          </Col>
        </Row>

        {/* 비회원 안내 */}
        <section className="mt-5 text-center">
          <p className="text-muted-soft small mb-2">
            이미 주문하셨나요?
          </p>
          <div className="d-flex justify-content-center gap-3">
            <Button as={Link as any} to="/orders/detail" variant="outline-secondary" size="sm">
              주문 조회
            </Button>
            <Button as={Link as any} to="/bookings/manage" variant="outline-secondary" size="sm">
              예약 조회
            </Button>
          </div>
        </section>
      </Container>
    </>
  );
}
