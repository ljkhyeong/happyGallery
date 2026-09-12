import type { CSSProperties } from "react";
import { Button, Container, Form } from "react-bootstrap";
import { Link, useSearchParams } from "react-router";
import leatherClass from "@/assets/happygallery/leather-class.jpg";
import { fetchClasses } from "@/features/booking-create/api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { formatKRW } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { LinkButton } from "@/shared/ui/LinkButton";
import type { ClassResponse } from "@/generated/api/booking";
import { queryKeys, useLoaderBackedQuery } from "@/shared/api";

export function ClassListPage({ initialClasses }: { initialClasses: ClassResponse[] }) {
  const [searchParams, setSearchParams] = useSearchParams();
  const passEligibleOnly = searchParams.get("passEligible") === "true";
  const {
    data: classes,
    error: classesError,
    isLoading: classesLoading,
  } = useLoaderBackedQuery({
    queryKey: queryKeys.catalog.classes,
    queryFn: fetchClasses,
    staleTime: REFERENCE_DATA_STALE_TIME,
  }, initialClasses);
  const visibleClasses = passEligibleOnly
    ? classes?.filter((bookingClass) => bookingClass.passEligible && bookingClass.category !== "PERFUME")
    : classes;

  function updatePassFilter(enabled: boolean) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current);
      if (enabled) next.set("passEligible", "true");
      else next.delete("passEligible");
      return next;
    }, { preventScrollReset: true });
  }

  const heroStyle = { "--hg-class-hero-image": `url(${leatherClass})` } as CSSProperties;

  return (
    <>
      <section className="class-catalog-hero" style={heroStyle}>
        <Container>
          <div className="class-catalog-hero-copy">
            <h1>공예 클래스</h1>
            <p>수업별 소요시간과 가격을 확인하고 원하는 날짜를 예약하세요.</p>
          </div>
        </Container>
      </section>

      <Container className="page-container class-catalog-page">
        <header className="class-catalog-header">
          <h2>수업 선택</h2>
          <LinkButton to="/group-classes" variant="outline-dark">단체수업 문의</LinkButton>
        </header>

        <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
          <Form.Check
            id="class-pass-filter"
            label="이용권 사용 가능 수업만"
            checked={passEligibleOnly}
            onChange={(event) => updatePassFilter(event.target.checked)}
          />
          {visibleClasses && <span className="small text-muted-soft" role="status">수업 {visibleClasses.length}개</span>}
        </div>

        {classesLoading && <LoadingSpinner text="클래스를 불러오는 중입니다" />}
        <ErrorAlert error={classesError} />
        {visibleClasses?.length === 0 && (
          <div className="text-center">
            <EmptyState message={passEligibleOnly
              ? "이용권을 사용할 수 있는 수업이 없습니다."
              : "예약 가능한 클래스를 준비하고 있습니다."} />
            {passEligibleOnly && (
              <Button variant="outline-dark" onClick={() => updatePassFilter(false)}>전체 수업 보기</Button>
            )}
          </div>
        )}

        <div className="class-catalog-list">
          {visibleClasses?.map((bookingClass) => (
            <article
              className={bookingClass.imageUrl ? "class-catalog-item has-media" : "class-catalog-item"}
              key={bookingClass.id}
            >
              {bookingClass.imageUrl && (
                <figure className="class-catalog-media">
                  <img src={bookingClass.imageUrl} alt={`${bookingClass.name} 수업`} loading="lazy" />
                </figure>
              )}
              <div className="class-catalog-content">
                <div className="class-catalog-title-row">
                  <h3>{bookingClass.name}</h3>
                  <strong>{formatKRW(bookingClass.price)}</strong>
                </div>
                <p className="class-catalog-description">
                  {bookingClass.description || "해피갤러리에서 재료와 과정을 차근차근 안내하는 공예 수업입니다."}
                </p>
                <dl className="class-catalog-meta">
                  <div><dt>소요시간</dt><dd>{bookingClass.durationMin}분</dd></div>
                  <div><dt>회차 정원</dt><dd>{bookingClass.capacity}명</dd></div>
                  <div>
                    <dt>이용권</dt>
                    <dd>
                      {bookingClass.passEligible && bookingClass.category !== "PERFUME"
                        ? "사용 가능"
                        : "사용 불가"}
                    </dd>
                  </div>
                  {bookingClass.targetAudience && (
                    <div className="class-catalog-meta-wide"><dt>추천 대상</dt><dd>{bookingClass.targetAudience}</dd></div>
                  )}
                  {bookingClass.preparationInfo && (
                    <div className="class-catalog-meta-wide"><dt>준비물</dt><dd>{bookingClass.preparationInfo}</dd></div>
                  )}
                </dl>
                <div className="class-catalog-actions">
                  <LinkButton
                    to={`/bookings/new?classId=${bookingClass.id}`}
                    variant="dark"
                    aria-label={`${bookingClass.name} 예약하기`}
                  >
                    날짜 선택하기 <span aria-hidden="true">→</span>
                  </LinkButton>
                  <Link to={`/classes/${bookingClass.id}`} className="store-section-link">
                    상세와 후기 보기 <span aria-hidden="true">→</span>
                  </Link>
                </div>
              </div>
            </article>
          ))}
        </div>

        <section className="class-followup-links" aria-label="다른 수업 방식">
          <Link to="/passes/purchase">
            <span>꾸준히 배우고 싶다면</span>
            <strong>정규 공예 4회권</strong>
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
