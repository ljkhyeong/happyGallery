import { useMemo, useState, type FormEvent } from "react";
import { Alert, Button, ButtonGroup, Col, Form, Row, Table } from "react-bootstrap";
import { ApiError } from "@/shared/api";
import { formatKRW } from "@/shared/lib";
import type { DashboardGranularity, DailyRevenue } from "@/shared/types";
import { EmptyState, ErrorAlert, getStatusLabel, LoadingSpinner } from "@/shared/ui";
import { fetchDashboardSnapshot, fetchSalesSummary } from "./api";
import { useAdminQuery } from "@/shared/hooks/useAdminQuery";

interface Props {
  adminKey: string;
  onAuthError: () => void;
}

interface DashboardRange {
  from: string;
  to: string;
}

const GRANULARITY_OPTIONS: Array<{ value: DashboardGranularity; label: string }> = [
  { value: "DAILY", label: "일별" },
  { value: "WEEKLY", label: "주별" },
  { value: "MONTHLY", label: "월별" },
];

function kstDate(date = new Date()): string {
  return date.toLocaleDateString("sv-SE", { timeZone: "Asia/Seoul" });
}

function recentRange(days: number): DashboardRange {
  const to = new Date();
  const from = new Date(to.getTime() - (days - 1) * 24 * 60 * 60 * 1000);
  return { from: kstDate(from), to: kstDate(to) };
}

function monthRange(): DashboardRange {
  const to = kstDate();
  return { from: `${to.slice(0, 8)}01`, to };
}

function percent(value: number): string {
  return new Intl.NumberFormat("ko-KR", {
    style: "percent",
    maximumFractionDigits: 1,
  }).format(value);
}

function RevenueTrend({ rows }: { rows: DailyRevenue[] }) {
  const maxAbsoluteRevenue = useMemo(
    () => Math.max(1, ...rows.map((row) => Math.abs(row.revenue))),
    [rows],
  );

  if (rows.length === 0) {
    return <EmptyState message="선택한 기간의 매출이 없습니다." />;
  }

  return (
    <div className="admin-revenue-trend" aria-label="일별 전체 순매출">
      {rows.map((row) => {
        const width = `${Math.max(1, Math.abs(row.revenue) / maxAbsoluteRevenue * 50)}%`;
        const direction = row.revenue < 0 ? "negative" : "positive";
        return (
          <div className="admin-revenue-row" key={row.date}>
            <time dateTime={row.date}>{row.date.slice(5).replace("-", ".")}</time>
            <div className="admin-revenue-bar-track" aria-hidden="true">
              <span className={`admin-revenue-bar ${direction}`} style={{ width }} />
            </div>
            <strong>{formatKRW(row.revenue)}</strong>
          </div>
        );
      })}
    </div>
  );
}

export function AdminDashboardSection({ adminKey, onAuthError }: Props) {
  const initialRange = useMemo(monthRange, []);
  const [draftRange, setDraftRange] = useState(initialRange);
  const [range, setRange] = useState(initialRange);
  const [rangeError, setRangeError] = useState<string | null>(null);
  const [granularity, setGranularity] = useState<DashboardGranularity>("DAILY");

  const snapshotQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "dashboard", "snapshot", range.from, range.to],
    queryFn: () => fetchDashboardSnapshot(adminKey, range),
  });
  const salesQuery = useAdminQuery(onAuthError, {
    queryKey: ["admin", "dashboard", "sales", range.from, range.to, granularity],
    queryFn: () => fetchSalesSummary(adminKey, range, granularity),
  });

  const queryError = snapshotQuery.error ?? salesQuery.error;
  const isLoading = snapshotQuery.isLoading || salesQuery.isLoading;
  const isFetching = snapshotQuery.isFetching || salesQuery.isFetching;

  function applyRange(nextRange: DashboardRange) {
    setDraftRange(nextRange);
    setRange(nextRange);
    setRangeError(null);
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (draftRange.from > draftRange.to) {
      setRangeError("시작일은 종료일보다 늦을 수 없습니다.");
      return;
    }
    applyRange(draftRange);
    if (draftRange.from === range.from && draftRange.to === range.to) {
      void snapshotQuery.refetch();
      void salesQuery.refetch();
    }
  }

  return (
    <section className="admin-dashboard" aria-labelledby="admin-dashboard-title">
      <div className="admin-dashboard-heading">
        <div>
          <h5 id="admin-dashboard-title" className="mb-1">운영 현황</h5>
          <small className="text-muted-soft">{range.from} ~ {range.to}</small>
        </div>
        <ButtonGroup size="sm" aria-label="집계 기간 빠른 선택">
          <Button variant="outline-secondary" onClick={() => applyRange(recentRange(7))}>최근 7일</Button>
          <Button variant="outline-secondary" onClick={() => applyRange(recentRange(30))}>최근 30일</Button>
          <Button variant="outline-secondary" onClick={() => applyRange(monthRange())}>이번 달</Button>
        </ButtonGroup>
      </div>

      <Form onSubmit={handleSubmit} className="admin-dashboard-filters">
        <Row className="g-2 align-items-end">
          <Col xs={12} sm={4} md={3}>
            <Form.Group controlId="admin-dashboard-from">
              <Form.Label>시작일</Form.Label>
              <Form.Control
                type="date"
                value={draftRange.from}
                onChange={(event) => setDraftRange((current) => ({ ...current, from: event.target.value }))}
              />
            </Form.Group>
          </Col>
          <Col xs={12} sm={4} md={3}>
            <Form.Group controlId="admin-dashboard-to">
              <Form.Label>종료일</Form.Label>
              <Form.Control
                type="date"
                value={draftRange.to}
                onChange={(event) => setDraftRange((current) => ({ ...current, to: event.target.value }))}
              />
            </Form.Group>
          </Col>
          <Col xs="auto">
            <Button type="submit" variant="dark" disabled={isFetching}>조회</Button>
          </Col>
        </Row>
      </Form>

      {rangeError && <Alert variant="danger" className="mt-3 mb-0">{rangeError}</Alert>}
      {isLoading && <LoadingSpinner text="운영 지표를 집계하는 중..." />}
      {queryError && !(queryError instanceof ApiError && queryError.status === 401) && (
        <div className="mt-3"><ErrorAlert error={queryError} /></div>
      )}

      {snapshotQuery.data && salesQuery.data && (
        <div className={isFetching ? "admin-dashboard-content is-refreshing" : "admin-dashboard-content"}>
          <dl className="admin-metric-grid">
            <div><dt>오늘 전체 순매출</dt><dd>{formatKRW(snapshotQuery.data.overview.todayRevenue)}</dd></div>
            <div><dt>오늘 주문</dt><dd>{snapshotQuery.data.overview.todayOrderCount.toLocaleString("ko-KR")}건</dd></div>
            <div><dt>승인 대기</dt><dd>{snapshotQuery.data.overview.pendingApprovalCount.toLocaleString("ko-KR")}건</dd></div>
            <div><dt>오늘 예약</dt><dd>{snapshotQuery.data.overview.todayBookingCount.toLocaleString("ko-KR")}건</dd></div>
            <div><dt>선택 기간 전체 순매출</dt><dd>{formatKRW(snapshotQuery.data.overview.monthRevenue)}</dd></div>
            <div><dt>선택 기간 주문</dt><dd>{snapshotQuery.data.overview.monthOrderCount.toLocaleString("ko-KR")}건</dd></div>
          </dl>

          <div className="admin-dashboard-columns">
            <section className="admin-dashboard-panel" aria-labelledby="admin-revenue-breakdown-title">
              <h6 id="admin-revenue-breakdown-title">기간 전체 순매출</h6>
              <Table size="sm" className="mb-0 admin-compact-table">
                <tbody>
                  <tr><th scope="row">상품 주문</th><td>{formatKRW(snapshotQuery.data.revenueBreakdown.orderRevenue)}</td></tr>
                  <tr><th scope="row">예약금</th><td>{formatKRW(snapshotQuery.data.revenueBreakdown.bookingDepositRevenue)}</td></tr>
                  <tr><th scope="row">예약 잔금</th><td>{formatKRW(snapshotQuery.data.revenueBreakdown.bookingBalanceRevenue)}</td></tr>
                  <tr><th scope="row">8회권</th><td>{formatKRW(snapshotQuery.data.revenueBreakdown.passPurchaseRevenue)}</td></tr>
                </tbody>
                <tfoot>
                  <tr><th scope="row">합계</th><td>{formatKRW(snapshotQuery.data.revenueBreakdown.totalRevenue)}</td></tr>
                </tfoot>
              </Table>
            </section>

            <section className="admin-dashboard-panel" aria-labelledby="admin-refund-stats-title">
              <h6 id="admin-refund-stats-title">완료 환불</h6>
              <dl className="admin-refund-stats">
                <div><dt>건수</dt><dd>{snapshotQuery.data.refundStats.totalRefundCount.toLocaleString("ko-KR")}건</dd></div>
                <div><dt>금액</dt><dd>{formatKRW(snapshotQuery.data.refundStats.totalRefundedAmount)}</dd></div>
                <div><dt>환불률</dt><dd>{percent(snapshotQuery.data.refundStats.refundRate)}</dd></div>
              </dl>
            </section>
          </div>

          <div className="admin-dashboard-columns admin-dashboard-detail-columns">
            <section className="admin-dashboard-panel" aria-labelledby="admin-sales-summary-title">
              <div className="admin-dashboard-panel-heading">
                <h6 id="admin-sales-summary-title">상품 주문 순매출</h6>
                <Form.Select
                  size="sm"
                  aria-label="상품 주문 매출 집계 단위"
                  value={granularity}
                  onChange={(event) => setGranularity(event.target.value as DashboardGranularity)}
                >
                  {GRANULARITY_OPTIONS.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </Form.Select>
              </div>
              {salesQuery.data.length === 0 ? (
                <EmptyState message="선택한 기간의 상품 주문이 없습니다." />
              ) : (
                <Table responsive size="sm" className="mb-0 admin-compact-table">
                  <thead><tr><th>기간</th><th>순매출</th><th>주문</th><th>평균 주문액</th></tr></thead>
                  <tbody>
                    {salesQuery.data.map((row) => (
                      <tr key={row.periodLabel}>
                        <td>{row.periodLabel}</td>
                        <td>{formatKRW(row.totalRevenue)}</td>
                        <td>{row.orderCount.toLocaleString("ko-KR")}건</td>
                        <td>{formatKRW(row.avgOrderValue)}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </section>

            <section className="admin-dashboard-panel" aria-labelledby="admin-daily-revenue-title">
              <h6 id="admin-daily-revenue-title">전체 순매출 추이</h6>
              <RevenueTrend rows={snapshotQuery.data.dailyRevenue} />
            </section>
          </div>

          <div className="admin-dashboard-columns admin-dashboard-detail-columns">
            <section className="admin-dashboard-panel" aria-labelledby="admin-top-products-title">
              <h6 id="admin-top-products-title">상품 주문 순위</h6>
              {snapshotQuery.data.topProducts.length === 0 ? (
                <EmptyState message="선택한 기간의 상품 주문이 없습니다." />
              ) : (
                <Table responsive size="sm" className="mb-0 admin-compact-table">
                  <thead>
                    <tr><th>상품</th><th>수량</th><th>순매출</th></tr>
                  </thead>
                  <tbody>
                    {snapshotQuery.data.topProducts.map((product) => (
                      <tr key={product.productId}>
                        <td>{product.productName}</td>
                        <td>{product.totalQuantity.toLocaleString("ko-KR")}개</td>
                        <td>{formatKRW(product.totalRevenue)}</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </section>

            <section className="admin-dashboard-panel" aria-labelledby="admin-order-status-title">
              <h6 id="admin-order-status-title">현재 주문 상태</h6>
              {snapshotQuery.data.orderStatus.length === 0 ? (
                <EmptyState message="주문이 없습니다." />
              ) : (
                <Table size="sm" className="mb-0 admin-compact-table">
                  <tbody>
                    {snapshotQuery.data.orderStatus.map((status) => (
                      <tr key={status.status}>
                        <th scope="row">{getStatusLabel(status.status)}</th>
                        <td>{status.count.toLocaleString("ko-KR")}건</td>
                      </tr>
                    ))}
                  </tbody>
                </Table>
              )}
            </section>
          </div>

          <section className="admin-dashboard-panel" aria-labelledby="admin-slot-utilization-title">
            <h6 id="admin-slot-utilization-title">클래스 슬롯 이용률</h6>
            {snapshotQuery.data.slotUtilization.length === 0 ? (
              <EmptyState message="선택한 기간의 클래스 슬롯이 없습니다." />
            ) : (
              <Table responsive size="sm" className="mb-0 admin-compact-table">
                <thead>
                  <tr><th>날짜</th><th>클래스</th><th>예약</th><th>이용률</th></tr>
                </thead>
                <tbody>
                  {snapshotQuery.data.slotUtilization.map((slot) => (
                    <tr key={`${slot.date}-${slot.className}`}>
                      <td>{slot.date}</td>
                      <td>{slot.className}</td>
                      <td>{slot.totalBooked.toLocaleString("ko-KR")} / {slot.totalCapacity.toLocaleString("ko-KR")}</td>
                      <td>{percent(slot.utilizationRate)}</td>
                    </tr>
                  ))}
                </tbody>
              </Table>
            )}
          </section>
        </div>
      )}
    </section>
  );
}
