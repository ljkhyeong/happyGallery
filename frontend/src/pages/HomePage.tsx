import { Container, Row, Col, Button } from "react-bootstrap";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { fetchProducts } from "@/features/product/api";
import { ProductCard } from "@/features/product/ProductCard";
import { NoticeListWidget } from "@/features/notice/NoticeListWidget";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";

export function HomePage() {
  const { data: products, isLoading, isError, error } = useQuery({
    queryKey: ["products"],
    queryFn: () => fetchProducts(),
    staleTime: PUBLIC_DATA_STALE_TIME,
  });

  const featured = products?.filter((p) => p.available).slice(0, 6) ?? [];
  const readyStockCount = products?.filter((p) => p.type === "READY_STOCK" && p.available).length ?? 0;
  const madeToOrderCount = products?.filter((p) => p.type === "MADE_TO_ORDER" && p.available).length ?? 0;

  return (
    <>
      <section className="store-hero">
        <Container style={{ maxWidth: 1100 }}>
          <Row className="align-items-center g-5">
            <Col lg={7}>
              <div className="store-hero-badge mb-4">
                HANDMADE STORE + WORKSHOP
              </div>
              <h1 className="store-hero-title mb-4">
                작품 구매와 클래스 예약을
                <br />
                한 곳에서.
              </h1>
              <p className="store-hero-copy mb-5">
                HappyGallery는 공방에서 직접 만드는 작품과 체험 클래스를
                함께 운영합니다. 원하는 상품을 고르고, 원하는 시간에 예약하세요.
              </p>
              <div className="d-flex flex-wrap gap-3 store-hero-actions">
                <Button
                  as={Link as any}
                  to="/products"
                  variant="light"
                  size="lg"
                  className="store-hero-btn-primary"
                >
                  SHOP NOW
                </Button>
                <Button
                  as={Link as any}
                  to="/bookings/new"
                  variant="outline-light"
                  size="lg"
                  className="store-hero-btn-secondary"
                >
                  BOOK A CLASS
                </Button>
              </div>
            </Col>
            <Col lg={5}>
              <div className="store-highlight-panel">
                <div className="store-highlight-kicker mb-3">THIS WEEK</div>
                <div className="store-highlight-grid">
                  <div>
                    <div className="store-highlight-value">{featured.length}</div>
                    <div className="store-highlight-label">Available Products</div>
                  </div>
                  <div>
                    <div className="store-highlight-value">{readyStockCount}</div>
                    <div className="store-highlight-label">Ready Stock</div>
                  </div>
                  <div>
                    <div className="store-highlight-value">{madeToOrderCount}</div>
                    <div className="store-highlight-label">Made to Order</div>
                  </div>
                  <div>
                    <div className="store-highlight-value">90<span className="store-highlight-unit">days</span></div>
                    <div className="store-highlight-label">Pass Validity</div>
                  </div>
                </div>
              </div>
            </Col>
          </Row>
        </Container>
      </section>

      <Container className="page-container">
        <NoticeListWidget />

        <section className="mb-5 anim-fade-up anim-delay-1">
          <div className="store-section-header">
            <div>
              <p className="store-section-kicker mb-2">Featured Products</p>
              <h4 className="store-section-title mb-1">
                지금 바로 주문할 수 있는 작품
              </h4>
              <p className="text-muted-soft store-section-desc mb-0">
                상품 상세에서 수량을 고르고 바로 구매하세요.
              </p>
            </div>
            <Link to="/products" className="store-section-link">
              View All →
            </Link>
          </div>
          {isLoading && <LoadingSpinner />}
          {isError && <ErrorAlert error={error} />}
          {featured.length > 0 && (
            <Row xs={1} sm={2} md={3} className="g-4">
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

        <Row xs={1} lg={2} className="g-4 mb-5 anim-fade-up anim-delay-2">
          <Col>
            <Link to="/bookings/new" className="store-feature-card h-100">
              <div className="store-feature-kicker mb-2">Workshop</div>
              <div className="store-feature-title">원하는 클래스와 시간을 먼저 고르세요</div>
              <p className="store-feature-desc">
                슬롯을 먼저 보고 마지막 제출 순간에만 인증을 붙입니다.
                공방 체험 예약을 편리하게 탐색할 수 있습니다.
              </p>
              <span className="store-feature-cta">BOOK NOW →</span>
            </Link>
          </Col>
          <Col>
            <Link to="/passes/purchase" className="store-feature-card h-100 store-feature-card-accent">
              <div className="store-feature-kicker mb-2">Pass</div>
              <div className="store-feature-title">8회권 구매 후 내 정보에서 바로 관리</div>
              <p className="store-feature-desc">
                구매 직후 잔여 횟수를 확인하고, 같은 세션으로 예약까지 이어갈 수 있습니다.
              </p>
              <span className="store-feature-cta">GET PASS →</span>
            </Link>
          </Col>
        </Row>

        <section className="lookup-panel anim-fade-up anim-delay-3">
          <Row className="g-4 align-items-center">
            <Col md={7}>
              <p className="store-section-kicker mb-2">Account</p>
              <h5 className="lookup-panel-title mb-2">
                회원은 내 정보에서, 비회원은 조회 경로에서 확인합니다.
              </h5>
              <p className="text-muted-soft store-section-desc mb-0">
                회원은 로그인 후 주문·예약·8회권을 추가 인증 없이 확인할 수 있습니다.
              </p>
            </Col>
            <Col md={5}>
              <div className="d-flex flex-wrap gap-2 justify-content-md-end">
                <Button as={Link as any} to="/my" variant="dark">
                  MY PAGE
                </Button>
                <Button
                  as={Link as any}
                  to="/guest"
                  state={{ monitoringSource: "home_lookup_panel" }}
                  variant="outline-dark"
                >
                  ORDER LOOKUP
                </Button>
              </div>
            </Col>
          </Row>
        </section>
      </Container>
    </>
  );
}
