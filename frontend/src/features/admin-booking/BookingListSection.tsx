import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Table, Button, Badge, Form, Row, Col } from "react-bootstrap";
import {
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
import { ApiError } from "@/shared/api";
import { useAdminMutation } from "@/shared/hooks/useAdminMutation";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";
import { formatDateTime, formatKRW } from "@/shared/lib";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

const STATUS_OPTIONS = [
  { value: "", label: "전체" },
  { value: "BOOKED", label: getStatusLabel("BOOKED") },
  { value: "CANCELED", label: getStatusLabel("CANCELED") },
  { value: "NO_SHOW", label: getStatusLabel("NO_SHOW") },
  { value: "COMPLETED", label: getStatusLabel("COMPLETED") },
] as const;

function todayStr(): string {
  return new Date().toLocaleDateString("sv-SE", { timeZone: "Asia/Seoul" });
}

export function BookingListSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [date, setDate] = useState(todayStr);
  const [statusFilter, setStatusFilter] = useState("");

  const { data: bookings, isLoading, error } = useAdminQuery(onAuthError, {
    queryKey: ["admin", "bookings", date, statusFilter],
    queryFn: () => fetchBookings(adminKey, date, statusFilter || undefined),
    enabled: date.length > 0,
  });

  const noShowMutation = useAdminMutation(onAuthError, {
    mutationFn: (bookingId: number) => markNoShow(adminKey, bookingId),
    onSuccess: (res) => {
      toast.show(`예약 #${res.bookingId} 노쇼 처리 완료`);
      queryClient.invalidateQueries({ queryKey: ["admin", "bookings", date, statusFilter] });
    },
  });

  const balancePaymentMutation = useAdminMutation(onAuthError, {
    mutationFn: (bookingId: number) => markBalancePaid(adminKey, bookingId),
    onSuccess: (res) => {
      toast.show(`예약 #${res.bookingId} 잔금 결제 처리 완료`);
      queryClient.invalidateQueries({ queryKey: ["admin", "bookings", date, statusFilter] });
    },
  });

  const arrearsMutation = useAdminMutation(onAuthError, {
    mutationFn: ({ bookingId, arrears }: { bookingId: number; arrears: boolean }) =>
      updateArrears(adminKey, bookingId, arrears),
    onSuccess: (res) => {
      toast.show(`예약 #${res.bookingId} 미수 상태 변경 완료`);
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

  const mutationPending = noShowMutation.isPending
    || balancePaymentMutation.isPending
    || arrearsMutation.isPending
    || completeMutation.isPending;
  const mutationError = noShowMutation.error
    ?? balancePaymentMutation.error
    ?? arrearsMutation.error
    ?? completeMutation.error;

  return (
    <div>
      <Row className="g-2 mb-3">
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
            <Form.Select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)}>
              {STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </Form.Select>
          </Form.Group>
        </Col>
      </Row>

      {isLoading && <LoadingSpinner />}
      {error && !(error instanceof ApiError && error.status === 401) && <ErrorAlert error={error} />}
      {bookings && bookings.length === 0 && <EmptyState message="해당 날짜에 예약이 없습니다." />}

      {bookings && bookings.length > 0 && (
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
              <tr key={b.bookingId}>
                <td>{b.bookingNumber}</td>
                <td>
                  <div>
                    {b.bookerName}
                    {b.bookerType === "MEMBER" && (
                      <Badge bg="success" className="ms-1" style={{ fontSize: "0.65em" }}>회원</Badge>
                    )}
                  </div>
                  <small className="text-muted-soft">{b.bookerPhone}</small>
                </td>
                <td>{b.className}</td>
                <td>
                  <small>{formatDateTime(b.startAt)}</small>
                  <br />
                  <small className="text-muted-soft">~ {formatDateTime(b.endAt)}</small>
                </td>
                <td><StatusBadge status={b.status} /></td>
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
                    {b.arrears && <Badge bg="warning" text="dark">미수</Badge>}
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
                          잔금 결제
                        </Button>
                        <Form.Check
                          type="switch"
                          id={`booking-arrears-${b.bookingId}`}
                          label="미수"
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
                          title={Date.parse(b.endAt) > Date.now()
                            ? "수업 종료 후 완료할 수 있습니다."
                            : b.balanceStatus === "UNPAID" && !b.arrears
                              ? "잔금을 결제하거나 미수로 표시해 주세요."
                              : undefined}
                          disabled={
                            mutationPending
                            || Date.parse(b.endAt) > Date.now()
                            || (b.balanceStatus === "UNPAID" && !b.arrears)
                          }
                          onClick={() => completeMutation.mutate(b.bookingId)}
                        >
                          수업 완료
                        </Button>
                        <Button
                          size="sm"
                          variant="outline-danger"
                          disabled={mutationPending}
                          onClick={() => noShowMutation.mutate(b.bookingId)}
                        >
                          노쇼
                        </Button>
                      </>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}

      {mutationError && !(mutationError instanceof ApiError && mutationError.status === 401) && (
        <ErrorAlert error={mutationError} />
      )}
    </div>
  );
}
