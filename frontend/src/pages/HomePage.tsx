import { useQueries, type UseQueryResult } from "@tanstack/react-query";
import { Container } from "react-bootstrap";
import { Link } from "react-router";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import groupResinClass from "@/assets/happygallery/group-resin-class.jpg";
import upcyclingClass from "@/assets/happygallery/upcycling-class.jpg";
import { fetchClasses } from "@/features/booking-create/api";
import { QuickBookingPanel } from "@/features/booking-create/QuickBookingPanel";
import {
  formatSlotStart,
  nextOpenSlot,
  remainingSeatLabel,
  UPCOMING_SLOT_DAYS,
  upcomingSlotsQuery,
} from "@/features/booking-create/upcomingSlots";
import { NoticeListWidget } from "@/features/notice/NoticeListWidget";
import { FeaturedEventWidget } from "@/features/event/FeaturedEventWidget";
import { fetchProducts } from "@/features/product/api";
import { ProductCard } from "@/features/product/ProductCard";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
import { PUBLIC_DATA_STALE_TIME, REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { classImageSrc, formatKRW, getClassCategoryLabel, isPerfumeClassCategory } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { LinkButton } from "@/shared/ui/LinkButton";
import type { ClassResponse, PublicSlotResponse } from "@/generated/api/booking";
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

const SHORTCUTS = [
  { to: "/classes", label: "원데이 클래스", description: "날짜를 골라 바로 예약" },
  { to: "/passes/purchase", label: "정규 4회권", description: "꾸준히 배우는 정규 과정" },
  { to: "/group-classes", label: "단체·기관 수업", description: "학교·기관으로 찾아가는 수업" },
  { to: "/guest", label: "비회원 조회", description: "주문·예약 번호로 확인" },
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

const HOME_CLASS_LIMIT = 3;
const HOME_PRODUCT_LIMIT = 8;

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

  const availableProducts = products?.filter((product) => product.available) ?? [];
  // 4열 그리드의 마지막 줄이 비지 않도록 4개 이상이면 4의 배수만 보여 준다.
  const featuredProducts = availableProducts.slice(0, availableProducts.length < 4
    ? availableProducts.length
    : Math.min(HOME_PRODUCT_LIMIT, availableProducts.length - (availableProducts.length % 4)));
  const featuredClasses = classes?.slice(0, HOME_CLASS_LIMIT) ?? [];
  const blogUrl = workshop?.naverBlogUrl;
  const classSlotResults = useQueries({
    queries: featuredClasses.map((bookingClass) => upcomingSlotsQuery(bookingClass.id)),
  });

  return (
    <>
      <section className="home-hero">
        <Container className="home-hero-inner">
          <div className="home-hero-visual">
            <img
              src={heroWorkshop}
              alt="빈티지 가죽 가방과 소품이 진열된 해피갤러리 공방 내부"
              fetchPriority="high"
            />
            <div className="home-hero-copy">
              <p className="home-hero-eyebrow">충주 계명대로 공예공방</p>
              <h1 className="home-hero-title">해피갤러리</h1>
              <p className="home-hero-lead">손으로 만드는 즐거움</p>
              <p className="home-hero-text">
                원데이클래스부터 자격증반·창업반까지.
                공예를 배우고 나만의 작품을 만들어 보세요.
              </p>
              <div className="home-hero-actions">
                <LinkButton to="/classes" variant="light">클래스 둘러보기</LinkButton>
                <LinkButton to="/products" variant="outline-light">공방 작품 보기</LinkButton>
              </div>
            </div>
          </div>
          <QuickBookingPanel classes={classes ?? []} />
        </Container>
      </section>

      <nav className="home-shortcuts" aria-label="해피갤러리 바로가기">
        <Container className="home-shortcuts-grid">
          {SHORTCUTS.map((shortcut) => (
            <Link key={shortcut.to} to={shortcut.to} className="home-shortcut">
              <strong>{shortcut.label}</strong>
              <span>{shortcut.description}</span>
              <span className="home-shortcut-arrow" aria-hidden="true">→</span>
            </Link>
          ))}
        </Container>
      </nav>

      <section className="home-section" aria-labelledby="home-class-title">
        <Container>
          <header className="home-section-head">
            <div>
              <p className="store-section-kicker">Class</p>
              <h2 id="home-class-title" className="home-section-title">나만의 작품을 만드는 클래스</h2>
              <p className="home-section-desc">
                처음 만드는 분도 편안하게 시작할 수 있도록 수업별 시간과 준비물을 안내합니다.
              </p>
            </div>
            <Link to="/classes" className="home-section-link">
              전체 클래스 보기 <span aria-hidden="true">→</span>
            </Link>
          </header>

          <ul className="home-craft-list" aria-label="해피갤러리 공예 분야">
            {CRAFT_SPECIALTIES.map((craft) => <li key={craft}>{craft}</li>)}
          </ul>

          {classesLoading && <LoadingSpinner text="클래스를 불러오는 중입니다" />}
          <ErrorAlert error={classesError} />
          {featuredClasses.length > 0 && (
            <div className="home-class-grid">
              {featuredClasses.map((bookingClass, index) => (
                <Link key={bookingClass.id} to={`/classes/${bookingClass.id}`} className="home-class-card">
                  <figure className="home-class-card-media">
                    <img src={classImageSrc(bookingClass)} alt="" loading="lazy" />
                  </figure>
                  <div className="home-class-card-body">
                    <span className="home-class-card-meta">
                      {getClassCategoryLabel(bookingClass.category)}
                      {bookingClass.passEligible && !isPerfumeClassCategory(bookingClass.category) && " · 4회권 사용 가능"}
                    </span>
                    <h3>{bookingClass.name}</h3>
                    <p>{bookingClass.durationMin}분 · {formatKRW(bookingClass.price)}</p>
                    <ClassNextSlot result={classSlotResults[index]} />
                  </div>
                </Link>
              ))}
            </div>
          )}
          {!classesLoading && !classesError && featuredClasses.length === 0 && (
            <p className="text-muted-soft mb-0">예약 가능한 클래스를 준비하고 있습니다.</p>
          )}
        </Container>
      </section>

      <section className="home-section home-section-raised" aria-labelledby="home-product-title">
        <Container>
          <header className="home-section-head">
            <div>
              <p className="store-section-kicker">Shop</p>
              <h2 id="home-product-title" className="home-section-title">공방에서 만든 핸드메이드 작품</h2>
              <p className="home-section-desc">바로 구매할 수 있는 작품과 주문 제작 작품을 함께 소개합니다.</p>
            </div>
            <Link to="/products" className="home-section-link">
              모든 작품 보기 <span aria-hidden="true">→</span>
            </Link>
          </header>
          {productsLoading && <LoadingSpinner />}
          <ErrorAlert error={productsError} />
          {featuredProducts.length > 0 && (
            <div className="product-grid home-product-grid">
              {featuredProducts.map((product) => <ProductCard key={product.id} product={product} />)}
            </div>
          )}
          {!productsLoading && !productsError && featuredProducts.length === 0 && (
            <p className="text-muted-soft">지금 소개할 작품을 준비하고 있습니다.</p>
          )}
        </Container>
      </section>

      <section className="home-group-band" aria-labelledby="home-group-title">
        <Container className="home-editorial-layout">
          <figure className="home-editorial-media">
            <img src={groupResinClass} alt="해피갤러리 단체 레진아트 수업 결과물" loading="lazy" />
          </figure>
          <div className="home-editorial-copy">
            <p className="store-section-kicker">단체·기관 수업</p>
            <h2 id="home-group-title">함께 만드는 시간이 필요한 곳으로 찾아갑니다</h2>
            <p>
              참여 인원과 장소, 원하는 공예를 알려주시면 수업에 맞는 재료와 진행 방법을 함께 정합니다.
            </p>
            <LinkButton to="/group-classes" variant="light">단체수업 알아보기</LinkButton>
          </div>
        </Container>
      </section>

      <section className="home-section home-updates-section">
        <Container className="home-updates-grid">
          <NoticeListWidget initialNotices={initialNotices} />
          <FeaturedEventWidget initialEvents={initialEvents} />
        </Container>
      </section>

      <section className="home-section home-story-section" aria-labelledby="home-story-title">
        <Container className="home-story-layout">
          <figure className="home-story-media">
            <img src={upcyclingClass} alt="해피갤러리 업사이클링 공예 수업 기록" loading="lazy" />
          </figure>
          <div className="home-story-copy">
            <p className="store-section-kicker">공방 기록</p>
            <h2 id="home-story-title">수업과 작품이 쌓여 온 해피갤러리의 시간</h2>
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
              <a className="home-section-link" href={blogUrl} target="_blank" rel="noreferrer">
                모든 공방 기록 보기 <span aria-hidden="true">↗</span>
              </a>
            )}
          </div>
        </Container>
      </section>

      <section className="home-section home-workshop-section">
        <Container><WorkshopVisitInfo /></Container>
      </section>
    </>
  );
}

function ClassNextSlot({ result }: { result: UseQueryResult<PublicSlotResponse[]> | undefined }) {
  if (!result || result.isPending) {
    return <p className="home-class-card-next">일정 확인 중</p>;
  }
  if (result.isError) {
    return <p className="home-class-card-next">일정은 수업 상세에서 확인해 주세요</p>;
  }
  const next = nextOpenSlot(result.data);
  if (!next) {
    return <p className="home-class-card-next">{UPCOMING_SLOT_DAYS}일 안에 예약 가능한 일정 없음</p>;
  }
  return (
    <p className="home-class-card-next">
      <span>다음 수업 {formatSlotStart(next)}</span>
      <b className={next.remainingCapacity <= 2 ? "is-few" : undefined}>{remainingSeatLabel(next.remainingCapacity)}</b>
    </p>
  );
}
