import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Form, Row, Col, ListGroup, Badge } from "react-bootstrap";
import { fetchClasses, fetchAvailableSlots } from "./api";
import { REFERENCE_DATA_STALE_TIME } from "@/shared/api/staleTimes";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import type { ClassResponse, PublicSlotResponse } from "@/shared/types";
import { WorkshopVisitInfo } from "@/features/workshop/WorkshopVisitInfo";

interface Props {
  selectedSlotId: number | null;
  onSelect: (slot: PublicSlotResponse) => void;
  onDeselect?: () => void;
  onClassChange?: (bookingClass: ClassResponse | null) => void;
}

export function SlotSelectionStep({ selectedSlotId, onSelect, onDeselect, onClassChange }: Props) {
  const [classId, setClassId] = useState("");
  const [date, setDate] = useState("");

  const { data: classes, isLoading: classesLoading } = useQuery({
    queryKey: ["classes"],
    queryFn: fetchClasses,
    staleTime: REFERENCE_DATA_STALE_TIME,
  });

  const classIdNum = Number(classId);
  const selectedClass = classes?.find((bookingClass) => bookingClass.id === classIdNum) ?? null;
  const { data: slots, isLoading: slotsLoading, error: slotsError } = useQuery({
    queryKey: ["slots", classIdNum, date],
    queryFn: () => fetchAvailableSlots(classIdNum, date),
    enabled: classIdNum > 0 && date.length > 0,
  });

  return (
    <div>
      <h6 className="mb-3">2. 클래스 / 슬롯 선택</h6>

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
            <Form.Control
              type="date"
              value={date}
              onChange={(e) => { setDate(e.target.value); onDeselect?.(); }}
            />
          </Form.Group>
        </Col>
      </Row>

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

      {slots && slots.length === 0 && (
        <EmptyState message="예약 가능한 슬롯이 없습니다." />
      )}

      {slots && slots.length > 0 && (
        <ListGroup>
          {slots.map((slot) => (
            <ListGroup.Item
              key={slot.id}
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
