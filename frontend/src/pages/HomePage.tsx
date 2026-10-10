import { useState, type ReactNode } from "react";
import { useQueries } from "@tanstack/react-query";
import { Container } from "react-bootstrap";
import { Link } from "react-router";
import heroWorkshop from "@/assets/happygallery/hero-workshop.jpg";
import leatherWorkshopHands from "@/assets/happygallery/leather-workshop-hands.jpg";
import leatherClass from "@/assets/happygallery/leather-class.jpg";
import resinSeaCoaster from "@/assets/happygallery/resin-sea-coaster.jpg";
import perfumeShelf from "@/assets/happygallery/perfume-shelf.jpg";
import upcyclingClass from "@/assets/happygallery/upcycling-class.jpg";
import toleMaterials from "@/assets/happygallery/tole-materials.jpg";
import acrylicPouring from "@/assets/happygallery/acrylic-pouring.jpg";
import { ClassNextSlot } from "@/features/booking-create/ClassNextSlot";
import {
  classAcceptsPass,
  nextOpenSlot,
  publicClassesQuery,
  SLOT_WHEN_LABEL,
  type SlotWhen,
  slotDayParts,
  upcomingSlotsQuery,
} from "@/features/booking-create/upcomingSlots";
import { NoticeListWidget } from "@/features/notice/NoticeListWidget";
import { FeaturedEventWidget } from "@/features/event/FeaturedEventWidget";
import { useOrderPricePolicy } from "@/features/order/useOrderPricePolicy";
import { fetchProducts } from "@/features/product/api";
import { ProductCard } from "@/features/product/ProductCard";
import { SHOP_CATEGORIES, type ShopCategory } from "@/features/product/shopCategories";
import { useWorkshopProfile } from "@/features/workshop/useWorkshopProfile";
import { PUBLIC_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { CLASS_SITUATION_TAG_OPTIONS, classImageSrc, formatKRW, getClassCategoryLabel } from "@/shared/lib";
import { ErrorAlert, LinkButton } from "@/shared/ui";
import type { ClassResponse } from "@/generated/api/booking";
import type { EventResponse } from "@/generated/api/event";
import type { NoticeListResponse } from "@/generated/api/notice";
import type { ProductDetailResponse } from "@/generated/api/product";
import type { WorkshopProfileResponse } from "@/generated/api/workshop";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";

/** 클래스 상황 바로가기. 주말·오늘은 실제 일정, 나머지는 관리자가 지정한 클래스 상황 태그로 거른다. */
const SITUATIONS = [
  ...(Object.keys(SLOT_WHEN_LABEL) as SlotWhen[]).map((when) => ({
    to: `/classes?when=${when}`,
    label: SLOT_WHEN_LABEL[when],
  })),
  ...CLASS_SITUATION_TAG_OPTIONS.map(({ code, label }) => ({ to: `/classes?tag=${code}`, label })),
];

/** 재료별 전시실. 카테고리 이름은 머리글 카테고리 줄·상품 등록 카테고리와 같다. */
const CATEGORY_ROOMS: ReadonlyArray<{ category: ShopCategory; label: string; image: string }> = [
  { category: "가죽", label: "가죽 소품", image: leatherClass },
  { category: "레진", label: "레진 소품·가구", image: resinSeaCoaster },
  { category: "향·아로마", label: "향수·디퓨저·핸드크림", image: perfumeShelf },
  { category: "새활용", label: "양말목·새활용", image: upcyclingClass },
  { category: "톨페인팅", label: "톨페인팅 원목 소품", image: toleMaterials },
  { category: "플루이드아트", label: "플루이드아트", image: acrylicPouring },
];

const HOME_CLASS_LIMIT = 4;
/** 데스크톱 네 줄 두 번(8개), 모바일은 CSS로 6개까지만 보여 준다. */
const HOME_PRODUCT_LIMIT = 8;
const ALL_CATEGORIES = "전체";

interface HomePageProps {
  initialProducts: ProductDetailResponse[];
  initialClasses: ClassResponse[];
  initialEvents: EventResponse[];
  initialNotices: NoticeListResponse[];
  initialWorkshop: WorkshopProfileResponse;
}

function SectionHead({ number, id, title, children }: {
  number: string;
  id: string;
  title: string;
  children?: ReactNode;
}) {
  return (
    <header className="gallery-section-head">
      <span className="gallery-section-number" aria-hidden="true">{number}</span>
      <h2 id={id}>{title}</h2>
      {children}
    </header>
  );
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
  } = useLoaderBackedQuery({
    queryKey: queryKeys.catalog.products,
    queryFn: () => fetchProducts(),
    staleTime: PUBLIC_DATA_STALE_TIME,
  }, initialProducts);
  const {
    data: classes,
    error: classesError,
  } = useLoaderBackedQuery(publicClassesQuery(), initialClasses);
  const { data: workshop } = useWorkshopProfile(initialWorkshop);
  const { data: pricePolicy } = useOrderPricePolicy();
  const [category, setCategory] = useState<string>(ALL_CATEGORIES);

  const availableProducts = products.filter((product) => product.available);
  // 이미 받은 목록을 화면에서만 나눠 보여 주므로 카테고리를 바꿔도 다시 조회하지 않는다.
  const featuredProducts = availableProducts
    .filter((product) => category === ALL_CATEGORIES || product.category === category)
    .slice(0, HOME_PRODUCT_LIMIT);
  const productCategories = SHOP_CATEGORIES.filter((name) =>
    availableProducts.some((product) => product.category === name));
  const productListHref = category === ALL_CATEGORIES
    ? "/products"
    : `/products?${new URLSearchParams({ category })}`;
  const featuredClasses = classes.slice(0, HOME_CLASS_LIMIT);
  const classSlotResults = useQueries({
    queries: featuredClasses.map((bookingClass) => upcomingSlotsQuery(bookingClass.id)),
  });
  const address = [workshop.addressLine1, workshop.addressLine2].filter(Boolean).join(" ");
  const consultHref = workshop.naverTalkUrl
    ?? (workshop.phone ? `tel:${workshop.phone.replace(/\D/g, "")}` : undefined);
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

      <section className="gallery-hero" aria-labelledby="home-hero-title">
        <Container className="gallery-hero-inner">
          <div className="gallery-hero-copy">
            <p className="gallery-hero-label">
              <span className="gallery-dot" aria-hidden="true" />
              상설전 · 충주 계명대로 공방
            </p>
            <h2 id="home-hero-title" className="gallery-hero-title">손으로 만든<br />것들의<br />작은 상설전</h2>
            <p className="gallery-hero-lead">
              공방에서 가죽, 레진, 향, 양말목, 톨페인팅 작품을 만들어 팝니다.
              마음에 드는 작품은 같은 재료로 직접 만들어 볼 수도 있습니다.
            </p>
            <dl className="gallery-hero-facts">
              {address && <div><dt>장소</dt><dd>{address}</dd></div>}
              {workshop.businessHours && <div><dt>관람</dt><dd>{workshop.businessHours}</dd></div>}
              <div><dt>구매</dt><dd>온라인 주문 · 택배 또는 공방 수령</dd></div>
              <div><dt>체험</dt><dd>원데이 클래스 · 정규반 4회권 · 단체 출강</dd></div>
            </dl>
            <div className="gallery-hero-actions">
              <LinkButton to="/products" variant="dark" size="lg">전시 작품 보기</LinkButton>
              <LinkButton to="/classes" variant="outline-dark" size="lg">체험 프로그램 예약</LinkButton>
            </div>
          </div>
          <figure className="gallery-hero-figure">
            <img src={heroWorkshop} alt="가죽 가방과 소품이 놓인 공방 진열 선반" fetchPriority="high" />
            <figcaption>
              <span>계명대로 공방 진열장</span>
              <span>가죽 가방 · 소품</span>
            </figcaption>
          </figure>
        </Container>
      </section>

      <section className="gallery-section is-tinted" aria-labelledby="home-product-title">
        <Container>
          <SectionHead number="01" id="home-product-title" title="전시 중인 작품">
            {productCategories.length > 0 && (
              <div className="gallery-tabs" role="group" aria-label="작품 카테고리">
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
            <Link to={productListHref} className="gallery-section-more">전체 작품 보기</Link>
          </SectionHead>
          <ErrorAlert error={productsError} />
          {featuredProducts.length > 0 && (
            <div className="product-grid home-product-grid">
              {featuredProducts.map((product, index) => (
                <ProductCard key={product.id} product={product} exhibitNumber={index + 1} />
              ))}
            </div>
          )}
          {!productsError && featuredProducts.length === 0 && (
            <p className="text-muted-soft">지금 소개할 작품을 준비하고 있습니다.</p>
          )}
        </Container>
      </section>

      <section className="gallery-section" aria-labelledby="home-class-title">
        <Container>
          <SectionHead number="02" id="home-class-title" title="체험 프로그램">
            <nav className="gallery-chips" aria-label="상황별로 수업 찾기">
              {SITUATIONS.map(({ to, label }) => <Link key={to} to={to}>{label}</Link>)}
            </nav>
            <Link to="/classes" className="gallery-section-more">전체 클래스 보기</Link>
          </SectionHead>
          <ErrorAlert error={classesError} />
          {featuredClasses.length > 0 && (
            <ul className="gallery-program-list">
              {featuredClasses.map((bookingClass, index) => {
                const next = nextOpenSlot(classSlotResults[index]?.data);
                const day = next ? slotDayParts(next.startAt) : null;
                return (
                  <li key={bookingClass.id}>
                    <Link to={`/classes/${bookingClass.id}`} className="gallery-program-row">
                      {/* 날짜는 아래 다음 수업 문장에도 있으므로 화면 읽기에서는 한 번만 읽힌다. */}
                      <span className="gallery-program-date" aria-hidden="true">
                        {day ? (
                          <>
                            <strong>{day.month}.{day.day}</strong>
                            <span>{day.weekday}요일</span>
                          </>
                        ) : <strong>—</strong>}
                      </span>
                      <img src={classImageSrc(bookingClass)} alt="" loading="lazy" />
                      <span className="gallery-program-body">
                        <span className="gallery-program-meta">
                          {getClassCategoryLabel(bookingClass.category)} · {bookingClass.durationMin}분
                          {classAcceptsPass(bookingClass) && " · 4회권 사용 가능"}
                        </span>
                        <h3>{bookingClass.name}</h3>
                        <ClassNextSlot result={classSlotResults[index]} className="gallery-program-next" />
                      </span>
                      <strong className="gallery-program-price">{formatKRW(bookingClass.price)}</strong>
                      <span className="gallery-program-cta">자세히 보기</span>
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
          {!classesError && featuredClasses.length === 0 && (
            <p className="text-muted-soft">예약 가능한 클래스를 준비하고 있습니다.</p>
          )}
          <div className="gallery-program-links">
            <Link to="/passes/purchase">
              <span>정규반 · 4회권</span>
              <strong>한 재료를 네 번에 걸쳐 깊게</strong>
              <span>4회권 하나로 원하는 회차를 골라 예약합니다. 남은 횟수는 내 정보에서 확인합니다.</span>
            </Link>
            <Link to="/group-classes">
              <span>단체 · 출강</span>
              <strong>학교·기관·동아리 단체 수업</strong>
              <span>인원과 날짜를 알려 주시면 재료를 챙겨 찾아갑니다.</span>
            </Link>
          </div>
        </Container>
      </section>

      <section className="gallery-section is-tinted" aria-labelledby="home-category-title">
        <Container>
          <SectionHead number="03" id="home-category-title" title="재료별 전시실" />
          <ul className="gallery-room-grid">
            {CATEGORY_ROOMS.map(({ category: name, label, image }, index) => (
              <li key={name}>
                <Link to={`/products?${new URLSearchParams({ category: name })}`}>
                  <img src={image} alt="" loading="lazy" />
                  <span className="gallery-room-number">제{index + 1}전시실</span>
                  <span className="gallery-room-name">{label}</span>
                </Link>
              </li>
            ))}
          </ul>
        </Container>
      </section>

      <section className="gallery-section home-updates-section">
        <Container className="home-updates-grid">
          <NoticeListWidget initialNotices={initialNotices} />
          <FeaturedEventWidget initialEvents={initialEvents} />
        </Container>
      </section>

      <section className="gallery-section" aria-labelledby="home-shop-title">
        <Container>
          <div className="gallery-story">
            <img src={leatherWorkshopHands} alt="빈티지 가죽에 각인 도장을 찍는 손" loading="lazy" />
            <div className="gallery-story-copy">
              <p className="gallery-story-label"><span aria-hidden="true">04</span>공방 이야기</p>
              <h2 id="home-shop-title">{workshop.name}</h2>
              {workshop.introduction && <p>{workshop.introduction}</p>}
              {address && <p className="gallery-story-address">{address}</p>}
              <div className="gallery-story-actions">
                {workshop.mapUrl && (
                  <a href={workshop.mapUrl} target="_blank" rel="noreferrer" className="btn btn-outline-dark">오시는 길</a>
                )}
                {consultHref && (
                  <a
                    href={consultHref}
                    target={workshop.naverTalkUrl ? "_blank" : undefined}
                    rel="noreferrer"
                    className="btn btn-dark"
                  >
                    {workshop.naverTalkUrl ? "톡톡 문의" : "전화 문의"}
                  </a>
                )}
              </div>
            </div>
          </div>

          <dl className="gallery-shop-info">
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
