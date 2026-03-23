import { Container, Row, Col, Button, Badge } from "react-bootstrap";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchProducts } from "@/features/product/api";
import { ProductCard } from "@/features/product/ProductCard";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";

export function HomePage() {
  const { data: products, isLoading, isError, error } = useQuery({
    queryKey: ["products"],
    queryFn: fetchProducts,
  });

  const featured = products?.filter((p) => p.available).slice(0, 6) ?? [];
  const readyStockCount = products?.filter((p) => p.type === "READY_STOCK" && p.available).length ?? 0;
  const madeToOrderCount = products?.filter((p) => p.type === "MADE_TO_ORDER" && p.available).length ?? 0;

  return (
    <>
      <section className="store-hero">
        <Container>
          <Row className="align-items-center g-4">
            <Col lg={7}>
              <Badge bg="light" text="dark" className="store-hero-badge mb-3">
                HANDMADE STORE + WORKSHOP BOOKING
              </Badge>
              <h1 className="store-hero-title mb-3">
                선물용 작품 구매와
                <br />
                클래스 예약을 한 흐름으로 연결합니다.
              </h1>
              <p className="store-hero-copy mb-4">
                HappyGallery는 공방에서 직접 만드는 작품과 체험 클래스를 함께 운영합니다.
                회원은 주문과 예약을 한 번에 관리하고, 비회원도 원하는 상품과 시간을 먼저 고른 뒤 필요한 순간에만 인증할 수 있습니다.
              </p>
              <div className="d-flex flex-wrap gap-2">
                <Button as={Link as any} to="/products" variant="primary" size="lg">
                  스토어 둘러보기
                </Button>
                <Button as={Link as any} to="/bookings/new" variant="outline-dark" size="lg">
                  클래스 예약하기
                </Button>
              </div>
            </Col>
            <Col lg={5}>
              <div className="store-highlight-panel">
                <div className="store-highlight-kicker">THIS WEEK</div>
                <div className="store-highlight-grid">
                  <div>
                    <div className="store-highlight-value">{featured.length}</div>
                    <div className="store-highlight-label">지금 주문 가능한 상품</div>
                  </div>
                  <div>
                    <div className="store-highlight-value">{readyStockCount}</div>
                    <div className="store-highlight-label">즉시 판매 상품</div>
                  </div>
                  <div>
                    <div className="store-highlight-value">{madeToOrderCount}</div>
                    <div className="store-highlight-label">예약 제작 상품</div>
                  </div>
                  <div>
                    <div className="store-highlight-value">90일</div>
                    <div className="store-highlight-label">8회권 사용 기한</div>
                  </div>
                </div>
              </div>
            </Col>
          </Row>
        </Container>
      </section>

      <Container className="page-container">
        <section className="store-section mb-5">
          <div className="store-section-header">
            <div>
              <p className="store-section-kicker mb-2">Featured Products</p>
              <h5 className="mb-1">지금 바로 주문할 수 있는 작품</h5>
              <p className="text-muted-soft mb-0">
                상품 상세에서 수량을 고르고 회원이면 바로 구매, 비회원이면 fallback 주문으로 이어집니다.
              </p>
            </div>
            <Link to="/products" className="text-decoration-none small">
              전체 보기 &rarr;
            </Link>
          </div>
          {isLoading && <LoadingSpinner />}
          {isError && <ErrorAlert error={error} />}
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

        <Row xs={1} lg={2} className="g-3 mb-5">
          <Col>
            <Link to="/bookings/new" className="store-feature-card h-100">
              <div className="store-feature-kicker">Workshop Flow</div>
              <div className="store-feature-title">원하는 클래스와 시간을 먼저 고르세요</div>
              <p className="store-feature-desc">
                슬롯을 먼저 보고 마지막 제출 순간에만 회원/비회원 인증을 붙입니다.
                공방 체험 예약을 커머스처럼 탐색할 수 있게 정리한 경로입니다.
              </p>
              <span className="store-feature-cta">체험 예약으로 이동 &rarr;</span>
            </Link>
          </Col>
          <Col>
            <Link to="/passes/purchase" className="store-feature-card h-100 store-feature-card-accent">
              <div className="store-feature-kicker">Member Pass</div>
              <div className="store-feature-title">8회권 구매 후 내 정보에서 바로 관리</div>
              <p className="store-feature-desc">
                회원은 구매 직후 `내 8회권`에서 잔여 횟수를 확인하고, 같은 세션으로 예약까지 이어갈 수 있습니다.
              </p>
              <span className="store-feature-cta">8회권 보러가기 &rarr;</span>
            </Link>
          </Col>
        </Row>

        <section className="lookup-panel">
          <Row className="g-4 align-items-center">
            <Col md={7}>
              <p className="store-section-kicker mb-2">Lookup & Account</p>
              <h5 className="mb-2">회원은 내 정보에서, 비회원은 조회 경로에서 확인합니다.</h5>
              <p className="text-muted-soft mb-0">
                회원은 로그인 후 주문·예약·8회권을 추가 인증 없이 확인할 수 있고,
                비회원 조회는 이미 생성된 주문과 예약을 확인하는 보조 경로로 유지됩니다.
              </p>
            </Col>
            <Col md={5}>
              <div className="lookup-panel-note mb-3">
                비회원 조회는 생성 후 확인용입니다. 계속 관리할 예정이라면 로그인 후 `/my` 경로를 사용하는 편이 안정적입니다.
              </div>
              <div className="d-flex flex-wrap gap-2 justify-content-md-end">
                <Button as={Link as any} to="/my" variant="dark">
                  회원 내 정보
                </Button>
                <Button
                  as={Link as any}
                  to="/guest"
                  state={{ monitoringSource: "home_lookup_panel" }}
                  variant="outline-secondary"
                >
                  비회원 조회 안내
                </Button>
              </div>
            </Col>
          </Row>
        </section>
      </Container>
    </>
  );
}
