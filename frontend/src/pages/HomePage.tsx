import { useState } from "react";
import { useQueries } from "@tanstack/react-query";
import { Container } from "react-bootstrap";
import { Link } from "react-router";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import groupResinClass from "@/assets/happygallery/group-resin-class.jpg";
import leatherWorkshopHands from "@/assets/happygallery/leather-workshop-hands.jpg";
import leatherClass from "@/assets/happygallery/leather-class.jpg";
import resinSeaCoaster from "@/assets/happygallery/resin-sea-coaster.jpg";
import perfumeShelf from "@/assets/happygallery/perfume-shelf.jpg";
import upcyclingClass from "@/assets/happygallery/upcycling-class.jpg";
import toleMaterials from "@/assets/happygallery/tole-materials.jpg";
import acrylicPouring from "@/assets/happygallery/acrylic-pouring.jpg";
import { fetchClasses } from "@/features/booking-create/api";
import { ClassNextSlot } from "@/features/booking-create/ClassNextSlot";
import { upcomingSlotsQuery } from "@/features/booking-create/upcomingSlots";
import { NoticeListWidget } from "@/features/notice/NoticeListWidget";
import { FeaturedEventWidget } from "@/features/event/FeaturedEventWidget";
import { useOrderPricePolicy } from "@/features/order/useOrderPricePolicy";
import { fetchProducts } from "@/features/product/api";
import { ProductCard } from "@/features/product/ProductCard";
import { SHOP_CATEGORIES, type ShopCategory } from "@/features/product/shopCategories";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import { PUBLIC_DATA_STALE_TIME, REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { classImageSrc, formatKRW, getClassCategoryLabel, isPerfumeClassCategory } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner } from "@/shared/ui";
import type { ClassResponse } from "@/generated/api/booking";
import type { EventResponse } from "@/generated/api/event";
import type { NoticeListResponse } from "@/generated/api/notice";
import type { ProductDetailResponse } from "@/generated/api/product";
import type { WorkshopProfileResponse } from "@/generated/api/workshop";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";

/** 클래스 상황 바로가기. 주말·오늘은 실제 일정, 나머지는 관리자가 지정한 클래스 상황 태그로 거른다. */
const SITUATIONS = [
  { to: "/classes?when=weekend", label: "이번 주말" },
  { to: "/classes?when=today", label: "오늘 바로" },
  { to: "/classes?tag=DATE", label: "데이트" },
  { to: "/classes?tag=WITH_KIDS", label: "아이와 함께" },
  { to: "/classes?tag=FRIENDS", label: "친구 모임" },
  { to: "/classes?tag=GIFT", label: "선물 만들기" },
] as const;

/** 카테고리 사진 칸. 카테고리 이름은 머리글 카테고리 줄·상품 등록 카테고리와 같다. */
const CATEGORY_TILES: ReadonlyArray<{ category: ShopCategory; label: string; image: string }> = [
  { category: "가죽", label: "가죽 소품", image: leatherClass },
  { category: "레진", label: "레진 소품·가구", image: resinSeaCoaster },
  { category: "향·아로마", label: "향수·디퓨저·핸드크림", image: perfumeShelf },
  { category: "새활용", label: "양말목·새활용", image: upcyclingClass },
  { category: "톨페인팅", label: "톨페인팅 원목 소품", image: toleMaterials },
  { category: "플루이드아트", label: "플루이드아트", image: acrylicPouring },
];

const HOME_CLASS_LIMIT = 4;
const HOME_PRODUCT_LIMIT = 10;
const ALL_CATEGORIES = "전체";

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
  const { data: pricePolicy } = useOrderPricePolicy();
  const [category, setCategory] = useState<string>(ALL_CATEGORIES);

  const availableProducts = products?.filter((product) => product.available) ?? [];
  // 이미 받은 목록을 화면에서만 나눠 보여 주므로 카테고리를 바꿔도 다시 조회하지 않는다.
  const featuredProducts = availableProducts
    .filter((product) => category === ALL_CATEGORIES || product.category === category)
    .slice(0, HOME_PRODUCT_LIMIT);
  const productCategories = SHOP_CATEGORIES.filter((name) =>
    availableProducts.some((product) => product.category === name));
  const productListHref = category === ALL_CATEGORIES
    ? "/products"
    : `/products?${new URLSearchParams({ category })}`;
  const featuredClasses = classes?.slice(0, HOME_CLASS_LIMIT) ?? [];
  const classSlotResults = useQueries({
    queries: featuredClasses.map((bookingClass) => upcomingSlotsQuery(bookingClass.id)),
  });
  const consultHref = workshop?.naverTalkUrl
    ?? (workshop?.phone ? `tel:${workshop.phone.replace(/\D/g, "")}` : undefined);
  const shippingFee = pricePolicy?.shippingFee;
  const shippingText = typeof shippingFee !== "number"
    ? "매장 수령은 무료이며, 택배 배송비는 주문서에서 확인할 수 있습니다."
    : shippingFee === 0
      ? "매장 수령·택배 배송 모두 무료입니다."
      : `매장 수령은 무료, 택배 배송은 주문당 ${formatKRW(shippingFee)}입니다.`;

  return (
    <>
      {/* 검색 결과 제목에 공방 이름이 들어가도록 화면에는 숨긴 H1을 둔다(배포 점검도 이 H1을 확인한다). */}
      <h1 className="visually-hidden">충주 공예 공방 해피갤러리</h1>

      <section className="market-banners" aria-label="기획 안내">
        <Container className="market-banners-inner">
          <Link to="/products?type=MADE_TO_ORDER" className="market-banner-main">
            <img src={leatherWorkshopHands} alt="빈티지 가죽에 각인 도장을 찍는 손" fetchPriority="high" />
            <span className="market-banner-copy">
              <span className="market-banner-kicker">주문 제작</span>
              <strong>원하는 문구와 색으로<br />만드는 빈티지 가죽 소품</strong>
              <span>카드지갑, 러기지 택, 다이어리를 주문받아 공방에서 직접 만듭니다.</span>
              <span className="market-banner-link">주문 제작 작품 보기</span>
            </span>
          </Link>
          <div className="market-banner-side">
            <Link to="/classes" className="market-banner-tile">
              <img src={heroWorkshop} alt="" loading="lazy" />
              <span>
                <strong>원데이 클래스</strong>
                <span>공방에서 하루 만에 작품을 완성합니다</span>
              </span>
            </Link>
            <Link to="/group-classes" className="market-banner-tile">
              <img src={groupResinClass} alt="" loading="lazy" />
              <span>
                <strong>학교·기관·동아리 단체수업</strong>
                <span>재료를 챙겨 찾아갑니다</span>
              </span>
            </Link>
          </div>
        </Container>
      </section>

      <section className="market-section" aria-labelledby="home-product-title">
        <Container>
          <header className="market-section-head">
            <h2 id="home-product-title">새로 올라온 작품</h2>
            {productCategories.length > 0 && (
              <div className="market-chips" role="group" aria-label="작품 카테고리">
                {[ALL_CATEGORIES, ...productCategories].map((name) => (
                  <button
                    key={name}
                    type="button"
                    aria-pressed={category === name}
                    onClick={() => setCategory(name)}
                  >
                    {name}
                  </button>
                ))}
              </div>
            )}
            <Link to={productListHref} className="market-section-more">전체 작품 보기</Link>
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

      <section className="market-section" aria-labelledby="home-class-title">
        <Container>
          <div className="market-class-box">
            <header className="market-section-head">
              <h2 id="home-class-title">공방에서 직접 만들어 보기</h2>
              <nav className="market-chips" aria-label="상황별로 수업 찾기">
                {SITUATIONS.map(({ to, label }) => <Link key={to} to={to}>{label}</Link>)}
              </nav>
              <Link to="/passes/purchase" className="market-section-more">정규반·4회권 보기</Link>
            </header>
            {classesLoading && <LoadingSpinner text="클래스를 불러오는 중입니다" />}
            <ErrorAlert error={classesError} />
            {featuredClasses.length > 0 && (
              <div className="market-class-grid">
                {featuredClasses.map((bookingClass, index) => (
                  <Link key={bookingClass.id} to={`/classes/${bookingClass.id}`} className="market-class-card">
                    <img src={classImageSrc(bookingClass)} alt="" loading="lazy" />
                    <span className="market-class-body">
                      <span className="market-class-meta">
                        {getClassCategoryLabel(bookingClass.category)} · {bookingClass.durationMin}분
                        {bookingClass.passEligible && !isPerfumeClassCategory(bookingClass.category) && " · 4회권 사용 가능"}
                      </span>
                      <h3>{bookingClass.name}</h3>
                      <strong className="market-class-price">{formatKRW(bookingClass.price)}</strong>
                      <ClassNextSlot result={classSlotResults[index]} className="market-class-next" />
                    </span>
                  </Link>
                ))}
              </div>
            )}
            {!classesLoading && !classesError && featuredClasses.length === 0 && (
              <p className="text-muted-soft mb-0">예약 가능한 클래스를 준비하고 있습니다.</p>
            )}
          </div>
        </Container>
      </section>

      <section className="market-section" aria-labelledby="home-category-title">
        <Container>
          <header className="market-section-head">
            <h2 id="home-category-title">카테고리별로 보기</h2>
          </header>
          <ul className="market-category-grid">
            {CATEGORY_TILES.map(({ category: name, label, image }) => (
              <li key={name}>
                <Link to={`/products?${new URLSearchParams({ category: name })}`}>
                  <img src={image} alt="" loading="lazy" />
                  <span>{label}</span>
                </Link>
              </li>
            ))}
          </ul>
        </Container>
      </section>

      <section className="market-section home-updates-section">
        <Container className="home-updates-grid">
          <NoticeListWidget initialNotices={initialNotices} />
          <FeaturedEventWidget initialEvents={initialEvents} />
        </Container>
      </section>

      <section className="market-section" aria-labelledby="home-shop-title">
        <Container>
          <div className="market-shop-profile">
            <div>
              <h2 id="home-shop-title">{workshop?.name ?? "해피갤러리"}</h2>
              {workshop?.introduction && <p>{workshop.introduction}</p>}
              {workshop?.addressLine1 && (
                <p className="market-shop-address">
                  {[workshop.addressLine1, workshop.addressLine2].filter(Boolean).join(" ")}
                </p>
              )}
            </div>
            <div className="market-shop-actions">
              {workshop?.mapUrl && (
                <a href={workshop.mapUrl} target="_blank" rel="noreferrer" className="btn btn-outline-dark">오시는 길</a>
              )}
              {consultHref && (
                <a
                  href={consultHref}
                  target={workshop?.naverTalkUrl ? "_blank" : undefined}
                  rel="noreferrer"
                  className="btn btn-dark"
                >
                  {workshop?.naverTalkUrl ? "톡톡 문의" : "전화 문의"}
                </a>
              )}
            </div>
          </div>

          <dl className="market-shop-info">
            <div>
              <dt>배송</dt>
              <dd>{shippingText}</dd>
            </div>
            <div>
              <dt>교환·반품</dt>
              <dd>주문 제작 작품은 결제 전 동의한 제작 조건에 따라 단순 변심 반품이 제한될 수 있습니다.</dd>
            </div>
            <div>
              <dt>회원 혜택</dt>
              <dd>회원은 쿠폰과 결제한 상품 금액 1% 적립을 받습니다. 비회원 주문도 됩니다.</dd>
            </div>
          </dl>
        </Container>
      </section>
    </>
  );
}
