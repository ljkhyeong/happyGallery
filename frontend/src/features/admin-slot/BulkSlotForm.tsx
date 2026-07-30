import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Badge, Button, Col, Form, Row, Table } from "react-bootstrap";
import { createBulkSlots, fetchClasses, previewBulkSlots } from "./api";
import { invalidateSlotAvailability, queryKeys } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime } from "@/shared/lib";
import { ErrorAlert, LoadingSpinner, useToast } from "@/shared/ui";
import type { BulkSlotRequest, BulkSlotResponse, BulkSlotStatus } from "@/shared/types";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

const WEEKDAYS = [
  ["MONDAY", "월"],
  ["TUESDAY", "화"],
  ["WEDNESDAY", "수"],
  ["THURSDAY", "목"],
  ["FRIDAY", "금"],
  ["SATURDAY", "토"],
  ["SUNDAY", "일"],
] as const;

const STATUS_VIEW: Record<BulkSlotStatus, { label: string; variant: string }> = {
  CREATABLE: { label: "생성 가능", variant: "primary" },
  CREATED: { label: "생성 완료", variant: "success" },
  SKIPPED_DUPLICATE: { label: "기존 슬롯", variant: "secondary" },
  SKIPPED_PAST: { label: "지난 시각", variant: "warning" },
};

export function BulkSlotForm({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [classId, setClassId] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [weekdays, setWeekdays] = useState<BulkSlotRequest["weekdays"]>([]);
  const [startTimes, setStartTimes] = useState(["10:00"]);
  const [result, setResult] = useState<BulkSlotResponse | null>(null);

  const { data: classes, isLoading: classesLoading } = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.classes,
    queryFn: () => fetchClasses(adminKey),
  });
  const activeClasses = classes?.filter((bookingClass) => bookingClass.status === "ACTIVE") ?? [];

  const request = (): BulkSlotRequest => ({
    classId: Number(classId),
    dateFrom,
    dateTo,
    weekdays,
    startTimes: startTimes.filter(Boolean),
  });

  const preview = useAdminMutation(onAuthError, {
    mutationFn: () => previewBulkSlots(adminKey, request()),
    onSuccess: setResult,
  });
  const create = useAdminMutation(onAuthError, {
    mutationFn: () => createBulkSlots(adminKey, request()),
    onSuccess: (created) => {
      setResult(created);
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.slots.all });
      void invalidateSlotAvailability(queryClient);
      toast.show(`${created.createdCount}개 슬롯이 생성되었습니다.`);
    },
  });

  const clearPreview = () => setResult(null);
  const valid = Number(classId) > 0
    && dateFrom.length > 0
    && dateTo.length > 0
    && dateFrom <= dateTo
    && weekdays.length > 0
    && startTimes.length > 0
    && startTimes.every(Boolean);

  if (classesLoading) return <LoadingSpinner text="클래스 목록 로딩 중..." />;

  return (
    <div>
      <ErrorAlert error={preview.error ?? create.error} />
      <Row className="g-3">
        <Col xs={12} md={4}>
          <Form.Group controlId="bulk-slot-class">
            <Form.Label>클래스</Form.Label>
            <Form.Select value={classId} onChange={(event) => {
              setClassId(event.target.value);
              clearPreview();
            }}>
              <option value="">선택하세요</option>
              {activeClasses.map((bookingClass) => (
                <option key={bookingClass.id} value={bookingClass.id}>
                  {bookingClass.name} ({bookingClass.durationMin}분)
                </option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} md={4}>
          <Form.Group controlId="bulk-slot-date-from">
            <Form.Label>시작일</Form.Label>
            <Form.Control type="date" value={dateFrom} onChange={(event) => {
              setDateFrom(event.target.value);
              clearPreview();
            }} />
          </Form.Group>
        </Col>
        <Col xs={12} sm={6} md={4}>
          <Form.Group controlId="bulk-slot-date-to">
            <Form.Label>종료일</Form.Label>
            <Form.Control type="date" value={dateTo} onChange={(event) => {
              setDateTo(event.target.value);
              clearPreview();
            }} />
          </Form.Group>
        </Col>
        <Col xs={12}>
          <Form.Label className="d-block">운영 요일</Form.Label>
          <div className="d-flex flex-wrap gap-3">
            {WEEKDAYS.map(([value, label]) => (
              <Form.Check
                key={value}
                inline
                type="checkbox"
                id={`bulk-slot-weekday-${value}`}
                label={label}
                checked={weekdays.includes(value)}
                onChange={(event) => {
                  setWeekdays((current) => event.target.checked
                    ? [...current, value]
                    : current.filter((weekday) => weekday !== value));
                  clearPreview();
                }}
              />
            ))}
          </div>
        </Col>
        <Col xs={12}>
          <Form.Label className="d-block">시작 시각</Form.Label>
          <div className="d-flex flex-wrap gap-2">
            {startTimes.map((time, index) => (
              <div key={index} className="d-flex gap-1">
                <Form.Control
                  type="time"
                  value={time}
                  style={{ width: 136 }}
                  aria-label={`${index + 1}번째 시작 시각`}
                  onChange={(event) => {
                    setStartTimes((current) => current.map((item, itemIndex) =>
                      itemIndex === index ? event.target.value : item));
                    clearPreview();
                  }}
                />
                {startTimes.length > 1 && (
                  <Button
                    type="button"
                    variant="outline-secondary"
                    title="시작 시각 제거"
                    aria-label="시작 시각 제거"
                    onClick={() => {
                      setStartTimes((current) => current.filter((_, itemIndex) => itemIndex !== index));
                      clearPreview();
                    }}
                  >
                    &times;
                  </Button>
                )}
              </div>
            ))}
            <Button
              type="button"
              variant="outline-secondary"
              title="시작 시각 추가"
              aria-label="시작 시각 추가"
              disabled={startTimes.length >= 24}
              onClick={() => {
                setStartTimes((current) => [...current, ""]);
                clearPreview();
              }}
            >
              +
            </Button>
          </div>
        </Col>
        <Col xs={12} className="d-flex justify-content-end gap-2">
          <Button
            type="button"
            variant="outline-primary"
            disabled={!valid || preview.isPending || create.isPending}
            onClick={() => preview.mutate()}
          >
            {preview.isPending ? "확인 중..." : "생성 미리보기"}
          </Button>
          <Button
            type="button"
            disabled={!result || result.creatableCount === 0 || create.isPending}
            onClick={() => create.mutate()}
          >
            {create.isPending ? "생성 중..." : `${result?.creatableCount ?? 0}개 생성`}
          </Button>
        </Col>
      </Row>

      {result && (
        <div className="mt-4">
          <div className="d-flex flex-wrap gap-3 mb-2 small">
            <span>전체 {result.totalCount}개</span>
            {result.creatableCount > 0 && <span>생성 가능 {result.creatableCount}개</span>}
            {result.createdCount > 0 && <span>생성 완료 {result.createdCount}개</span>}
            {result.skippedCount > 0 && <span>건너뜀 {result.skippedCount}개</span>}
          </div>
          <Table responsive hover size="sm">
            <thead>
              <tr>
                <th>시작</th>
                <th>종료</th>
                <th>판정</th>
                <th>버퍼</th>
              </tr>
            </thead>
            <tbody>
              {result.items.map((item) => {
                const status = STATUS_VIEW[item.status];
                return (
                  <tr key={item.startAt}>
                    <td>{formatDateTime(item.startAt)}</td>
                    <td>{formatDateTime(item.endAt)}</td>
                    <td>
                      <Badge bg={status.variant} text={status.variant === "warning" ? "dark" : undefined}>
                        {status.label}
                      </Badge>
                    </td>
                    <td>{item.bufferBlocked ? "예약 버퍼로 차단" : "-"}</td>
                  </tr>
                );
              })}
            </tbody>
          </Table>
        </div>
      )}
    </div>
  );
}
