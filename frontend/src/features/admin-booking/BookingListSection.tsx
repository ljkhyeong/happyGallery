import { useState, useEffect } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Table, Button, Badge, Form, Row, Col } from "react-bootstrap";
import { fetchBookings, markNoShow } from "./api";
import { LoadingSpinner, ErrorAlert, EmptyState, useToast } from "@/shared/ui";
import { ApiError } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

const STATUS_OPTIONS = [
  { value: "", label: "전체" },
  { value: "BOOKED", label: "예약됨" },
  { value: "CANCELED", label: "취소" },
  { value: "NO_SHOW", label: "노쇼" },
  { value: "COMPLETED", label: "완료" },
] as const;

function statusBadge(status: string) {
  switch (status) {
    case "BOOKED": return <Badge bg="primary">예약됨</Badge>;
    case "CANCELED": return <Badge bg="secondary">취소</Badge>;
    case "NO_SHOW": return <Badge bg="danger">노쇼</Badge>;
    case "COMPLETED": return <Badge bg="success">완료</Badge>;
    default: return <Badge bg="dark">{status}</Badge>;
  }
}

function todayStr(): string {
  return new Date().toISOString().slice(0, 10);
}

export function BookingListSection({ adminKey, onAuthError }: Props) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [date, setDate] = useState(todayStr);
  const [statusFilter, setStatusFilter] = useState("");
  const [pendingId, setPendingId] = useState<number | null>(null);

  const { data: bookings, isLoading, error } = useQuery({
    queryKey: ["admin", "bookings", date, statusFilter],
    queryFn: () => fetchBookings(adminKey, date, statusFilter || undefined),
    enabled: date.length > 0,
  });

  useEffect(() => {
    if (error instanceof ApiError && error.status === 401) {
      onAuthError();
    }
  }, [error, onAuthError]);

  const noShowMutation = useMutation({
    mutationFn: (bookingId: number) => markNoShow(adminKey, bookingId),
    onMutate: (bookingId) => setPendingId(bookingId),
    onSuccess: (res) => {
      toast.show(`예약 #${res.bookingId} 노쇼 처리 완료`);
      queryClient.invalidateQueries({ queryKey: ["admin", "bookings", date, statusFilter] });
    },
    onError: (err) => {
      if (err instanceof ApiError && err.status === 401) onAuthError();
    },
    onSettled: () => setPendingId(null),
  });

  return (
    <div>
      <Row className="g-2 mb-3">
        <Col xs={12} sm={5}>
          <Form.Group>
            <Form.Label>날짜</Form.Label>
            <Form.Control
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          </Form.Group>
        </Col>
        <Col xs={12} sm={4}>
          <Form.Group>
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
              <th>게스트</th>
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
                  <div>{b.guestName}</div>
                  <small className="text-muted-soft">{b.guestPhone}</small>
                </td>
                <td>{b.className}</td>
                <td>
                  <small>{formatDateTime(b.startAt)}</small>
                  <br />
                  <small className="text-muted-soft">~ {formatDateTime(b.endAt)}</small>
                </td>
                <td>{statusBadge(b.status)}</td>
                <td>
                  {b.passBooking ? (
                    <Badge bg="info">8회권</Badge>
                  ) : (
                    <small>{formatKRW(b.depositAmount)}</small>
                  )}
                </td>
                <td>
                  {b.status === "BOOKED" && (
                    <Button
                      size="sm"
                      variant="outline-danger"
                      disabled={pendingId === b.bookingId}
                      onClick={() => noShowMutation.mutate(b.bookingId)}
                    >
                      {pendingId === b.bookingId ? "처리 중..." : "노쇼"}
                    </Button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
      )}

      {noShowMutation.error && !(noShowMutation.error instanceof ApiError && noShowMutation.error.status === 401) && (
        <ErrorAlert error={noShowMutation.error} />
      )}
    </div>
  );
}
