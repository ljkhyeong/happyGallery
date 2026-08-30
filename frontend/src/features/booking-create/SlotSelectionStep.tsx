import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Form, Row, Col, ListGroup, Badge } from "react-bootstrap";
import { fetchClasses, fetchUpcomingSlots } from "./api";
import { queryKeys } from "@/shared/api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { CLASS_CATEGORY_OPTIONS, formatDate, formatDateTime } from "@/shared/lib";
import type { ClassResponse, PublicSlotResponse } from "@/shared/types";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";
import { WorkshopInquiryLink } from "@/features/workshop/WorkshopInquiryLink";
import { VacancyAlertButton } from "./VacancyAlertButton";

interface Props {
  initialClassId?: number | null;
  selectedSlot: PublicSlotResponse | null;
  onSelect: (slot: PublicSlotResponse) => void;
  onDeselect?: () => void;
  onClassChange?: (bookingClass: ClassResponse | null) => void;
}

const UPCOMING_DAYS = 14;

export function SlotSelectionStep({
  initialClassId,
  selectedSlot,
  onSelect,
  onDeselect,
  onClassChange,
}: Props) {
  const appliedInitialClassId = useRef<number | null | undefined>(undefined);
  const appliedInitialClass = useRef<ClassResponse | null | undefined>(undefined);
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
    queryKey: queryKeys.slotAvailability.upcoming.byClass(
      selectedClass?.id ?? 0,
      UPCOMING_DAYS,
    ),
    queryFn: () => fetchUpcomingSlots(selectedClass!.id, UPCOMING_DAYS),
    enabled: selectedClass !== null,
  });

  const availableDates = useMemo(
    () => Array.from(
      new Set(upcomingSlots?.map((slot) => slot.startAt.slice(0, 10)) ?? []),
    ).sort(),
    [upcomingSlots],
  );
  const activeDate = availableDates.includes(date) ? date : (availableDates[0] ?? "");
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
    if (selectedSlot === null || upcomingSlots === undefined) return;
    const refreshedSlot = upcomingSlots.find((slot) => slot.id === selectedSlot.id);
    if (!refreshedSlot) {
      onDeselect?.();
    } else if (refreshedSlot !== selectedSlot) {
      onSelect(refreshedSlot);
    }
  }, [onDeselect, onSelect, selectedSlot, upcomingSlots]);

  return (
    <div>
      <h6 className="mb-3">2. 클래스 / 날짜 / 시간 선택</h6>

      {classesLoading && <LoadingSpinner text="클래스를 불러오는 중입니다..." />}
      <ErrorAlert
        error={classesError}
        onRetry={() => { void refetchClasses(); }}
        retrying={classesFetching}
      />

      {classes !== undefined && (
        <Row className="g-2 mb-3">
          <Col xs={12} sm={6}>
            <Form.Group controlId="booking-class-select">
              <Form.Label>클래스</Form.Label>
              <Form.Select value={classId} onChange={(e) => {
                const nextId = Number(e.target.value);
                setClassId(e.target.value);
                setDate("");
                setInquiryDate("");
                onClassChange?.(classes?.find((bookingClass) => bookingClass.id === nextId) ?? null);
                onDeselect?.();
              }}>
                <option value="">선택하세요</option>
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
          </Col>
          <Col xs={12} sm={6}>
            <Form.Group controlId="booking-date-input">
              <Form.Label>날짜</Form.Label>
              <Form.Select
                value={activeDate}
                onChange={(e) => { setDate(e.target.value); onDeselect?.(); }}
                disabled={!selectedClass || slotsLoading || availableDates.length === 0}
              >
                <option value="" disabled>
                  {!selectedClass
                    ? "클래스를 먼저 선택하세요"
                    : slotsLoading
                      ? "예약 가능일 조회 중..."
                      : slotsError
                        ? "예약 가능일을 다시 조회해 주세요"
                        : availableDates.length > 0
                          ? "예약 가능한 날짜를 선택하세요"
                          : "예약 가능한 날짜가 없습니다"}
                </option>
                {availableDates.map((availableDate) => (
                  <option key={availableDate} value={availableDate}>
                    {formatDate(availableDate)}
                  </option>
                ))}
              </Form.Select>
              <Form.Text className="text-muted">
                앞으로 {UPCOMING_DAYS}일 안에 예약 가능하거나 빈자리 알림을 신청할 수 있는 날짜를 표시합니다.
              </Form.Text>
            </Form.Group>
          </Col>
        </Row>
      )}

      {selectedClass && (
        <section className="booking-class-detail mb-3">
          {selectedClass.imageUrl && (
            <img src={selectedClass.imageUrl} alt={`${selectedClass.name} 대표 이미지`} />
          )}
          {selectedClass.description && <p>{selectedClass.description}</p>}
          <div className="d-flex flex-wrap gap-4 small">
            {selectedClass.targetAudience && (
              <div>
                <strong className="d-block mb-1">대상</strong>
                <span>{selectedClass.targetAudience}</span>
              </div>
            )}
            {selectedClass.preparationInfo && (
              <div>
                <strong className="d-block mb-1">준비물</strong>
                <span>{selectedClass.preparationInfo}</span>
              </div>
            )}
          </div>
        </section>
      )}

      <ErrorAlert
        error={slotsError}
        onRetry={() => { void refetchSlots(); }}
        retrying={slotsFetching}
      />

      {slotsLoading && <LoadingSpinner text="예약 가능한 시간을 불러오는 중입니다..." />}

      {!slotsError && upcomingSlots && upcomingSlots.length === 0 && (
        <div>
          <EmptyState message={`앞으로 ${UPCOMING_DAYS}일 안에 예약 가능한 일정이 없습니다.`} />
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

      {slots && slots.length > 0 && (
        <ListGroup>
          {slots.map((slot) => slot.remainingCapacity === 0 ? (
            <ListGroup.Item
              key={slot.id}
              data-slot-id={slot.id}
              className="d-flex flex-wrap justify-content-between align-items-center gap-2"
            >
              <span>
                {formatDateTime(slot.startAt)} ~ {formatDateTime(slot.endAt)}
              </span>
              <span className="d-flex align-items-center gap-2">
                <Badge bg="secondary" className="badge-status">만석</Badge>
                <VacancyAlertButton slotId={slot.id} />
              </span>
            </ListGroup.Item>
          ) : (
            <ListGroup.Item
              key={slot.id}
              data-slot-id={slot.id}
              action
              active={selectedSlot?.id === slot.id}
              onClick={() => onSelect(slot)}
              className="d-flex justify-content-between align-items-center"
            >
              <span>
                {formatDateTime(slot.startAt)} ~ {formatDateTime(slot.endAt)}
              </span>
              <Badge bg={slot.remainingCapacity <= 2 ? "warning" : "info"} className="badge-status">
                {slot.remainingCapacity}명 예약 가능
              </Badge>
            </ListGroup.Item>
          ))}
        </ListGroup>
      )}

      {selectedSlot !== null && <WorkshopVisitInfo compact />}
    </div>
  );
}
