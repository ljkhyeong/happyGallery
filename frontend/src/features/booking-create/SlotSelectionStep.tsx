import { useEffect, useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Form, Row, Col, ListGroup, Badge } from "react-bootstrap";
import { fetchClasses, fetchUpcomingSlots } from "./api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatDate, formatDateTime } from "@/shared/lib";
import type { ClassResponse, PublicSlotResponse } from "@/shared/types";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";

interface Props {
  initialClassId?: number | null;
  selectedSlotId: number | null;
  onSelect: (slot: PublicSlotResponse) => void;
  onDeselect?: () => void;
  onClassChange?: (bookingClass: ClassResponse | null) => void;
}

const UPCOMING_DAYS = 14;

export function SlotSelectionStep({
  initialClassId,
  selectedSlotId,
  onSelect,
  onDeselect,
  onClassChange,
}: Props) {
  const appliedInitialClassId = useRef<number | null | undefined>(undefined);
  const [classId, setClassId] = useState(() => initialClassId ? String(initialClassId) : "");
  const [date, setDate] = useState("");

  const { data: classes, isLoading: classesLoading, error: classesError } = useQuery({
    queryKey: ["classes"],
    queryFn: fetchClasses,
    staleTime: REFERENCE_DATA_STALE_TIME,
  });

  const classIdNum = Number(classId);
  const selectedClass = classes?.find((bookingClass) => bookingClass.id === classIdNum) ?? null;

  useEffect(() => {
    if (classes === undefined || appliedInitialClassId.current === initialClassId) {
      return;
    }
    appliedInitialClassId.current = initialClassId;
    const initialClass = classes.find((bookingClass) => bookingClass.id === initialClassId) ?? null;
    setClassId(initialClass ? String(initialClass.id) : "");
    setDate("");
    onClassChange?.(initialClass);
    onDeselect?.();
  }, [classes, initialClassId, onClassChange, onDeselect]);

  const { data: upcomingSlots, isLoading: slotsLoading, error: slotsError } = useQuery({
    queryKey: ["upcoming-slots", selectedClass?.id ?? 0, UPCOMING_DAYS],
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
      onDeselect?.();
    }
  }, [activeDate, date, onDeselect]);

  return (
    <div>
      <h6 className="mb-3">2. 클래스 / 날짜 / 시간 선택</h6>

      <Row className="g-2 mb-3">
        <Col xs={12} sm={6}>
          <Form.Group controlId="booking-class-select">
            <Form.Label>클래스</Form.Label>
            {classesLoading ? (
              <LoadingSpinner text="클래스 로딩..." />
            ) : (
              <Form.Select value={classId} onChange={(e) => {
                const nextId = Number(e.target.value);
                setClassId(e.target.value);
                setDate("");
                onClassChange?.(classes?.find((bookingClass) => bookingClass.id === nextId) ?? null);
                onDeselect?.();
              }}>
                <option value="">선택하세요</option>
                {classes?.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name} ({c.category}, {c.durationMin}분)
                  </option>
                ))}
              </Form.Select>
            )}
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
              앞으로 {UPCOMING_DAYS}일 안에 예약 가능한 날짜만 표시됩니다.
            </Form.Text>
          </Form.Group>
        </Col>
      </Row>

      <ErrorAlert error={classesError} />

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

      <ErrorAlert error={slotsError} />

      {slotsLoading && <LoadingSpinner text="슬롯 조회 중..." />}

      {upcomingSlots && upcomingSlots.length === 0 && (
        <EmptyState message={`앞으로 ${UPCOMING_DAYS}일 안에 예약 가능한 일정이 없습니다.`} />
      )}

      {slots && slots.length > 0 && (
        <ListGroup>
          {slots.map((slot) => (
            <ListGroup.Item
              key={slot.id}
              data-slot-id={slot.id}
              action
              active={selectedSlotId === slot.id}
              onClick={() => onSelect(slot)}
              className="d-flex justify-content-between align-items-center"
            >
              <span>
                {formatDateTime(slot.startAt)} ~ {formatDateTime(slot.endAt)}
              </span>
              <Badge bg={slot.remainingCapacity <= 2 ? "warning" : "info"} className="badge-status">
                잔여 {slot.remainingCapacity}명
              </Badge>
            </ListGroup.Item>
          ))}
        </ListGroup>
      )}

      {selectedSlotId != null && <WorkshopVisitInfo compact />}
    </div>
  );
}
