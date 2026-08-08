import { LinkButton } from "@/shared/ui/LinkButton";
import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Button, Card, Col, Container, Modal, Row } from "react-bootstrap";
import { Link } from "react-router";
import { fetchMyPassesPage, refundMyPass, type MyPassSummary } from "@/features/my/api";
import { MyAuthGateCard } from "@/features/my/MyAuthGateCard";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import {
  buildPassTabs,
  getPassFilterKey,
  isPassAvailableForBooking,
  isPassRefundable,
} from "@/features/my/listUtils";
import { useMyListFilters } from "@/features/my/useMyListFilters";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import {
  invalidateSlotAvailability,
  queryKeys,
  runForCurrentCustomer,
} from "@/shared/api";
import { RefundProgressAlert } from "@/features/refund/RefundProgressAlert";
import { LoadingSpinner, ErrorAlert, EmptyState, useToast } from "@/shared/ui";
import {
  customerRefundPollingInterval,
  formatDateTime,
  formatKRW,
  parseApiDateTime,
} from "@/shared/lib";

const DEFAULT_SORT = "EXPIRY_ASC";
const PASS_SORT_OPTIONS = [
  { value: "EXPIRY_ASC", label: "만료 임박순" },
  { value: "PURCHASE_DESC", label: "최근 구매순" },
  { value: "CREDITS_DESC", label: "잔여 횟수 많은순" },
];

export function MyPassesPage() {
  const { sessionVersion } = useCustomerAuth();
  return <MyPassesContent key={sessionVersion} />;
}

function MyPassesContent() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { isAuthenticated, isLoading: authLoading } = useCustomerAuth();
  const [refundTarget, setRefundTarget] = useState<MyPassSummary | null>(null);
  const {
    searchQuery,
    statusFilter: passFilter,
    sortValue,
    updateFilters,
    resetFilters,
  } = useMyListFilters({ defaultSort: DEFAULT_SORT, legacyStatusParam: "filter" });
  const {
    data: passesData,
    isLoading,
    isFetching,
    error,
    refetch,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: queryKeys.member.passHistory,
    queryFn: ({ pageParam, signal }) => fetchMyPassesPage(pageParam, signal),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.hasMore ? lastPage.nextCursor ?? undefined : undefined,
    enabled: isAuthenticated,
    refetchInterval: ({ state }) => {
      const pendingRefund = state.data?.pages
        .flatMap((page) => page.content)
        .find(({ refund }) =>
        refund !== null && refund.status !== "SUCCEEDED" && refund.status !== "FAILED");
      return customerRefundPollingInterval(
        pendingRefund?.refund?.status,
        state.dataUpdateCount + state.fetchFailureCount,
      );
    },
  });
  const passes = passesData?.pages.flatMap((page) => page.content) ?? [];
  const hasLoadedPasses = passesData !== undefined;
  const normalizedQuery = searchQuery.trim();
  const filteredPasses = passes.filter((pass) => {
    const matchesFilter = passFilter === "ALL" || getPassFilterKey(pass) === passFilter;
    const matchesQuery = normalizedQuery === "" || String(pass.passId).includes(normalizedQuery);
    return matchesFilter && matchesQuery;
  });
  const sortedPasses = [...filteredPasses].sort((left, right) => {
    switch (sortValue) {
      case "PURCHASE_DESC":
        return parseApiDateTime(right.purchasedAt) - parseApiDateTime(left.purchasedAt);
      case "CREDITS_DESC":
        return right.remainingCredits - left.remainingCredits;
      case "EXPIRY_ASC":
      default:
        return parseApiDateTime(left.expiresAt) - parseApiDateTime(right.expiresAt);
    }
  });
  const quickTabs = buildPassTabs(passes);
  const activePassCount = passes.filter((pass) => getPassFilterKey(pass) === "ACTIVE").length;
  const expiringSoonCount = passes.filter((pass) => {
    const expiresIn = parseApiDateTime(pass.expiresAt) - Date.now();
    return getPassFilterKey(pass) === "ACTIVE" && expiresIn <= 7 * 24 * 60 * 60 * 1000;
  }).length;
  const remainingCredits = passes.reduce((sum, pass) => sum + pass.remainingCredits, 0);
  const refundMutation = useMutation({
    mutationFn: (passId: number) =>
      runForCurrentCustomer(
        () => refundMyPass(passId),
        async (result, requireCurrent) => {
          await Promise.all([
            queryClient.invalidateQueries({ queryKey: queryKeys.member.passes }),
            queryClient.invalidateQueries({ queryKey: queryKeys.member.bookings.all }),
            invalidateSlotAvailability(queryClient),
          ]);
          requireCurrent();
          setRefundTarget(null);
          if (result.refundStatus) {
            toast.show(
              `환불 요청 접수: ${result.refundCredits}회분 ${formatKRW(result.refundAmount)}, 미래 예약 ${result.canceledBookings}건 취소`,
              "info",
            );
          } else {
            toast.show("환불 금액 없이 8회권 정산이 완료되었습니다.");
          }
        },
      ),
  });

  if (authLoading || isLoading) {
    return <Container className="page-container"><LoadingSpinner /></Container>;
  }

  if (!isAuthenticated) {
    return (
      <Container className="page-container" style={{ maxWidth: 720 }}>
        <MyAuthGateCard
          title="로그인이 필요합니다"
          description="회원 8회권 목록은 로그인 후 내 정보에서 바로 확인할 수 있습니다."
        />
      </Container>
    );
  }

  return (
    <Container className="page-container" style={{ maxWidth: 720 }}>
      <div className="my-detail-header">
        <div className="d-flex flex-wrap justify-content-between gap-2 align-items-start mb-3">
          <Link to="/my" className="text-decoration-none small">
            &larr; 내 정보
          </Link>
          <LinkButton to="/passes/purchase" variant="outline-secondary" size="sm">
            8회권 구매
          </LinkButton>
        </div>
        <div className="my-section-kicker mb-2">My Passes</div>
        <h4 className="mb-2">전체 8회권</h4>
        <p className="text-muted-soft small mb-0">
          남은 횟수와 만료일을 기준으로 현재 사용할 수 있는 8회권을 정렬하고, 빠른 상태 탭으로 바로 좁힐 수 있습니다.
        </p>
      </div>

      <ErrorAlert
        error={error}
        onRetry={() => { void refetch(); }}
        retrying={isFetching && !isFetchingNextPage}
      />
      {passes.length > 0 && (
        <div className="my-list-summary mb-3">
          <span className="my-summary-chip">불러온 8회권 중 사용 가능 {activePassCount}건</span>
          <span className="my-summary-chip">불러온 8회권 잔여 {remainingCredits}회</span>
          <span className="my-summary-chip">불러온 8회권 중 7일 내 만료 {expiringSoonCount}건</span>
        </div>
      )}
      {passes.length > 0 && (
        <MyListFilterBar
          idPrefix="my-passes"
          searchLabel="8회권 번호 검색"
          searchPlaceholder="예: 12"
          searchValue={searchQuery}
          onSearchChange={(value) => updateFilters({ q: value })}
          filterLabel="상태"
          filterValue={passFilter}
          filterOptions={[
            { value: "ALL", label: "전체 상태" },
            { value: "ACTIVE", label: "사용 가능" },
            { value: "USED_UP", label: "사용 완료" },
            { value: "EXPIRED", label: "만료" },
          ]}
          onFilterChange={(value) => updateFilters({ status: value })}
          quickTabs={quickTabs}
          activeTabValue={passFilter}
          onTabChange={(value) => updateFilters({ status: value })}
          sortLabel="정렬"
          sortValue={sortValue}
          sortOptions={PASS_SORT_OPTIONS}
          onSortChange={(value) => updateFilters({ sort: value })}
          defaultSortValue={DEFAULT_SORT}
          resultText={`${sortedPasses.length}건 표시 중 · 불러온 8회권 ${passes.length}건`}
          onReset={resetFilters}
        />
      )}
      {hasLoadedPasses && passes.length === 0 && <EmptyState message="8회권이 없습니다." />}
      {passes.length > 0 && sortedPasses.length === 0 && (
        <EmptyState message="필터 조건에 맞는 8회권이 없습니다." />
      )}
      {sortedPasses.length > 0 && sortedPasses.map((pass) => (
        <Card key={pass.passId} className="mb-2 my-list-card border-0">
          <Card.Body className="py-3 px-3">
            <Row className="align-items-center g-2">
              <Col xs={12} md={4}>
                <div className="fw-semibold small">{pass.planName} #{pass.passId}</div>
                <small className="text-muted-soft">구매 {formatDateTime(pass.purchasedAt)}</small>
              </Col>
              <Col xs={6} md={2}>
                <small>잔여 <strong>{pass.remainingCredits}</strong>/{pass.totalCredits}회</small>
              </Col>
              <Col xs={6} md={2}>
                <small>{formatKRW(pass.totalPrice)}</small>
              </Col>
              <Col xs={12} md={4} className="text-md-end">
                <small className="d-block text-muted-soft">~{formatDateTime(pass.expiresAt)}</small>
                <div className="d-flex flex-wrap justify-content-md-end gap-2 mt-2">
                  {isPassAvailableForBooking(pass) && (
                    <LinkButton
                      to={`/bookings/new?passId=${pass.passId}`}
                      variant="outline-primary"
                      size="sm"
                    >
                      이 8회권으로 예약
                    </LinkButton>
                  )}
                  {isPassRefundable(pass) && (
                    <Button
                      type="button"
                      variant="outline-danger"
                      size="sm"
                      onClick={() => {
                        refundMutation.reset();
                        setRefundTarget(pass);
                      }}
                    >
                      환불 요청
                    </Button>
                  )}
                </div>
              </Col>
            </Row>
            <RefundProgressAlert refund={pass.refund} />
          </Card.Body>
        </Card>
      ))}
      {hasNextPage && (
        <div className="d-grid mt-3">
          <Button
            type="button"
            variant="outline-primary"
            disabled={isFetchingNextPage}
            onClick={() => { void fetchNextPage(); }}
          >
            {isFetchingNextPage ? "8회권 불러오는 중..." : "8회권 더 보기"}
          </Button>
        </div>
      )}

      <Modal
        show={refundTarget !== null}
        aria-labelledby="my-pass-refund-title"
        onHide={() => {
          if (!refundMutation.isPending) setRefundTarget(null);
        }}
        centered
      >
        <Modal.Header closeButton={!refundMutation.isPending}>
          <Modal.Title id="my-pass-refund-title">8회권 환불 요청</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <ErrorAlert error={refundMutation.error} />
          <p className="mb-2">
            미래 예약은 자동으로 취소되며, 현재 잔여 횟수와 취소되는 예약 횟수를 합산해 회당 구매 단가로 환불합니다.
          </p>
          {refundTarget && (
            <p className="text-muted-soft small mb-0">
              {refundTarget.planName} #{refundTarget.passId} · 현재 잔여 {refundTarget.remainingCredits}회
            </p>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button
            variant="outline-secondary"
            disabled={refundMutation.isPending}
            onClick={() => setRefundTarget(null)}
          >
            취소
          </Button>
          <Button
            variant="danger"
            disabled={!refundTarget || refundMutation.isPending}
            onClick={() => refundTarget && refundMutation.mutate(refundTarget.passId)}
          >
            {refundMutation.isPending ? "요청 중..." : "환불 요청"}
          </Button>
        </Modal.Footer>
      </Modal>
    </Container>
  );
}
