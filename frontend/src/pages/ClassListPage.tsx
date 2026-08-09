import type { CSSProperties } from "react";
import { useQuery } from "@tanstack/react-query";
import { Container } from "react-bootstrap";
import { Link } from "react-router";
import leatherClass from "@/assets/happygallery/leather-class.jpg";
import { fetchClasses } from "@/features/booking-create/api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { formatKRW } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { LinkButton } from "@/shared/ui/LinkButton";

export function ClassListPage() {
  const classesQuery = useQuery({
    queryKey: ["classes"],
    queryFn: fetchClasses,
    staleTime: REFERENCE_DATA_STALE_TIME,
  });
  const heroStyle = { "--hg-class-hero-image": `url(${leatherClass})` } as CSSProperties;

  return (
    <>
      <section className="class-catalog-hero" style={heroStyle}>
        <Container>
          <div className="class-catalog-hero-copy">
            <p className="store-section-kicker">해피갤러리 클래스</p>
            <h1>내 손으로 완성하는<br />한 번의 즐거움</h1>
            <p>수업별 소요시간과 가격을 확인하고 원하는 날짜를 예약하세요.</p>
          </div>
        </Container>
      </section>

      <Container className="page-container class-catalog-page">
        <header className="class-catalog-header">
          <div>
            <p className="store-section-kicker">예약 가능한 수업</p>
            <h2>클래스 선택</h2>
          </div>
          <LinkButton to="/group-classes" variant="outline-dark">단체수업 문의</LinkButton>
        </header>

        {classesQuery.isLoading && <LoadingSpinner text="클래스를 불러오는 중입니다" />}
        <ErrorAlert error={classesQuery.error} />
        {classesQuery.data?.length === 0 && <EmptyState message="예약 가능한 클래스를 준비하고 있습니다." />}

        <div className="class-catalog-list">
          {classesQuery.data?.map((bookingClass, index) => (
            <article
              className={bookingClass.imageUrl ? "class-catalog-item has-media" : "class-catalog-item"}
              key={bookingClass.id}
            >
              <div className="class-catalog-number" aria-hidden="true">
                {String(index + 1).padStart(2, "0")}
              </div>
              <div className="class-catalog-content">
                <div className="class-catalog-title-row">
                  <h3>{bookingClass.name}</h3>
                  <strong>{formatKRW(bookingClass.price)}</strong>
                </div>
                <p className="class-catalog-description">
                  {bookingClass.description || "해피갤러리에서 재료와 과정을 차근차근 안내하는 공예 수업입니다."}
                </p>
                <Link to={`/classes/${bookingClass.id}`} className="my-inline-link small">
                  상세와 후기 보기 →
                </Link>
                <dl className="class-catalog-meta">
                  <div><dt>소요시간</dt><dd>{bookingClass.durationMin}분</dd></div>
                  <div>
                    <dt>8회권</dt>
                    <dd>
                      {bookingClass.passEligible && bookingClass.category !== "PERFUME"
                        ? "사용 가능"
                        : "사용 불가"}
                    </dd>
                  </div>
                  {bookingClass.targetAudience && (
                    <div><dt>추천 대상</dt><dd>{bookingClass.targetAudience}</dd></div>
                  )}
                  {bookingClass.preparationInfo && (
                    <div><dt>준비물</dt><dd>{bookingClass.preparationInfo}</dd></div>
                  )}
                </dl>
              </div>
              {bookingClass.imageUrl && (
                <figure className="class-catalog-media">
                  <img src={bookingClass.imageUrl} alt={`${bookingClass.name} 수업`} loading="lazy" />
                </figure>
              )}
              <Link
                className="class-catalog-book-link"
                to={`/bookings/new?classId=${bookingClass.id}`}
                aria-label={`${bookingClass.name} 예약하기`}
              >
                날짜 선택하기 <span aria-hidden="true">→</span>
              </Link>
            </article>
          ))}
        </div>

        <section className="class-followup-links" aria-label="다른 수업 방식">
          <Link to="/passes/purchase">
            <span>꾸준히 배우고 싶다면</span>
            <strong>정규 공예 8회권</strong>
            <span aria-hidden="true">↗</span>
          </Link>
          <Link to="/group-classes">
            <span>학교·기관·모임과 함께</span>
            <strong>단체·기관 수업</strong>
            <span aria-hidden="true">↗</span>
          </Link>
        </section>
      </Container>
    </>
  );
}
