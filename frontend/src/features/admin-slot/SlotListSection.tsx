import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Table, Button, Badge, Form, Row, Col, ProgressBar, Modal } from "react-bootstrap";
import { CalendarX2 } from "lucide-react";
import {
  activateSlot,
  cancelSlotSession,
  deactivateSlot,
  fetchClasses,
  fetchSlotsByClass,
} from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, useToast } from "@/shared/ui";
import {
  ApiError,
  invalidateSlotAvailability,
  queryKeys,
} from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime } from "@/shared/lib";
import type { SlotResponse } from "@/shared/types";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

export function SlotListSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [classId, setClassId] = useState("");
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [cancelTarget, setCancelTarget] = useState<SlotResponse | null>(null);
  const [cancelReason, setCancelReason] = useState("");

  const classesQuery = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.classes,
    queryFn: () => fetchClasses(adminKey),
  });

  const classIdNum = Number(classId);
  const slotsQuery = useAdminQuery(onAuthError, {
    queryKey: queryKeys.admin.slots.byClass(classIdNum),
    queryFn: () => fetchSlotsByClass(adminKey, classIdNum),
    enabled: classIdNum > 0,
  });

  const mutation = useAdminMutation(onAuthError, {
    mutationFn: ({ slotId, activate }: { slotId: number; activate: boolean }) =>
      activate ? activateSlot(adminKey, slotId) : deactivateSlot(adminKey, slotId),
    onMutate: ({ slotId }) => setPendingId(slotId),
    onSuccess: (slot) => {
      toast.show(`수업 일정 #${slot.id}의 예약을 ${slot.adminActive ? "재개" : "중지"}했습니다.`);
      queryClient.invalidateQueries({
        queryKey: queryKeys.admin.slots.byClass(classIdNum),
      });
      void invalidateSlotAvailability(queryClient);
    },
    onSettled: () => setPendingId(null),
  });

  const cancelMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ slotId, reason }: { slotId: number; reason: string }) =>
      cancelSlotSession(adminKey, slotId, { reason }),
    onSuccess: (result) => {
      const needsManualAction = result.balanceSettlementsRequired > 0
        || result.manualCompensationsRequired > 0;
      toast.show(
        `회차 취소 완료 · 예약 취소 ${result.canceledBookings}건 · 8회권 복구 ${result.passCreditsRestored}회 · 예약금 환불 요청 ${result.depositRefundsRequested}건 · 받은 잔금 직접 반환 ${result.balanceSettlementsRequired}건 · 고객과 보상 협의 ${result.manualCompensationsRequired}건`,
        needsManualAction
          ? "warning"
          : result.depositRefundsRequested > 0
            ? "info"
            : "success",
      );
      setCancelTarget(null);
      setCancelReason("");
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.slots.all });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.bookings });
      void invalidateSlotAvailability(queryClient);
    },
  });

  const managedSlots = slotsQuery.data?.filter((slot) =>
    slot.bookedCount > 0 || !slot.adminActive,
  );

  return (
    <div>
      {classesQuery.isLoading && <LoadingSpinner text="클래스 목록을 불러오는 중..." />}
      {classesQuery.error && !(classesQuery.error instanceof ApiError && classesQuery.error.status === 401) && (
        <ErrorAlert
          error={classesQuery.error}
          onRetry={() => { void classesQuery.refetch(); }}
          retrying={classesQuery.isFetching}
        />
      )}
      {classesQuery.data !== undefined && (
        <>
          <Row className="g-2 mb-3">
            <Col xs={12} sm={6}>
              <Form.Group>
                <Form.Label>클래스 선택</Form.Label>
                <Form.Select value={classId} onChange={(e) => setClassId(e.target.value)}>
                  <option value="">클래스를 선택하세요</option>
                  {classesQuery.data.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name} ({c.durationMin}분{c.status === "INACTIVE" ? ", 운영 중지" : ""})
                    </option>
                  ))}
                </Form.Select>
              </Form.Group>
            </Col>
          </Row>

          {!classesQuery.error && !classIdNum && (
            <EmptyState message="클래스를 선택하면 예약이 있거나 직접 중지한 회차만 표시됩니다." />
          )}
        </>
      )}
      {slotsQuery.isLoading && <LoadingSpinner />}
      {slotsQuery.error && !(slotsQuery.error instanceof ApiError && slotsQuery.error.status === 401) && (
        <ErrorAlert
          error={slotsQuery.error}
          onRetry={() => { void slotsQuery.refetch(); }}
          retrying={slotsQuery.isFetching}
        />
      )}
      {!slotsQuery.error && managedSlots && managedSlots.length === 0 && (
        <EmptyState message="관리할 예약 회차가 없습니다. 빈 회차는 예약 캘린더에서 자동으로 관리됩니다." />
      )}

      {managedSlots && managedSlots.length > 0 && (
        <Table responsive hover size="sm">
          <thead>
            <tr>
              <th>일정 번호</th>
              <th>시작</th>
              <th>종료</th>
              <th>예약 현황</th>
              <th>상태</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {managedSlots.map((s) => {
              const pct = s.capacity > 0 ? Math.round((s.bookedCount / s.capacity) * 100) : 0;
              const variant = pct >= 80 ? "danger" : pct >= 50 ? "warning" : "success";
              const status = !s.adminActive
                ? { label: "예약 중지", variant: "secondary" }
                : !s.calendarActive
                  ? { label: "달력에서 닫힘", variant: "secondary" }
                : s.bufferBlocked
                  ? { label: "다른 수업·정리 시간과 겹침", variant: "warning" }
                  : { label: "예약 가능", variant: "success" };
              return (
                <tr key={s.id}>
                  <td>{s.id}</td>
                  <td>{formatDateTime(s.startAt)}</td>
                  <td>{formatDateTime(s.endAt)}</td>
                  <td style={{ minWidth: 120 }}>
                    <div className="d-flex align-items-center gap-2">
                      <ProgressBar now={pct} variant={variant} style={{ flex: 1, height: 8 }} />
                      <small className="text-nowrap">{s.bookedCount}/{s.capacity}</small>
                    </div>
                  </td>
                  <td>
                    <Badge bg={status.variant} text={status.variant === "warning" ? "dark" : undefined}>
                      {status.label}
                    </Badge>
                  </td>
                  <td>
                    <div className="d-flex flex-wrap gap-1" style={{ minWidth: 180 }}>
                      <Button
                        size="sm"
                        variant={s.adminActive ? "outline-danger" : "outline-success"}
                        style={{ minWidth: 84 }}
                        disabled={pendingId === s.id || cancelMutation.isPending}
                        onClick={() => mutation.mutate({ slotId: s.id, activate: !s.adminActive })}
                      >
                        {pendingId === s.id ? "처리 중..." : s.adminActive ? "예약 중지" : "예약 재개"}
                      </Button>
                      {!s.adminActive && s.bookedCount > 0 && (
                        <Button
                          size="sm"
                          variant="danger"
                          disabled={pendingId !== null || cancelMutation.isPending}
                          onClick={() => {
                            cancelMutation.reset();
                            setCancelReason("");
                            setCancelTarget(s);
                          }}
                        >
                          <CalendarX2 size={14} aria-hidden="true" className="me-1" />
                          회차 취소
                        </Button>
                      )}
                    </div>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </Table>
      )}

      <Modal
        show={cancelTarget !== null}
        aria-labelledby="admin-slot-session-cancel-title"
        onHide={() => {
          if (!cancelMutation.isPending) setCancelTarget(null);
        }}
        centered
      >
        <Modal.Header closeButton={!cancelMutation.isPending}>
          <Modal.Title id="admin-slot-session-cancel-title" className="fs-6">
            회차 일괄 취소
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={cancelMutation.error} />
          {cancelTarget && (
            <>
              <p className="small text-muted-soft mb-3">
                수업 일정 #{cancelTarget.id} · {formatDateTime(cancelTarget.startAt)} · 예약 {cancelTarget.bookedCount}건
              </p>
              <p className="mb-3">
                이 회차의 예약을 모두 취소하고 예약금 환불 또는 8회권 복구를 시작합니다.
                처리 결과에 따라 이미 받은 잔금을 직접 반환하거나 고객과 보상 방법을 협의해야 할 수 있습니다.
              </p>
            </>
          )}
          <Form.Group controlId="admin-slot-session-cancel-reason">
            <Form.Label>취소 사유</Form.Label>
            <Form.Control
              as="textarea"
              rows={3}
              maxLength={200}
              value={cancelReason}
              disabled={cancelMutation.isPending}
              onChange={(event) => setCancelReason(event.target.value)}
              placeholder="고객에게 안내할 회차 취소 사유를 입력하세요."
              autoFocus
            />
            <Form.Text className="text-muted">{cancelReason.length}/200자</Form.Text>
          </Form.Group>
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="outline-secondary"
            disabled={cancelMutation.isPending}
            onClick={() => setCancelTarget(null)}
          >
            닫기
          </Button>
          <Button
            variant="danger"
            disabled={!cancelTarget || !cancelReason.trim() || cancelMutation.isPending}
            onClick={() => cancelTarget && cancelMutation.mutate({
              slotId: cancelTarget.id,
              reason: cancelReason.trim(),
            })}
          >
            {cancelMutation.isPending ? "취소 처리 중..." : "회차 취소 확정"}
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}
