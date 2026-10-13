import { FavoriteButton } from "@/features/my/Favorites";
import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { Container } from "react-bootstrap";
import { Link } from "react-router";
import { getPublicClass, type ClassResponse } from "@/generated/api/booking";
import { splitBookingAmount } from "@/features/booking-create/bookingAmounts";
import { ClassUpcomingSlots } from "@/features/booking-create/ClassUpcomingSlots";
import { PublicReviewSection } from "@/features/review/PublicReviewSection";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
import {
  bookingCreateHref,
  classAcceptsPass,
  formatSlotStart,
  isFewSeats,
  nextOpenSlot,
  remainingSeatLabel,
  upcomingSlotsQuery,
} from "@/features/booking-create/upcomingSlots";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import {
  classImageSrc,
  formatKRW,
  getClassCategoryLabel,
  getClassSituationTagLabel,
  isClassSituationTag,
} from "@/shared/lib";
import { ErrorAlert, LinkButton } from "@/shared/ui";

export function ClassDetailPage({ initialClass }: { initialClass: ClassResponse }) {
  const classId = initialClass.id;
  const classQueryKey = useMemo(
    () => queryKeys.catalog.classDetail(classId),
    [classId],
  );
  const {
    data: bookingClass,
    error,
  } = useLoaderBackedQuery({
    queryKey: classQueryKey,
    queryFn: ({ signal }) => getPublicClass(classId, { signal }),
    staleTime: REFERENCE_DATA_STALE_TIME,
  }, initialClass);
  // 다가오는 일정 구역과 같은 캐시를 써서 가장 가까운 회차를 결제 안내에 한 줄로 보여 준다.
  const slotsQuery = useQuery(upcomingSlotsQuery(classId));

  if (error) {
    return <Container className="page-container"><ErrorAlert error={error} /></Container>;
  }
  const passEligible = classAcceptsPass(bookingClass);
  const { depositAmount, balanceAmount } = splitBookingAmount(bookingClass.price);
  const nearestSlot = nextOpenSlot(slotsQuery.data);
  const situationTags = bookingClass.situationTags.filter(isClassSituationTag);

  return (
    <Container className="page-container class-detail-page">
      <nav className="store-detail-breadcrumb" aria-label="경로">
        <Link to="/" className="store-detail-breadcrumb-link">홈</Link>
        <span aria-hidden="true">/</span>
        <Link to="/classes" className="store-detail-breadcrumb-link">클래스</Link>
        <span aria-hidden="true">/</span>
        <span className="store-detail-breadcrumb-current" aria-current="page">{bookingClass.name}</span>
      </nav>

      <div className="class-detail-layout">
        <figure className="class-detail-media">
          <img
            src={classImageSrc(bookingClass)}
            alt={bookingClass.imageUrl ? `${bookingClass.name} 수업` : ""}
          />
        </figure>

        <div className="class-detail-summary">
          <div className="class-detail-badges">
            <span>{getClassCategoryLabel(bookingClass.category)}</span>
            {passEligible && <Link to="/classes?passEligible=true">4회권 사용 가능</Link>}
          </div>
          <h1 className="class-detail-title">{bookingClass.name}</h1>
          <div className="class-detail-price-row">
            <p className="class-detail-price">{formatKRW(bookingClass.price)}</p>
            <span>1인 수업료</span>
            <div className="class-detail-favorite">
              <FavoriteButton type="CLASS" targetId={bookingClass.id} />
            </div>
          </div>
          {situationTags.length > 0 && (
            <nav className="class-detail-tags" aria-label="이 수업이 어울리는 상황">
              {situationTags.map((tag) => (
                <Link key={tag} to={`/classes?tag=${tag}`}>{getClassSituationTagLabel(tag)}</Link>
              ))}
            </nav>
          )}
          <dl className="class-detail-meta">
            <div><dt>소요 시간</dt><dd>{bookingClass.durationMin}분</dd></div>
            <div><dt>회차 정원</dt><dd>{bookingClass.capacity}명</dd></div>
            {bookingClass.targetAudience && <div><dt>추천 대상</dt><dd>{bookingClass.targetAudience}</dd></div>}
            {bookingClass.preparationInfo && <div><dt>준비물</dt><dd>{bookingClass.preparationInfo}</dd></div>}
          </dl>

          <section className="class-detail-card" aria-labelledby="class-payment-title">
            <h2 id="class-payment-title" className="visually-hidden">예약</h2>
            <dl className="class-detail-amounts">
              <div>
                <dt>예약금 <span>수업료의 10% · 예약할 때 결제</span></dt>
                <dd>{formatKRW(depositAmount)}</dd>
              </div>
              <div>
                <dt>잔금 <span>수업 당일 공방에서 결제</span></dt>
                <dd>{formatKRW(balanceAmount)}</dd>
              </div>
              {passEligible && (
                <div>
                  <dt>4회권이 있으면</dt>
                  <dd>예약금 없이 1회 차감</dd>
                </div>
              )}
            </dl>
            <LinkButton
              to={bookingCreateHref(bookingClass.id)}
              variant="dark"
              size="lg"
              className="w-100"
            >
              날짜 선택하고 예약하기
            </LinkButton>
            {nearestSlot && (
              <Link
                to={bookingCreateHref(bookingClass.id, nearestSlot.id, { selectSlot: true })}
                className="class-detail-nearest"
              >
                <span>가장 가까운 회차 <strong>{formatSlotStart(nearestSlot)}</strong></span>
                <b className={isFewSeats(nearestSlot.remainingCapacity) ? "is-few" : undefined}>
                  {remainingSeatLabel(nearestSlot.remainingCapacity)}
                </b>
              </Link>
            )}
          </section>
        </div>
      </div>

      <ClassUpcomingSlots bookingClass={bookingClass} />

      <div className="class-detail-columns">
        {/* 소개 글이 없는 수업은 꾸민 대체 문장을 넣지 않고 구역을 숨긴다. */}
        {bookingClass.description && (
          <section className="class-detail-section" aria-labelledby="class-detail-story-title">
            <div className="class-detail-section-head">
              <h2 id="class-detail-story-title">수업 소개</h2>
            </div>
            <p className="class-detail-description">{bookingClass.description}</p>
          </section>
        )}

        <section className="class-detail-section" aria-labelledby="class-policy-title">
          <div className="class-detail-section-head">
            <h2 id="class-policy-title">예약 안내</h2>
          </div>
          <ol className="class-detail-policy">
            <li>날짜와 시간을 고르고 수업료의 10%를 예약금으로 결제하면 자리가 확정됩니다. 잔금은 수업 당일 공방에서 결제합니다.</li>
            <li>체험 전날 23:59까지 취소하면 예약금을 돌려드립니다. 체험 당일 00:00부터는 환불되지 않습니다.</li>
            {passEligible && <li>4회권이 있다면 예약금 없이 잔여 횟수에서 1회를 차감해 본인 1명을 예약할 수 있습니다.</li>}
          </ol>
        </section>
      </div>

      <section className="class-detail-section" aria-labelledby="class-visit-title">
        <div className="class-detail-section-head">
          <h2 id="class-visit-title">오시는 길</h2>
        </div>
        <WorkshopVisitInfo />
      </section>

      <PublicReviewSection targetType="CLASS" targetId={bookingClass.id} />
    </Container>
  );
}
