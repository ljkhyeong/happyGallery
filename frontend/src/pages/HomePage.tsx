import type { CSSProperties } from "react";
import { Col, Container, Row } from "react-bootstrap";
import { Link } from "react-router";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import leatherClass from "@/assets/happygallery/leather-class.jpg";
import groupResinClass from "@/assets/happygallery/group-resin-class.jpg";
import upcyclingClass from "@/assets/happygallery/upcycling-class.jpg";
import { fetchClasses } from "@/features/booking-create/api";
import { NoticeListWidget } from "@/features/notice/NoticeListWidget";
import { FeaturedEventWidget } from "@/features/event/FeaturedEventWidget";
import { fetchProducts } from "@/features/product/api";
import { ProductCard } from "@/features/product/ProductCard";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
import { PUBLIC_DATA_STALE_TIME, REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { formatKRW } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { LinkButton } from "@/shared/ui/LinkButton";
import type { ClassResponse } from "@/generated/api/booking";
import type { EventResponse } from "@/generated/api/event";
import type { NoticeListResponse } from "@/generated/api/notice";
import type { ProductDetailResponse } from "@/generated/api/product";
import type { WorkshopProfileResponse } from "@/generated/api/workshop";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";

const CRAFT_SPECIALTIES = [
  "빈티지 가죽공예",
  "레진아트",
  "플루이드아트",
  "톨페인팅",
  "냅킨아트",
  "양말목공예",
  "하바리움",
  "위빙",
  "POP",
] as const;

const BLOG_STORIES = [
  {
    title: "빈티지가죽 카드지갑 원데이클래스",
    label: "공방 클래스",
    href: "https://blog.naver.com/ssim1972/224351321964",
  },
  {
    title: "예성초등학교 레진아트 키링 수업",
    label: "학교 출강",
    href: "https://blog.naver.com/ssim1972/224329992719",
  },
  {
    title: "새활용 양말목 생활소품",
    label: "공예 이야기",
    href: "https://blog.naver.com/ssim1972/224241899556",
  },
] as const;

interface HomePageProps {
  initialProducts: ProductDetailResponse[];
  initialClasses: ClassResponse[];
  initialEvents: EventResponse[];
  initialNotices: NoticeListResponse[];
  initialWorkshop: WorkshopProfileResponse;
}

export function HomePage({
  initialProducts,
  initialClasses,
  initialEvents,
  initialNotices,
  initialWorkshop,
}: HomePageProps) {
  const {
    data: products,
    error: productsError,
    isLoading: productsLoading,
  } = useLoaderBackedQuery({
    queryKey: queryKeys.catalog.products,
    queryFn: () => fetchProducts(),
    staleTime: PUBLIC_DATA_STALE_TIME,
  }, initialProducts);
  const {
    data: classes,
    error: classesError,
    isLoading: classesLoading,
  } = useLoaderBackedQuery({
    queryKey: queryKeys.catalog.classes,
    queryFn: fetchClasses,
    staleTime: REFERENCE_DATA_STALE_TIME,
  }, initialClasses);
  const { data: workshop } = useWorkshopProfile(initialWorkshop);

  const featuredProducts = products?.filter((product) => product.available).slice(0, 6) ?? [];
  const featuredClasses = classes?.slice(0, 4) ?? [];
  const heroStyle = { "--hg-hero-image": `url(${heroWorkshop})` } as CSSProperties;
  const blogUrl = workshop?.naverBlogUrl;

  return (
    <>
      <section className="store-hero" style={heroStyle}>
        <Container className="store-hero-inner">
          <div className="store-hero-content">
            <p className="store-hero-badge">충주 계명대로 공예공방</p>
            <h1 className="store-hero-title">해피갤러리</h1>
            <p className="store-hero-lead">손으로 만드는 즐거움이 오래 남는 곳</p>
            <p className="store-hero-copy">
              원데이클래스부터 자격증반과 창업반까지,
              다양한 공예를 배우고 나만의 작품을 완성해 보세요.
            </p>
            <div className="store-hero-actions">
              <LinkButton to="/classes" variant="dark" size="lg">클래스 둘러보기</LinkButton>
              <Link to="/products" className="store-hero-text-link">
                공방 작품 보기 <span aria-hidden="true">→</span>
              </Link>
            </div>
          </div>
        </Container>
      </section>

      <section className="home-band home-updates-section anim-fade-up">
        <Container className="home-updates-grid">
          <NoticeListWidget initialNotices={initialNotices} />
          <FeaturedEventWidget initialEvents={initialEvents} />
        </Container>
      </section>

      <section className="home-band home-class-section anim-fade-up">
        <Container>
          <div className="store-section-header home-section-heading">
            <div>
              <p className="store-section-kicker">해피갤러리 클래스</p>
              <h2 className="store-section-title">오늘의 체험부터 오래 배우는 과정까지</h2>
              <p className="store-section-desc">
                처음 만드는 분도 편안하게 시작할 수 있도록 수업별 시간과 준비물을 안내합니다.
              </p>
            </div>
            <Link to="/classes" className="store-section-link">
              전체 클래스 보기 <span aria-hidden="true">→</span>
            </Link>
          </div>

          <p className="home-craft-specialties" aria-label="해피갤러리 공예 분야">
            {CRAFT_SPECIALTIES.join(" · ")}
          </p>

          <div className="home-class-layout">
            <figure className="home-class-media">
              <img src={leatherClass} alt="해피갤러리 가죽공예 수업" />
            </figure>
            <div className="home-class-list">
              {classesLoading && <LoadingSpinner text="클래스를 불러오는 중입니다" />}
              <ErrorAlert error={classesError} />
              {featuredClasses.map((bookingClass) => (
                <Link
                  key={bookingClass.id}
                  to={`/classes/${bookingClass.id}`}
                  className="home-class-row"
                >
                  <div>
                    <strong>{bookingClass.name}</strong>
                    <span>{bookingClass.durationMin}분 · {formatKRW(bookingClass.price)}</span>
                  </div>
                  <span aria-hidden="true">↗</span>
                </Link>
              ))}
              {!classesLoading && !classesError && featuredClasses.length === 0 && (
                <p className="text-muted-soft mb-0">예약 가능한 클래스를 준비하고 있습니다.</p>
              )}
            </div>
          </div>
        </Container>
      </section>

      <section className="home-group-band anim-fade-up anim-delay-1">
        <Container className="home-editorial-layout">
          <figure className="home-editorial-media">
            <img src={groupResinClass} alt="해피갤러리 단체 레진아트 수업" />
          </figure>
          <div className="home-editorial-copy">
            <p className="store-section-kicker">단체·기관 수업</p>
            <h2>함께 만드는 시간이 필요한 곳으로 찾아갑니다</h2>
            <p>
              참여 인원과 장소, 원하는 공예를 알려주시면 수업에 맞는 재료와 진행 방법을 함께 정합니다.
            </p>
            <LinkButton to="/group-classes" variant="light">단체수업 알아보기</LinkButton>
          </div>
        </Container>
      </section>

      <section className="home-band home-story-section anim-fade-up anim-delay-2">
        <Container className="home-story-layout">
          <div className="home-story-copy">
            <p className="store-section-kicker">공방 기록</p>
            <h2>수업과 작품이 쌓여 온 해피갤러리의 시간</h2>
            <p>
              수강생과 함께 만든 작품, 새로운 재료를 만나는 과정, 공방의 일상을 네이버 블로그에 기록합니다.
            </p>
            <div className="home-story-links">
              {BLOG_STORIES.map((story) => (
                <a key={story.href} href={story.href} target="_blank" rel="noreferrer">
                  <span>{story.label}</span>
                  <strong>{story.title}</strong>
                  <span aria-hidden="true">↗</span>
                </a>
              ))}
            </div>
            {blogUrl && (
              <a className="store-section-link" href={blogUrl} target="_blank" rel="noreferrer">
                모든 공방 기록 보기 <span aria-hidden="true">↗</span>
              </a>
            )}
          </div>
          <figure className="home-story-media">
            <img src={upcyclingClass} alt="해피갤러리 업사이클링 공예 수업 기록" />
          </figure>
        </Container>
      </section>

      <section className="home-band home-product-section anim-fade-up anim-delay-3">
        <Container>
          <div className="store-section-header home-section-heading">
            <div>
              <p className="store-section-kicker">공방 작품</p>
              <h2 className="store-section-title">해피갤러리에서 만든 작품을 만나보세요</h2>
              <p className="store-section-desc">바로 구매할 수 있는 작품과 주문 제작 작품을 함께 소개합니다.</p>
            </div>
            <Link to="/products" className="store-section-link">
              모든 작품 보기 <span aria-hidden="true">→</span>
            </Link>
          </div>
          {productsLoading && <LoadingSpinner />}
          <ErrorAlert error={productsError} />
          {featuredProducts.length > 0 && (
            <Row xs={1} sm={2} md={3} className="g-4">
              {featuredProducts.map((product) => (
                <Col key={product.id}><ProductCard product={product} /></Col>
              ))}
            </Row>
          )}
          {!productsLoading && !productsError && featuredProducts.length === 0 && (
            <p className="text-muted-soft">지금 소개할 작품을 준비하고 있습니다.</p>
          )}
        </Container>
      </section>

      <section className="home-band home-workshop-section anim-fade-up">
        <Container><WorkshopVisitInfo /></Container>
      </section>
    </>
  );
}
