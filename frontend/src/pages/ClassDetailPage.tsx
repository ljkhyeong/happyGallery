import { FavoriteButton } from "@/features/my/Favorites";
import { useMemo } from "react";
import { Badge, Card, Container } from "react-bootstrap";
import { Link } from "react-router";
import { getPublicClass, type ClassResponse } from "@/generated/api/booking";
import { ClassUpcomingSlots } from "@/features/booking-create/ClassUpcomingSlots";
import { PublicReviewSection } from "@/features/review/PublicReviewSection";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
import { bookingCreateHref, bookingPaymentHint } from "@/features/booking-create/upcomingSlots";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { classImageSrc, formatKRW, getClassCategoryLabel, isPerfumeClassCategory } from "@/shared/lib";
import { ErrorAlert, LinkButton, LoadingSpinner } from "@/shared/ui";

export function ClassDetailPage({ initialClass }: { initialClass: ClassResponse }) {
  const classId = initialClass.id;
  const classQueryKey = useMemo(
    () => queryKeys.catalog.classDetail(classId),
    [classId],
  );
  const {
    data: bookingClass,
    error,
    isLoading,
  } = useLoaderBackedQuery({
    queryKey: classQueryKey,
    queryFn: ({ signal }) => getPublicClass(classId, { signal }),
    staleTime: REFERENCE_DATA_STALE_TIME,
  }, initialClass);

  if (isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }
  if (error) {
    return <Container className="page-container"><ErrorAlert error={error} /></Container>;
  }
  if (!bookingClass) return null;
  const passEligible = bookingClass.passEligible && !isPerfumeClassCategory(bookingClass.category);

  return (
    <Container className="page-container class-detail-page">
      <nav className="store-detail-breadcrumb" aria-label="경로">
        <Link to="/classes" className="store-detail-breadcrumb-link">클래스</Link>
        <span>/</span>
        <span className="store-detail-breadcrumb-current">{bookingClass.name}</span>
      </nav>

      <div className="class-detail-layout">
        <div className="class-detail-main">
          <figure className="class-detail-media">
            <img
              src={classImageSrc(bookingClass)}
              alt={bookingClass.imageUrl ? `${bookingClass.name} 수업` : ""}
            />
          </figure>
          <section className="class-detail-story" aria-labelledby="class-detail-story-title">
            <h2 id="class-detail-story-title">수업 소개</h2>
            <p className="class-detail-description">
              {bookingClass.description || "재료와 과정을 차근차근 안내하는 해피갤러리 공예 수업입니다."}
            </p>
          </section>

          <ClassUpcomingSlots bookingClass={bookingClass} />

          <section className="class-detail-section" aria-labelledby="class-policy-title">
            <div className="class-detail-section-head">
              <h2 id="class-policy-title">예약 안내</h2>
            </div>
            <ul className="class-detail-policy">
              <li>날짜와 시간을 고르고 수업료의 10%를 예약금으로 결제하면 자리가 확정됩니다. 잔금은 수업 당일 공방에서 결제합니다.</li>
              <li>체험 전날 23:59까지 취소하면 예약금을 돌려드립니다. 체험 당일 00:00부터는 환불되지 않습니다.</li>
              {passEligible && <li>4회권이 있다면 예약금 없이 잔여 횟수에서 1회를 차감해 본인 1명을 예약할 수 있습니다.</li>}
            </ul>
          </section>

          <div className="class-detail-section">
            <WorkshopVisitInfo compact />
          </div>
        </div>

        <Card className="class-detail-card">
          <Card.Body>
            <div className="d-flex flex-wrap align-items-center gap-2 mb-3">
              <Badge bg="dark">
                {isPerfumeClassCategory(bookingClass.category) ? "향수 클래스" : "공예 클래스"}
              </Badge>
              <Badge bg="light" text="dark">{getClassCategoryLabel(bookingClass.category)}</Badge>
              {passEligible && <Badge bg="light" text="dark">이용권 사용 가능</Badge>}
            </div>
            <h1 className="class-detail-title">{bookingClass.name}</h1>
            <FavoriteButton type="CLASS" targetId={bookingClass.id} />
            <p className="class-detail-price">{formatKRW(bookingClass.price)}</p>
            <dl className="class-detail-meta">
              <div><dt>소요 시간</dt><dd>{bookingClass.durationMin}분</dd></div>
              <div><dt>회차 정원</dt><dd>{bookingClass.capacity}명</dd></div>
              {bookingClass.targetAudience && <div><dt>추천 대상</dt><dd>{bookingClass.targetAudience}</dd></div>}
              {bookingClass.preparationInfo && <div><dt>준비물</dt><dd>{bookingClass.preparationInfo}</dd></div>}
            </dl>
            <LinkButton
              to={bookingCreateHref(bookingClass.id)}
              variant="dark"
              size="lg"
              className="w-100"
            >
              날짜 선택하고 예약하기
            </LinkButton>
            <p className="class-detail-helper">{bookingPaymentHint(bookingClass)}</p>
          </Card.Body>
        </Card>
      </div>

      <PublicReviewSection targetType="CLASS" targetId={bookingClass.id} />
    </Container>
  );
}
