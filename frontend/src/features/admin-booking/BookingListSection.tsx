import { useEffect, useMemo, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Table, Button, Badge, Form, Row, Col, Modal } from "react-bootstrap";
import { CalendarPlus, CalendarX2 } from "lucide-react";
import {
  cancelBookingByAdmin,
  completeBooking,
  fetchBookings,
  markBalancePaid,
  markNoShow,
  updateArrears,
} from "./api";
import {
  EmptyState,
  ErrorAlert,
  getStatusLabel,
  LoadingSpinner,
  StatusBadge,
  useToast,
} from "@/shared/ui";
import { ApiError, queryKeys } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime, formatKRW, parseApiDateTime } from "@/shared/lib";
import type {
  AdminBookingCancelResponse,
  AdminBookingResponse,
  ListBookingsStatus,
} from "@/generated/api/adminBooking";
import type { RefundStatus } from "@/shared/types";
import { AdminBookingCreateModal } from "./AdminBookingCreateModal";
import {
  HalfHourDaySchedule,
  type HalfHourScheduleItem,
} from "@/features/admin-calendar/HalfHourDaySchedule";

interface Props {
  adminKey: string;
  onAuthError: () => void;
  initialDate?: string;
  initialStatus?: "" | ListBookingsStatus;
  focusBookingId?: number;
}

const STATUS_OPTIONS = [
  { value: "", label: "전체" },
  { value: "BOOKED", label: getStatusLabel("BOOKED", "admin") },
  { value: "CANCELED", label: getStatusLabel("CANCELED", "admin") },
  { value: "NO_SHOW", label: getStatusLabel("NO_SHOW", "admin") },
  { value: "COMPLETED", label: getStatusLabel("COMPLETED", "admin") },
] as const;

function todayStr(): string {
  return new Date().toLocaleDateString("sv-SE", { timeZone: "Asia/Seoul" });
}

const REFUND_STATUS_LABEL: Record<RefundStatus, string> = {
  REQUESTED: "요청 접수",
  PROCESSING: "처리 중",
  RETRYABLE: "자동으로 다시 처리 예정",
  RECONCILIATION_REQUIRED: "결제사 확인 필요",
  SUCCEEDED: "환불 완료",
  FAILED: "환불 실패",
};

function cancelResultToast(result: AdminBookingCancelResponse) {
  const compensation = result.depositRefundAmount > 0
    ? `예약금 ${formatKRW(result.depositRefundAmount)} ${result.depositRefundStatus
      ? REFUND_STATUS_LABEL[result.depositRefundStatus]
      : "확인 필요"}`
    : result.passCreditRestored
      ? "8회권 1회 복구"
      : result.manualCompensationRequired
        ? "직접 받은 예약금 반환 또는 8회권 보상 협의 필요"
        : "자동 환불 없음";
  const balance = result.balanceSettlementRequired
    ? "고객에게 받은 잔금 직접 반환 필요"
    : "별도로 반환할 잔금 없음";
  const needsAttention = result.manualCompensationRequired
    || result.balanceSettlementRequired
    || result.depositRefundStatus === "FAILED"
    || result.depositRefundStatus === "RETRYABLE"
    || result.depositRefundStatus === "RECONCILIATION_REQUIRED";
  const processing = result.depositRefundStatus === "REQUESTED"
    || result.depositRefundStatus === "PROCESSING";

  return {
    message: `예약 #${result.bookingId} 취소 완료 · ${compensation} · ${balance}`,
    variant: needsAttention ? "warning" : processing ? "info" : "success",
  } as const;
}

export function BookingListSection({
  adminKey,
  onAuthError,
  initialDate = todayStr(),
  initialStatus = "",
  focusBookingId,
}: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [date, setDate] = useState(initialDate);
  const [statusFilter, setStatusFilter] = useState<"" | ListBookingsStatus>(initialStatus);
  const [cancelTarget, setCancelTarget] = useState<AdminBookingResponse | null>(null);
  const [cancelReason, setCancelReason] = useState("");
  const [showCreate, setShowCreate] = useState(false);

  const { data: bookings, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: ["admin", "bookings", date, statusFilter],
    queryFn: () => fetchBookings(adminKey, date, statusFilter || undefined),
    enabled: date.length > 0,
  });

  const bookingScheduleItems = useMemo<HalfHourScheduleItem[]>(() => {
    const grouped = new Map<string, {
      start: string;
      end: string;
      className: string;
      status: AdminBookingResponse["status"];
      bookingCount: number;
      participantCount: number;
    }>();
    for (const booking of bookings ?? []) {
      const key = [booking.startAt, booking.endAt, booking.className, booking.status].join("|");
      const current = grouped.get(key);
      if (current) {
        current.bookingCount += 1;
        current.participantCount += booking.participantCount;
      } else {
        grouped.set(key, {
          start: booking.startAt,
          end: booking.endAt,
          className: booking.className,
          status: booking.status,
          bookingCount: 1,
          participantCount: booking.participantCount,
        });
      }
    }
    return Array.from(grouped.entries()).map(([id, group]) => ({
      id,
      start: group.start,
      end: group.end,
      title: group.className,
      detail: `${group.bookingCount}건 · ${group.participantCount}명 · ${getStatusLabel(group.status, "admin")}`,
      tone: group.status === "BOOKED"
        ? "primary"
        : group.status === "COMPLETED"
          ? "success"
          : group.status === "NO_SHOW"
            ? "danger"
            : "muted",
    }));
  }, [bookings]);

  const noShowMutation = useAdminMutation(onAuthError, {
    mutationFn: (bookingId: number) => markNoShow(adminKey, bookingId),
    onSuccess: (res) => {
      toast.show(`예약 #${res.bookingId}을 고객 불참으로 표시했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["admin", "bookings", date, statusFilter] });
    },
  });

  const balancePaymentMutation = useAdminMutation(onAuthError, {
    mutationFn: (bookingId: number) => markBalancePaid(adminKey, bookingId),
    onSuccess: (res) => {
      toast.show(`예약 #${res.bookingId}의 잔금을 수납 완료로 표시했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["admin", "bookings", date, statusFilter] });
    },
  });

  const arrearsMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ bookingId, arrears }: { bookingId: number; arrears: boolean }) =>
      updateArrears(adminKey, bookingId, arrears),
    onSuccess: (res) => {
      toast.show(`예약 #${res.bookingId}의 잔금 수납 여부를 변경했습니다.`);
      queryClient.invalidateQueries({ queryKey: ["admin", "bookings", date, statusFilter] });
    },
  });

  const completeMutation = useAdminMutation(onAuthError, {
    mutationFn: (bookingId: number) => completeBooking(adminKey, bookingId),
    onSuccess: (res) => {
      toast.show(`예약 #${res.bookingId} 수업 완료 처리 완료`);
      queryClient.invalidateQueries({ queryKey: ["admin", "bookings", date, statusFilter] });
    },
  });

  const cancelMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ bookingId, reason }: { bookingId: number; reason: string }) =>
      cancelBookingByAdmin(adminKey, bookingId, { reason }),
    onSuccess: (res) => {
      const notification = cancelResultToast(res);
      toast.show(notification.message, notification.variant);
      setCancelTarget(null);
      setCancelReason("");
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.bookings });
    },
  });

  const mutationPending = noShowMutation.isPending
    || balancePaymentMutation.isPending
    || arrearsMutation.isPending
    || completeMutation.isPending
    || cancelMutation.isPending;
  const mutationError = noShowMutation.error
    ?? balancePaymentMutation.error
    ?? arrearsMutation.error
    ?? completeMutation.error;

  useEffect(() => {
    if (!bookings?.some((booking) => booking.bookingId === focusBookingId)) return;
    document.getElementById(`admin-booking-${focusBookingId}`)?.scrollIntoView({ block: "center" });
  }, [bookings, focusBookingId]);

  return (
    <div>
      <Row className="g-2 mb-3 align-items-end">
        <Col xs={12} sm={5}>
          <Form.Group controlId="admin-booking-date-filter">
            <Form.Label>날짜</Form.Label>
            <Form.Control
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} sm={4}>
          <Form.Group controlId="admin-booking-status-filter">
            <Form.Label>상태</Form.Label>
            <Form.Select
              value={statusFilter}
              onChange={(event) => {
                const selected = STATUS_OPTIONS.find(
                  (option) => option.value === event.target.value,
                );
                setStatusFilter(selected?.value ?? "");
              }}
            >
              {STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
        <Col xs={12} sm={3}>
          <Button className="w-100" onClick={() => setShowCreate(true)}>
            <CalendarPlus size={16} aria-hidden="true" className="me-1" />
            전화·메신저·방문 예약 등록
          </Button>
        </Col>
      </Row>

      {isLoading && <LoadingSpinner />}
      {error && !(error instanceof ApiError && error.status === 401) && <ErrorAlert error={error} />}
      {bookings && bookings.length === 0 && <EmptyState message="해당 날짜에 예약이 없습니다." />}

      {bookings && bookings.length > 0 && (
        <>
          <HalfHourDaySchedule
            ariaLabel={`${date} 예약 시간표`}
            date={date}
            items={bookingScheduleItems}
            emptyMessage="표시할 예약이 없습니다."
          />
          <Table responsive hover size="sm">
          <thead>
            <tr>
              <th>예약번호</th>
              <th>예약자</th>
              <th>클래스</th>
              <th>시간</th>
              <th>상태</th>
              <th>결제</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {bookings.map((b) => (
              <tr
                key={b.bookingId}
                id={`admin-booking-${b.bookingId}`}
                className={b.bookingId === focusBookingId ? "table-info" : undefined}
              >
                <td>{b.bookingNumber}</td>
                <td>
                  <div>
                    {b.customerSummary.name}
                    {b.customerSummary.type === "MEMBER" && (
                      <Badge bg="success" className="ms-1" style={{ fontSize: "0.65em" }}>회원</Badge>
                    )}
                  </div>
                  <small className="text-muted-soft">{b.customerSummary.phone}</small>
                </td>
                <td>
                  <div>{b.className}</div>
                  <small className="text-muted-soft">
                    {b.participantCount}명
                    {b.source !== "WEB" ? ` · ${sourceLabel(b.source)}` : ""}
                  </small>
                </td>
                <td>
                  <small>{formatDateTime(b.startAt)}</small>
                  <br />
                  <small className="text-muted-soft">~ {formatDateTime(b.endAt)}</small>
                </td>
                <td><StatusBadge status={b.status} audience="admin" /></td>
                <td>
                  {b.passBooking ? (
                    <Badge bg="info">8회권</Badge>
                  ) : (
                    <small>예약금 {formatKRW(b.depositAmount)}</small>
                  )}
                  <div className="mt-1 d-flex flex-wrap gap-1">
                    <Badge bg={b.balanceStatus === "PAID" ? "success" : "secondary"}>
                      잔금 {b.balanceStatus === "PAID" ? "결제" : "미결제"}
                    </Badge>
                    {b.arrears && <Badge bg="warning" text="dark">잔금 받지 못함</Badge>}
                  </div>
                  {b.balanceAmount > 0 && (
                    <div>
                      <small className="text-muted-soft">{formatKRW(b.balanceAmount)}</small>
                      {b.balancePaidAt && (
                        <small className="d-block text-muted-soft">{formatDateTime(b.balancePaidAt)}</small>
                      )}
                    </div>
                  )}
                </td>
                <td>
                  <div className="d-flex flex-wrap gap-1" style={{ minWidth: 190 }}>
                    {["BOOKED", "COMPLETED"].includes(b.status) && b.balanceStatus === "UNPAID" && (
                      <>
                        <Button
                          size="sm"
                          variant="outline-success"
                          disabled={mutationPending}
                          onClick={() => balancePaymentMutation.mutate(b.bookingId)}
                        >
                          잔금 수납 완료로 표시
                        </Button>
                        <Form.Check
                          type="switch"
                          id={`booking-arrears-${b.bookingId}`}
                          label="잔금을 아직 받지 못함"
                          checked={b.arrears}
                          disabled={mutationPending}
                          onChange={(event) => arrearsMutation.mutate({
                            bookingId: b.bookingId,
                            arrears: event.target.checked,
                          })}
                        />
                      </>
                    )}
                    {b.status === "BOOKED" && (
                      <>
                        <Button
                          size="sm"
                          variant="outline-primary"
                          title={parseApiDateTime(b.endAt) > Date.now()
                            ? "수업 종료 후 완료할 수 있습니다."
                            : b.balanceStatus === "UNPAID" && !b.arrears
                              ? "잔금을 받았거나 아직 받지 못한 것으로 표시해 주세요."
                              : undefined}
                          disabled={
                            mutationPending
                            || parseApiDateTime(b.endAt) > Date.now()
                            || (b.balanceStatus === "UNPAID" && !b.arrears)
                          }
                          onClick={() => completeMutation.mutate(b.bookingId)}
                        >
                          수업 완료로 처리
                        </Button>
                        <Button
                          size="sm"
                          variant="outline-danger"
                          title={parseApiDateTime(b.endAt) > Date.now()
                            ? "수업 종료 후 고객 불참으로 표시할 수 있습니다."
                            : undefined}
                          disabled={mutationPending || parseApiDateTime(b.endAt) > Date.now()}
                          onClick={() => noShowMutation.mutate(b.bookingId)}
                        >
                          고객 불참으로 처리
                        </Button>
                        <Button
                          size="sm"
                          variant="danger"
                          disabled={mutationPending}
                          onClick={() => {
                            cancelMutation.reset();
                            setCancelReason("");
                            setCancelTarget(b);
                          }}
                        >
                          <CalendarX2 size={14} aria-hidden="true" className="me-1" />
                          공방 취소
                        </Button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
          </Table>
        </>
      )}

      {mutationError && !(mutationError instanceof ApiError && mutationError.status === 401) && (
        <ErrorAlert error={mutationError} />
      )}

      <Modal
        show={cancelTarget !== null}
        aria-labelledby="admin-booking-cancel-title"
        onHide={() => {
          if (!cancelMutation.isPending) setCancelTarget(null);
        }}
        centered
      >
        <Modal.Header closeButton={!cancelMutation.isPending}>
          <Modal.Title id="admin-booking-cancel-title" className="fs-6">
            공방 사정으로 예약 취소
          </Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={cancelMutation.error} />
          {cancelTarget && (
            <p className="small text-muted-soft mb-3">
              {cancelTarget.bookingNumber} · {cancelTarget.customerSummary.name} · {formatDateTime(cancelTarget.startAt)}
            </p>
          )}
          <p className="mb-3">
            예약을 취소하고 예약금 환불 또는 8회권 1회 복구를 시작합니다.
            이미 받은 잔금은 결과에 따라 고객에게 직접 반환해야 할 수 있습니다.
          </p>
          <Form.Group controlId="admin-booking-cancel-reason">
            <Form.Label>취소 사유</Form.Label>
            <Form.Control
              as="textarea"
              rows={3}
              maxLength={200}
              value={cancelReason}
              disabled={cancelMutation.isPending}
              onChange={(event) => setCancelReason(event.target.value)}
              placeholder="고객에게 안내할 취소 사유를 입력하세요."
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
              bookingId: cancelTarget.bookingId,
              reason: cancelReason.trim(),
            })}
          >
            {cancelMutation.isPending ? "취소 처리 중..." : "예약 취소 확정"}
          </Button>
        </Modal.Footer>
      </Modal>
      <AdminBookingCreateModal
        adminKey={adminKey}
        onAuthError={onAuthError}
        show={showCreate}
        onHide={() => setShowCreate(false)}
        onCreated={(booking) => {
          setShowCreate(false);
          setDate(booking.startAt.slice(0, 10));
          setStatusFilter("BOOKED");
          toast.show(`${booking.bookingNumber} 예약을 등록했습니다.`);
          queryClient.invalidateQueries({ queryKey: queryKeys.admin.bookings });
          queryClient.invalidateQueries({ queryKey: ["admin", "slots"] });
        }}
      />
    </div>
  );
}

function sourceLabel(source: AdminBookingResponse["source"]): string {
  if (source === "PHONE") return "전화";
  if (source === "NAVER_TALK") return "네이버톡톡";
  if (source === "KAKAO") return "카카오톡";
  if (source === "VISIT") return "방문";
  return "웹";
}
