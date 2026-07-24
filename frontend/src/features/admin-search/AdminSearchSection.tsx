import { useState, type FormEvent } from "react";
import { Badge, Button, ButtonGroup, Col, Form, Row, Table } from "react-bootstrap";
import { ArrowRight } from "lucide-react";
import { ApiError } from "@/shared/api";
import { formatDateTime, formatKRW } from "@/shared/lib";
import type { AdminBookingSearchRow, AdminOrderSearchRow, OffsetPage } from "@/shared/types";
import {
  EmptyState,
  ErrorAlert,
  getStatusLabel,
  LinkButton,
  LoadingSpinner,
  StatusBadge,
} from "@/shared/ui";
import {
  searchAdminRecords,
  type AdminSearchCriteria,
  type AdminSearchResult,
  type AdminSearchTarget,
} from "./api";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

const ORDER_STATUS_OPTIONS = [
  "PAID_APPROVAL_PENDING",
  "APPROVED_FULFILLMENT_PENDING",
  "IN_PRODUCTION",
  "DELAY_CONSENT_PENDING",
  "DELAY_ACCEPTED",
  "SHIPPING_PREPARING",
  "SHIPPED",
  "DELIVERED",
  "PICKUP_READY",
  "PICKED_UP",
  "COMPLETED",
  "REJECTED",
  "CUSTOMER_CANCELED",
  "AUTO_REFUND_TIMEOUT",
  "PICKUP_EXPIRED",
  "PICKUP_FORFEITED",
] as const;

const BOOKING_STATUS_OPTIONS = ["BOOKED", "CANCELED", "NO_SHOW", "COMPLETED"] as const;

function Pagination({
  page,
  onMove,
  disabled,
}: {
  page: OffsetPage<unknown>;
  onMove: (nextPage: number) => void;
  disabled: boolean;
}) {
  if (page.totalPages === 0) {
    return <small className="d-block mt-3 text-muted-soft">총 0건</small>;
  }

  const currentPage = page.page + 1;
  return (
    <div className="admin-search-pagination">
      <small className="text-muted-soft">
        총 {page.totalCount.toLocaleString("ko-KR")}건 · {currentPage}/{page.totalPages.toLocaleString("ko-KR")} 페이지
      </small>
      <ButtonGroup size="sm" aria-label="검색 결과 페이지 이동">
        <Button
          variant="outline-secondary"
          disabled={disabled || page.page === 0}
          onClick={() => onMove(page.page - 1)}
        >
          이전
        </Button>
        <Button
          variant="outline-secondary"
          disabled={disabled || page.page + 1 >= page.totalPages}
          onClick={() => onMove(page.page + 1)}
        >
          다음
        </Button>
      </ButtonGroup>
    </div>
  );
}

function normalizeKeyword(value: string): string | undefined {
  const trimmed = value.trim();
  return trimmed || undefined;
}

function OrderSearchResults({ page }: { page: OffsetPage<AdminOrderSearchRow> }) {
  if (page.content.length === 0) {
    return <EmptyState message="검색 조건에 맞는 주문이 없습니다." />;
  }

  return (
    <Table responsive hover size="sm" className="mb-0">
      <thead>
        <tr><th>주문번호</th><th>구매자</th><th>상태</th><th>금액</th><th>결제일</th><th>생성일</th><th></th></tr>
      </thead>
      <tbody>
        {page.content.map((order) => (
          <tr key={order.orderId}>
            <td>{order.orderNumber}</td>
            <td>
              <div>{order.buyerName}</div>
              <small className="text-muted-soft">{order.buyerPhone ?? "-"}</small>
            </td>
            <td><StatusBadge status={order.status} /></td>
            <td>{formatKRW(order.totalAmount)}</td>
            <td><small>{order.paidAt ? formatDateTime(order.paidAt) : "-"}</small></td>
            <td><small>{formatDateTime(order.createdAt)}</small></td>
            <td>
              <LinkButton
                size="sm"
                variant="outline-primary"
                to={`/admin?view=orders&orderId=${order.orderId}&orderStatus=${encodeURIComponent(order.status)}`}
              >
                운영 <ArrowRight size={14} aria-hidden="true" />
              </LinkButton>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}

function BookingSearchResults({ page }: { page: OffsetPage<AdminBookingSearchRow> }) {
  if (page.content.length === 0) {
    return <EmptyState message="검색 조건에 맞는 예약이 없습니다." />;
  }

  return (
    <Table responsive hover size="sm" className="mb-0">
      <thead>
        <tr><th>예약번호</th><th>예약자</th><th>클래스</th><th>수업 시간</th><th>상태</th><th>결제</th><th></th></tr>
      </thead>
      <tbody>
        {page.content.map((booking) => (
          <tr key={booking.bookingId}>
            <td>{booking.bookingNumber}</td>
            <td>
              <div>
                {booking.bookerName}
                {booking.bookerType === "MEMBER" && <Badge bg="success" className="ms-1">회원</Badge>}
              </div>
              <small className="text-muted-soft">{booking.bookerPhone ?? "-"}</small>
            </td>
            <td>{booking.className}</td>
            <td>
              <small>{formatDateTime(booking.startAt)}</small>
              <small className="d-block text-muted-soft">~ {formatDateTime(booking.endAt)}</small>
            </td>
            <td><StatusBadge status={booking.status} /></td>
            <td>
              {booking.passBooking ? (
                <Badge bg="info">8회권</Badge>
              ) : (
                <small>예약금 {formatKRW(booking.depositAmount)}</small>
              )}
              {!booking.passBooking && (
                <div className="mt-1 d-flex gap-1">
                  <Badge bg={booking.balanceStatus === "PAID" ? "success" : "secondary"}>
                    잔금 {booking.balanceStatus === "PAID" ? "결제" : "미결제"}
                  </Badge>
                  {booking.arrears && <Badge bg="warning" text="dark">미수</Badge>}
                </div>
              )}
            </td>
            <td>
              <LinkButton
                size="sm"
                variant="outline-primary"
                to={`/admin?view=bookings&bookingId=${booking.bookingId}&bookingDate=${booking.startAt.slice(0, 10)}&bookingStatus=${encodeURIComponent(booking.status)}`}
              >
                운영 <ArrowRight size={14} aria-hidden="true" />
              </LinkButton>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
}

function SearchResults({ result }: { result: AdminSearchResult }) {
  return result.target === "ORDER"
    ? <OrderSearchResults page={result.page} />
    : <BookingSearchResults page={result.page} />;
}

export function AdminSearchSection({ adminKey, onAuthError }: Props) {
  const [target, setTarget] = useState<AdminSearchTarget>("ORDER");
  const [keyword, setKeyword] = useState("");
  const [status, setStatus] = useState("");
  const [dateFrom, setDateFrom] = useState("");
  const [dateTo, setDateTo] = useState("");
  const [validationError, setValidationError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState<AdminSearchCriteria | null>(null);

  const query = useAdminQuery(onAuthError, {
    queryKey: ["admin", "search", submitted],
    queryFn: () => searchAdminRecords(adminKey, submitted!),
    enabled: submitted != null,
  });

  function selectTarget(nextTarget: AdminSearchTarget) {
    setTarget(nextTarget);
    setStatus("");
    setSubmitted(null);
    setValidationError(null);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (dateFrom && dateTo && dateFrom > dateTo) {
      setValidationError("시작일은 종료일보다 늦을 수 없습니다.");
      return;
    }

    setValidationError(null);
    setSubmitted({
      target,
      status: status || undefined,
      dateFrom: dateFrom || undefined,
      dateTo: dateTo || undefined,
      keyword: normalizeKeyword(keyword),
      page: 0,
    });
  }

  function movePage(page: number) {
    setSubmitted((current) => current ? { ...current, page } : current);
  }

  const statusOptions = target === "ORDER" ? ORDER_STATUS_OPTIONS : BOOKING_STATUS_OPTIONS;

  return (
    <div>
      <ButtonGroup size="sm" className="mb-3" aria-label="검색 대상">
        <Button
          variant={target === "ORDER" ? "dark" : "outline-secondary"}
          onClick={() => selectTarget("ORDER")}
        >
          주문
        </Button>
        <Button
          variant={target === "BOOKING" ? "dark" : "outline-secondary"}
          onClick={() => selectTarget("BOOKING")}
        >
          예약
        </Button>
      </ButtonGroup>

      <Form onSubmit={handleSubmit}>
        <Row className="g-2 align-items-end">
          <Col xs={12} md={4}>
            <Form.Group controlId="admin-search-keyword">
              <Form.Label>식별자 또는 고객명</Form.Label>
              <Form.Control
                type="search"
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder={target === "ORDER" ? "주문번호 또는 구매자명" : "예약번호 또는 예약자명"}
              />
            </Form.Group>
          </Col>
          <Col xs={12} sm={4} md={2}>
            <Form.Group controlId="admin-search-status">
              <Form.Label>상태</Form.Label>
              <Form.Select value={status} onChange={(event) => setStatus(event.target.value)}>
                <option value="">전체</option>
                {statusOptions.map((value) => (
                  <option key={value} value={value}>{getStatusLabel(value)}</option>
                ))}
              </Form.Select>
            </Form.Group>
          </Col>
          <Col xs={12} sm={4} md={2}>
            <Form.Group controlId="admin-search-date-from">
              <Form.Label>시작일</Form.Label>
              <Form.Control type="date" value={dateFrom} onChange={(event) => setDateFrom(event.target.value)} />
            </Form.Group>
          </Col>
          <Col xs={12} sm={4} md={2}>
            <Form.Group controlId="admin-search-date-to">
              <Form.Label>종료일</Form.Label>
              <Form.Control type="date" value={dateTo} onChange={(event) => setDateTo(event.target.value)} />
            </Form.Group>
          </Col>
          <Col xs="auto">
            <Button type="submit" variant="dark" disabled={query.isFetching}>
              {query.isFetching ? "검색 중..." : "검색"}
            </Button>
          </Col>
        </Row>
      </Form>

      {validationError && <div className="text-danger mt-2" role="alert">{validationError}</div>}
      {query.isLoading && <LoadingSpinner text="검색 중..." />}
      {query.error && !(query.error instanceof ApiError && query.error.status === 401) && (
        <div className="mt-3"><ErrorAlert error={query.error} /></div>
      )}
      {query.data && (
        <div className="admin-search-results">
          <SearchResults result={query.data} />
          <Pagination page={query.data.page} onMove={movePage} disabled={query.isFetching} />
        </div>
      )}
    </div>
  );
}
