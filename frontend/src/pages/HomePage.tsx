import type { CSSProperties } from "react";
import { Container, Row, Col, Button } from "react-bootstrap";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import studioHero from "@/assets/studio-hero.jpg";
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

  const featured = products?.filter((product) => product.available).slice(0, 6) ?? [];
  const heroStyle = { "--hg-hero-image": `url(${studioHero})` } as CSSProperties;

  return (
    <>
      <section className="store-hero" style={heroStyle}>
        <Container className="store-hero-inner">
          <div className="store-hero-content">
            <p className="store-hero-badge">HANDMADE STORE &amp; WORKSHOP</p>
            <h1 className="store-hero-title">
              손으로 만든 하루를
              <br />
              가까이 두는 방법.
            </h1>
            <p className="store-hero-copy">
              공방에서 천천히 빚은 작품을 만나고,
              직접 만드는 시간을 예약해 보세요.
            </p>
            <div className="store-hero-actions">
              <Button as={Link as any} to="/products" variant="dark" size="lg">
                작품 둘러보기
              </Button>
              <Link to="/bookings/new" className="store-hero-text-link">
                클래스 예약하기 <span aria-hidden="true">→</span>
              </Link>
            </div>
          </div>
        </Container>
      </section>

      <Container className="page-container home-sections">
        <section className="home-product-section anim-fade-up">
          <div className="store-section-header">
            <div>
              <p className="store-section-kicker">오늘의 작품</p>
              <h2 className="store-section-title">공방에서 바로 만날 수 있어요</h2>
              <p className="store-section-desc">
                재고 작품과 주문 제작 작품을 한눈에 살펴보세요.
              </p>
            </div>
            <Link to="/products" className="store-section-link">
              모든 작품 보기 <span aria-hidden="true">→</span>
            </Link>
          </div>
          {isLoading && <LoadingSpinner />}
          {isError && <ErrorAlert error={error} />}
          {featured.length > 0 && (
            <Row xs={1} sm={2} md={3} className="g-4">
              {featured.map((product) => (
                <Col key={product.id}>
                  <ProductCard product={product} />
                </Col>
              ))}
            </Row>
          )}
          {!isLoading && featured.length === 0 && (
            <p className="text-muted-soft">지금 소개할 작품을 준비하고 있습니다.</p>
          )}
        </section>

        <section className="atelier-paths anim-fade-up anim-delay-1">
          <div className="atelier-paths-intro">
            <p className="store-section-kicker">AT THE ATELIER</p>
            <h2>작품을 고르는 일부터<br />직접 만드는 시간까지</h2>
            <p>처음 방문해도 편안하게 시작할 수 있도록 필요한 경로만 담았습니다.</p>
          </div>
          <div className="atelier-paths-links">
            <Link to="/bookings/new" className="atelier-path-link">
              <span className="atelier-path-meta">한 번의 공방 경험</span>
              <strong>클래스 날짜와 시간 고르기</strong>
              <span aria-hidden="true">↗</span>
            </Link>
            <Link to="/passes/purchase" className="atelier-path-link">
              <span className="atelier-path-meta">꾸준한 작업을 위한</span>
              <strong>8회권으로 공방 이어가기</strong>
              <span aria-hidden="true">↗</span>
            </Link>
          </div>
        </section>

        <div className="home-info-grid anim-fade-up anim-delay-2">
          <NoticeListWidget />
          <section className="lookup-panel">
            <p className="store-section-kicker">내 기록 찾기</p>
            <h2 className="lookup-panel-title">주문과 예약을 다시 확인하세요.</h2>
            <p className="store-section-desc">
              회원은 내 정보에서, 비회원은 발급받은 접근 토큰으로 확인할 수 있습니다.
            </p>
            <div className="lookup-panel-actions">
              <Button as={Link as any} to="/my" variant="dark">내 정보</Button>
              <Button
                as={Link as any}
                to="/guest"
                state={{ monitoringSource: "home_lookup_panel" }}
                variant="outline-dark"
              >
                비회원 조회
              </Button>
            </div>
          </section>
        </div>
      </Container>
    </>
  );
}
