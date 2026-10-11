import { useQueries } from "@tanstack/react-query";
import { Button, Container, Form } from "react-bootstrap";
import { Link, useSearchParams } from "react-router";
import { ClassNextSlot } from "@/features/booking-create/ClassNextSlot";
import {
  bookingCreateHref,
  classAcceptsPass,
  hasOpenSlotOn,
  publicClassesQuery,
  SLOT_WHEN_LABEL,
  type SlotWhen,
  slotWhenDates,
  upcomingSlotsQuery,
} from "@/features/booking-create/upcomingSlots";
import { classListHeading, readClassListFilters } from "@/features/booking-create/classListFilters";
import {
  CLASS_SITUATION_TAG_OPTIONS,
  type ClassSituationTagCode,
  classImageSrc,
  formatKRW,
  getClassCategoryLabel,
  getClassSituationTagLabel,
} from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";
import { LinkButton } from "@/shared/ui/LinkButton";
import type { ClassResponse } from "@/generated/api/booking";
import { useLoaderBackedQuery } from "@/shared/api";

const FILTER_PARAMS = ["passEligible", "tag", "when"] as const;

export function ClassListPage({ initialClasses }: { initialClasses: ClassResponse[] }) {
  const [searchParams, setSearchParams] = useSearchParams();
  const filters = readClassListFilters(searchParams);
  const { passEligibleOnly, tag, when } = filters;
  const heading = classListHeading(filters);
  const {
    data: classes,
    error: classesError,
  } = useLoaderBackedQuery(publicClassesQuery(), initialClasses);
  const slotResults = useQueries({
    queries: classes.map((bookingClass) => upcomingSlotsQuery(bookingClass.id)),
  });
  const slotResultByClassId = new Map(classes.map((bookingClass, index) => [bookingClass.id, slotResults[index]]));
  // 주말·오늘 조건은 일정이 모두 도착한 뒤에 걸러 목록이 줄어드는 깜빡임을 막는다.
  const whenPending = when !== null && slotResults.some((result) => result.isPending);
  const whenDates = when ? slotWhenDates(when) : [];
  const visibleClasses = whenPending ? undefined : classes.filter((bookingClass) =>
    (!passEligibleOnly || classAcceptsPass(bookingClass))
    && (!tag || bookingClass.situationTags.includes(tag))
    && (!when || hasOpenSlotOn(slotResultByClassId.get(bookingClass.id)?.data, whenDates)));
  const emptyMessage = when
    ? `${SLOT_WHEN_LABEL[when]} 예약 가능한 수업이 없습니다.`
    : tag
      ? `${getClassSituationTagLabel(tag)} 수업이 아직 없습니다.`
      : passEligibleOnly
        ? "이용권을 사용할 수 있는 수업이 없습니다."
        : "지금 예약할 수 있는 클래스가 없습니다.";
  const hasFilter = passEligibleOnly || tag !== null || when !== null;

  function updateParams(apply: (next: URLSearchParams) => void) {
    setSearchParams((current) => {
      const next = new URLSearchParams(current);
      apply(next);
      return next;
    }, { preventScrollReset: true });
  }

  function updatePassFilter(enabled: boolean) {
    updateParams((next) => {
      if (enabled) next.set("passEligible", "true");
      else next.delete("passEligible");
    });
  }

  /** 상황 조건은 하나만 고른다. 주말·오늘(일정)과 상황 태그는 서로를 지운다. */
  function selectSituation(situation: { tag: ClassSituationTagCode } | { when: SlotWhen } | null) {
    updateParams((next) => {
      next.delete("tag");
      next.delete("when");
      if (situation && "tag" in situation) next.set("tag", situation.tag);
      if (situation && "when" in situation) next.set("when", situation.when);
    });
  }

  function clearFilters() {
    updateParams((next) => FILTER_PARAMS.forEach((param) => next.delete(param)));
  }

  return (
    <Container className="page-container class-catalog-page">
      {heading.documentName && (
        <nav className="store-detail-breadcrumb" aria-label="경로">
          <Link to="/classes" className="store-detail-breadcrumb-link">공예 클래스</Link>
          <span aria-hidden="true">/</span>
          <span className="store-detail-breadcrumb-current" aria-current="page">{heading.title}</span>
        </nav>
      )}
      <header className="class-catalog-header">
        <h1>{heading.title}</h1>
        <LinkButton to="/group-classes#group-inquiry" variant="outline-dark">단체수업 문의</LinkButton>
      </header>

      <div className="class-situation-chips" role="group" aria-label="상황으로 수업 찾기">
        <button type="button" aria-pressed={!tag && !when} onClick={() => selectSituation(null)}>전체</button>
        {(Object.keys(SLOT_WHEN_LABEL) as SlotWhen[]).map((option) => (
          <button key={option} type="button" aria-pressed={when === option} onClick={() => selectSituation({ when: option })}>
            {SLOT_WHEN_LABEL[option]}
          </button>
        ))}
        {CLASS_SITUATION_TAG_OPTIONS.map(({ code, label }) => (
          <button key={code} type="button" aria-pressed={tag === code} onClick={() => selectSituation({ tag: code })}>
            {label}
          </button>
        ))}
      </div>

      <div className="class-catalog-toolbar">
        <Form.Check
          id="class-pass-filter"
          label="이용권 사용 가능 수업만"
          checked={passEligibleOnly}
          onChange={(event) => updatePassFilter(event.target.checked)}
        />
        {visibleClasses && <span className="small text-muted-soft" role="status">수업 {visibleClasses.length}개</span>}
      </div>

      {whenPending && <LoadingSpinner text={`${SLOT_WHEN_LABEL[when!]} 일정을 확인하는 중입니다`} />}
      <ErrorAlert error={classesError} />
      {visibleClasses?.length === 0 && (
        <div className="text-center pb-4">
          <EmptyState message={emptyMessage} />
          {hasFilter && (
            <Button variant="outline-dark" onClick={clearFilters}>전체 수업 보기</Button>
          )}
        </div>
      )}

      <div className="class-catalog-list">
        {visibleClasses?.map((bookingClass) => (
          <article className="class-catalog-item" key={bookingClass.id}>
            <figure className="class-catalog-media">
              <img
                src={classImageSrc(bookingClass)}
                alt={bookingClass.imageUrl ? `${bookingClass.name} 수업` : ""}
                loading="lazy"
              />
            </figure>
            <div className="class-catalog-content">
              {/* 4회권·상황 표시는 같은 조건의 목록으로 이어진다. */}
              <div className="class-catalog-labels">
                <span>{getClassCategoryLabel(bookingClass.category)}</span>
                {classAcceptsPass(bookingClass) && (
                  <Link to="/classes?passEligible=true">4회권 사용 가능</Link>
                )}
                {bookingClass.situationTags.map((situationTag) => (
                  <Link key={situationTag} to={`/classes?${new URLSearchParams({ tag: situationTag })}`}>
                    {getClassSituationTagLabel(situationTag)}
                  </Link>
                ))}
              </div>
              <div className="class-catalog-title-row">
                <h2>{bookingClass.name}</h2>
                <strong>{formatKRW(bookingClass.price)}</strong>
              </div>
              {bookingClass.description && (
                <p className="class-catalog-description">{bookingClass.description}</p>
              )}
              <dl className="class-catalog-meta">
                <div><dt>소요시간</dt><dd>{bookingClass.durationMin}분</dd></div>
                <div><dt>회차 정원</dt><dd>{bookingClass.capacity}명</dd></div>
                {bookingClass.targetAudience && (
                  <div className="class-catalog-meta-wide"><dt>추천 대상</dt><dd>{bookingClass.targetAudience}</dd></div>
                )}
                {bookingClass.preparationInfo && (
                  <div className="class-catalog-meta-wide"><dt>준비물</dt><dd>{bookingClass.preparationInfo}</dd></div>
                )}
              </dl>
              <ClassNextSlot result={slotResultByClassId.get(bookingClass.id)} className="class-catalog-next" />
              <div className="class-catalog-actions">
                <LinkButton
                  to={bookingCreateHref(bookingClass.id)}
                  variant="dark"
                  aria-label={`${bookingClass.name} 예약하기`}
                >
                  날짜 선택하기
                </LinkButton>
                <Link to={`/classes/${bookingClass.id}`} className="store-section-link">
                  상세와 후기 보기
                </Link>
              </div>
            </div>
          </article>
        ))}
      </div>

      <section className="class-followup-links" aria-label="다른 수업 방식">
        <Link to="/passes/purchase">
          <strong>정규 공예 4회권</strong>
          <span>정규 수업을 원하는 날짜에 네 번 예약합니다.</span>
        </Link>
        <Link to="/group-classes">
          <strong>단체·출강 수업</strong>
          <span>학교·기관·모임에 맞춰 수업을 엽니다.</span>
        </Link>
      </section>
    </Container>
  );
}
