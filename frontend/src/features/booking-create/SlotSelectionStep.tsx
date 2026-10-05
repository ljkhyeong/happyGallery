import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Alert, Form, ListGroup } from "react-bootstrap";
import { fetchClasses } from "./api";
import { slotDayParts, UPCOMING_SLOT_DAYS, upcomingSlotsQuery } from "./upcomingSlots";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { CheckoutPanel, LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import {
  CLASS_CATEGORY_OPTIONS,
  classImageSrc,
  formatDate,
  formatDateTime,
  formatTime,
  getClassCategoryLabel,
  isPerfumeClassCategory,
} from "@/shared/lib";
import type { ClassResponse, PublicSlotResponse } from "@/shared/types";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
import { WorkshopInquiryLink } from "@/features/workshop/WorkshopInquiryLink";
import { VacancyAlertButton } from "./VacancyAlertButton";

/** 위에서 고른 날짜의 슬롯만 보여 주므로 화면에는 시간만 표시한다. 날짜는 보조기기용 숨김 텍스트로 붙이고, 다음 날 끝나는 수업만 종료 날짜를 보인다. */
function SlotTime({ slot }: { slot: Pick<PublicSlotResponse, "startAt" | "endAt"> }) {
  const sameDay = slot.endAt.slice(0, 10) === slot.startAt.slice(0, 10);
  return (
    <span className="booking-slot-time">
      <span className="visually-hidden">{formatDate(slot.startAt)} </span>
      <b>{formatTime(slot.startAt)}</b>
      <small> ~ {sameDay ? formatTime(slot.endAt) : formatDateTime(slot.endAt)}</small>
    </span>
  );
}

interface Props {
  initialClassId?: number | null;
  initialSlotId?: number | null;
  /** 빈자리 알림 링크는 날짜만 펼치고, 사용자가 이미 시간을 고른 홈 빠른 예약은 남은 자리가 있으면 바로 선택한다. */
  selectInitialSlot?: boolean;
  selectedSlot: PublicSlotResponse | null;
  onSelect: (slot: PublicSlotResponse) => void;
  onDeselect?: () => void;
  onClassChange?: (bookingClass: ClassResponse | null) => void;
}


export function SlotSelectionStep({
  initialClassId,
  initialSlotId,
  selectInitialSlot = false,
  selectedSlot,
  onSelect,
  onDeselect,
  onClassChange,
}: Props) {
  const appliedInitialClassId = useRef<number | null | undefined>(undefined);
  const appliedInitialClass = useRef<ClassResponse | null | undefined>(undefined);
  const appliedInitialSlotId = useRef<number | null | undefined>(undefined);
  const [classId, setClassId] = useState(() => initialClassId ? String(initialClassId) : "");
  const [date, setDate] = useState(() => selectedSlot?.startAt.slice(0, 10) ?? "");
  const [inquiryDate, setInquiryDate] = useState("");

  const {
    data: classes,
    isLoading: classesLoading,
    isFetching: classesFetching,
    error: classesError,
    refetch: refetchClasses,
  } = useQuery({
    queryKey: ["classes"],
    queryFn: fetchClasses,
    staleTime: REFERENCE_DATA_STALE_TIME,
  });

  const classIdNum = Number(classId);
  const selectedClass = classes?.find((bookingClass) => bookingClass.id === classIdNum) ?? null;

  useEffect(() => {
    if (classes === undefined) return;
    const initialClass = classes.find((bookingClass) => bookingClass.id === initialClassId) ?? null;
    if (
      appliedInitialClassId.current === initialClassId
      && appliedInitialClass.current === initialClass
    ) {
      return;
    }
    appliedInitialClassId.current = initialClassId;
    appliedInitialClass.current = initialClass;
    const keepsSelectedSlot = selectedSlot?.classId === initialClass?.id;
    setClassId(initialClass ? String(initialClass.id) : "");
    setDate(keepsSelectedSlot ? selectedSlot?.startAt.slice(0, 10) ?? "" : "");
    setInquiryDate("");
    onClassChange?.(initialClass);
    if (!keepsSelectedSlot) {
      onDeselect?.();
    }
  }, [classes, initialClassId, onClassChange, onDeselect, selectedSlot]);

  const {
    data: upcomingSlots,
    isLoading: slotsLoading,
    isFetching: slotsFetching,
    error: slotsError,
    refetch: refetchSlots,
  } = useQuery({
    ...upcomingSlotsQuery(selectedClass?.id ?? 0),
    enabled: selectedClass !== null,
    refetchOnMount: initialSlotId != null ? "always" : true,
  });

  const availableDates = useMemo(
    () => Array.from(
      new Set(upcomingSlots?.map((slot) => slot.startAt.slice(0, 10)) ?? []),
    ).sort(),
    [upcomingSlots],
  );
  const activeDate = availableDates.includes(date) ? date : (availableDates[0] ?? "");
  const openDates = useMemo(
    () => new Set(upcomingSlots?.filter((slot) => slot.remainingCapacity > 0)
      .map((slot) => slot.startAt.slice(0, 10)) ?? []),
    [upcomingSlots],
  );
  const monthLabel = Array.from(new Set(availableDates.map((value) => `${slotDayParts(value).month}월`)))
    .join(" · ");
  const slots = useMemo(
    () => upcomingSlots?.filter((slot) => slot.startAt.startsWith(activeDate)),
    [activeDate, upcomingSlots],
  );

  useEffect(() => {
    if (activeDate !== date) {
      setDate(activeDate);
    }
  }, [activeDate, date]);

  useEffect(() => {
    if (initialSlotId == null || upcomingSlots === undefined || slotsError || slotsFetching
      || selectedClass?.id !== initialClassId
      || appliedInitialSlotId.current === initialSlotId) return;
    appliedInitialSlotId.current = initialSlotId;
    const initialSlot = upcomingSlots.find((slot) => slot.id === initialSlotId);
    if (initialSlot && selectedSlot === null) {
      setDate(initialSlot.startAt.slice(0, 10));
      if (selectInitialSlot && initialSlot.remainingCapacity > 0) onSelect(initialSlot);
    }
  }, [initialClassId, initialSlotId, onSelect, selectInitialSlot, selectedClass, selectedSlot, slotsError, slotsFetching, upcomingSlots]);

  useEffect(() => {
    if (selectedSlot === null || upcomingSlots === undefined) return;
    const refreshedSlot = upcomingSlots.find((slot) => slot.id === selectedSlot.id);
    if (!refreshedSlot) {
      onDeselect?.();
    } else if (refreshedSlot !== selectedSlot) {
      onSelect(refreshedSlot);
    }
  }, [onDeselect, onSelect, selectedSlot, upcomingSlots]);

  const activeDay = activeDate ? slotDayParts(activeDate) : null;

  return (
    <>
      <CheckoutPanel step={1} title="수업">
        {classesLoading && <LoadingSpinner text="클래스를 불러오는 중입니다..." />}
        <ErrorAlert
          error={classesError}
          onRetry={() => { void refetchClasses(); }}
          retrying={classesFetching}
        />
        {classes !== undefined && initialClassId != null
          && !classes.some((bookingClass) => bookingClass.id === initialClassId) && (
          <Alert variant="info">
            이 수업은 현재 예약할 수 없습니다. 다른 수업을 선택해 주세요.
          </Alert>
        )}

        {classes !== undefined && (
          <Form.Group controlId="booking-class-select">
            <Form.Label visuallyHidden>클래스</Form.Label>
            <Form.Select value={classId} onChange={(e) => {
              const nextId = Number(e.target.value);
              setClassId(e.target.value);
              setDate("");
              setInquiryDate("");
              onClassChange?.(classes?.find((bookingClass) => bookingClass.id === nextId) ?? null);
              onDeselect?.();
            }}>
              <option value="">수업을 선택하세요</option>
              {classes?.map((c) => {
                const categoryLabel = CLASS_CATEGORY_OPTIONS.find(
                  ({ code }) => code === c.category.trim().toUpperCase(),
                )?.label;
                return (
                  <option key={c.id} value={c.id}>
                    {c.name} ({categoryLabel ? `${categoryLabel}, ` : ""}{c.durationMin}분)
                  </option>
                );
              })}
            </Form.Select>
          </Form.Group>
        )}

        {selectedClass && (
          <div className="booking-class-detail">
            <img src={classImageSrc(selectedClass)} alt="" />
            <div>
              <p className="booking-class-meta">
                {getClassCategoryLabel(selectedClass.category)} · {selectedClass.durationMin}분
                {selectedClass.passEligible && !isPerfumeClassCategory(selectedClass.category) && " · 4회권 사용 가능"}
              </p>
              {selectedClass.description && <p>{selectedClass.description}</p>}
              {(selectedClass.targetAudience || selectedClass.preparationInfo) && (
                <dl>
                  {selectedClass.targetAudience && (
                    <div>
                      <dt>추천 대상</dt>
                      <dd>{selectedClass.targetAudience}</dd>
                    </div>
                  )}
                  {selectedClass.preparationInfo && (
                    <div>
                      <dt>준비물</dt>
                      <dd>{selectedClass.preparationInfo}</dd>
                    </div>
                  )}
                </dl>
              )}
            </div>
          </div>
        )}
      </CheckoutPanel>

      <CheckoutPanel step={2} title="날짜와 시간" meta={`앞으로 ${UPCOMING_SLOT_DAYS}일`}>
        {!selectedClass && classes !== undefined && (
          <p className="checkout-panel-empty">수업을 먼저 선택해 주세요.</p>
        )}

        <ErrorAlert
          error={slotsError}
          onRetry={() => { void refetchSlots(); }}
          retrying={slotsFetching}
        />

        {slotsLoading && <LoadingSpinner text="예약 가능한 시간을 불러오는 중입니다..." />}

        {initialSlotId != null && selectedClass && upcomingSlots !== undefined && !slotsError && !slotsFetching
          && !upcomingSlots.some((slot) => slot.id === initialSlotId) && (
          <Alert variant="info">
            이 일정은 현재 예약할 수 없습니다. 다른 날짜나 시간을 선택해 주세요.
          </Alert>
        )}

        {!slotsError && upcomingSlots && upcomingSlots.length === 0 && (
          <div>
            <EmptyState message={`앞으로 ${UPCOMING_SLOT_DAYS}일 안에 예약 가능한 일정이 없습니다.`} />
            <Form.Group controlId="booking-inquiry-date" className="mt-3">
              <Form.Label>문의할 희망일</Form.Label>
              <Form.Control
                type="date"
                value={inquiryDate}
                min={new Date().toLocaleDateString("sv-SE", { timeZone: "Asia/Seoul" })}
                onChange={(event) => setInquiryDate(event.target.value)}
              />
            </Form.Group>
            {selectedClass && (
              <WorkshopInquiryLink
                className={selectedClass.name}
                desiredDate={inquiryDate}
              />
            )}
          </div>
        )}

        {availableDates.length > 0 && (
          <div className="booking-date-step">
            <p className="booking-step-label">{monthLabel}</p>
            <div className="booking-date-chips" role="group" aria-label="날짜">
              {availableDates.map((value) => {
                const { month, day, weekday } = slotDayParts(value);
                const open = openDates.has(value);
                return (
                  <button
                    key={value}
                    type="button"
                    data-booking-date={value}
                    aria-pressed={value === activeDate}
                    aria-label={`${month}월 ${day}일 ${weekday}요일${open ? "" : " 마감"}`}
                    className={open ? undefined : "is-full"}
                    onClick={() => { setDate(value); onDeselect?.(); }}
                  >
                    <span>{weekday}</span>
                    <b>{day}</b>
                    {!open && <small>마감</small>}
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {slots && slots.length > 0 && activeDay && (
          <>
            <p className="booking-step-label">
              {activeDay.month}월 {activeDay.day}일 {activeDay.weekday}요일 · 남은 자리
            </p>
            <ListGroup className="booking-slot-list">
              {slots.map((slot) => slot.remainingCapacity === 0 ? (
                <ListGroup.Item key={slot.id} data-slot-id={slot.id} className="is-full">
                  {slot.id === initialSlotId && <small className="booking-slot-flag">알림 신청한 일정</small>}
                  <SlotTime slot={slot} />
                  <span className="booking-slot-seats">만석</span>
                  <VacancyAlertButton slotId={slot.id} />
                </ListGroup.Item>
              ) : (
                <ListGroup.Item
                  key={slot.id}
                  data-slot-id={slot.id}
                  action
                  active={selectedSlot?.id === slot.id}
                  onClick={() => onSelect(slot)}
                  className={slot.remainingCapacity <= 2 ? "is-few" : undefined}
                >
                  {slot.id === initialSlotId && <small className="booking-slot-flag">알림 신청한 일정</small>}
                  <SlotTime slot={slot} />
                  <span className="booking-slot-seats">{slot.remainingCapacity}명 예약 가능</span>
                </ListGroup.Item>
              ))}
            </ListGroup>
          </>
        )}

        {selectedSlot !== null && <WorkshopVisitInfo compact />}
      </CheckoutPanel>
    </>
  );
}
