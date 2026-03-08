import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Form, Row, Col, ListGroup, Badge } from "react-bootstrap";
import { fetchClasses, fetchAvailableSlots } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState } from "@/shared/ui";
import { formatDateTime } from "@/shared/lib";
import type { PublicSlotResponse } from "@/shared/types";

interface Props {
  selectedSlotId: number | null;
  onSelect: (slot: PublicSlotResponse) => void;
}

export function SlotSelectionStep({ selectedSlotId, onSelect }: Props) {
  const [classId, setClassId] = useState("");
  const [date, setDate] = useState("");

  const { data: classes, isLoading: classesLoading } = useQuery({
    queryKey: ["classes"],
    queryFn: fetchClasses,
  });

  const classIdNum = Number(classId);
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
          <Form.Group>
            <Form.Label>클래스</Form.Label>
            {classesLoading ? (
              <LoadingSpinner text="클래스 로딩..." />
            ) : (
              <Form.Select value={classId} onChange={(e) => setClassId(e.target.value)}>
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
          <Form.Group>
            <Form.Label>날짜</Form.Label>
            <Form.Control
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          </Form.Group>
        </Col>
      </Row>

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
    </div>
  );
}
