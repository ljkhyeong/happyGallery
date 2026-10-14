import { ListMyPassesPageStatus } from "@/generated/api/customerStore";
import { LinkButton } from "@/shared/ui/LinkButton";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Alert, Button, Card, Col, Modal, Row } from "react-bootstrap";
import { fetchMyPassesPage, refundMyPass, type MyPassSummary } from "@/features/my/api";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import {
  getPassFilterKey,
  isPassAvailableForBooking,
  isPassRefundable,
} from "@/features/my/listUtils";
import { useMyHistoryList } from "@/features/my/useMyHistoryList";
import { MySectionPage } from "@/features/my/MySectionPage";
import {
  invalidateSlotAvailability,
  queryKeys,
  runForCurrentCustomer,
  flattenCursorPages,
} from "@/shared/api";
import { RefundProgressAlert } from "@/features/refund/RefundProgressAlert";
import { PaymentReceiptLink } from "@/features/payment/PaymentReceiptLink";
import { LoadingSpinner, ErrorAlert, EmptyState, useToast, CursorLoadMore } from "@/shared/ui";
import {
  customerRefundRefetchInterval,
  formatDateTime,
  formatKRW,
  parseApiDateTime,
} from "@/shared/lib";

const DEFAULT_SORT = "EXPIRY_ASC";
const PASS_SORT_OPTIONS = [
  { value: "EXPIRY_ASC", label: "만료 임박순" },
  { value: "PURCHASE_DESC", label: "최근 구매순" },
  { value: "CREDITS_DESC", label: "잔여 횟수 많은순" },
] as const;
const PASS_STATUS_OPTIONS = [
  { value: "ALL", label: "전체 상태" },
  { value: "ACTIVE", label: "사용 가능" },
  { value: "USED_UP", label: "사용 완료" },
  { value: "EXPIRED", label: "만료" },
];
const PASS_QUICK_TABS = [
  { value: "ALL", label: "전체" },
  { value: "ACTIVE", label: "사용 가능" },
  { value: "USED_UP", label: "사용 완료" },
  { value: "EXPIRED", label: "만료" },
];

export function MyPassesPage() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [refundTarget, setRefundTarget] = useState<MyPassSummary | null>(null);
  const { filters, query, items: passes } = useMyHistoryList({
    queryKey: queryKeys.member.passHistory,
    fetchPage: fetchMyPassesPage,
    statusValues: Object.values(ListMyPassesPageStatus),
    sortOptions: PASS_SORT_OPTIONS,
    defaultSort: DEFAULT_SORT,
    refetchInterval: customerRefundRefetchInterval((data) => flattenCursorPages(data)
      .find(({ refund }) => refund !== null && refund.status !== "SUCCEEDED" && refund.status !== "FAILED")
      ?.refund?.status),
  });
  const hasLoadedPasses = query.data !== undefined;
  const expiringSoonCount = passes.filter((pass) => {
    const expiresIn = parseApiDateTime(pass.expiresAt) - Date.now();
    return getPassFilterKey(pass) === "ACTIVE" && expiresIn <= 7 * 24 * 60 * 60 * 1000;
  }).length;
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
              `환불 요청 접수: ${result.refundCredits}회분 ${formatKRW(result.refundAmount)}, 예정된 예약 ${result.canceledBookings}건 취소`,
              "info",
            );
          } else {
            toast.show("돌려드릴 금액 없이 이용권 환불 처리가 완료되었습니다.");
          }
        },
      ),
  });

  return (
    <MySectionPage
      path="/my/passes"
      actions={(
        <LinkButton to="/passes/purchase" variant="outline-dark" size="sm">4회권 구매</LinkButton>
      )}
    >
      <ErrorAlert
        error={query.isFetchNextPageError ? null : query.error}
        onRetry={() => { void query.refetch(); }}
        retrying={query.isFetching && !query.isFetchingNextPage}
      />
      {expiringSoonCount > 0 && (
        <Alert variant="warning" className="mb-3">
          7일 안에 만료되는 이용권이 {expiringSoonCount}건 있습니다. 만료 전에 예약해 주세요.
        </Alert>
      )}
      <MyListFilterBar
        idPrefix="my-passes"
        searchLabel="이용권 번호 검색"
        searchPlaceholder="예: 12"
        filters={filters}
        statusOptions={PASS_STATUS_OPTIONS}
        quickTabs={PASS_QUICK_TABS}
        sortOptions={PASS_SORT_OPTIONS}
        count={passes.length}
        hasMore={query.hasNextPage}
      />
      {query.isLoading && <LoadingSpinner />}
      {hasLoadedPasses && passes.length === 0 && <EmptyState message="검색 조건에 맞는 이용권 내역이 없습니다." />}
      {passes.length > 0 && passes.map((pass) => (
        <Card key={pass.passId} className="mb-2 my-list-card">
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
                  <PaymentReceiptLink receiptUrl={pass.receiptUrl} />
                  {isPassAvailableForBooking(pass) && (
                    <LinkButton
                      to={`/bookings/new?passId=${pass.passId}`}
                      variant="outline-primary"
                      size="sm"
                    >
                      이 이용권으로 예약
                    </LinkButton>
                  )}
                  {isPassRefundable(pass) && (
                    <Button
                      type="button"
                      variant="outline-dark"
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
      <CursorLoadMore query={query} label="이용권 더 보기" loadingLabel="이용권 불러오는 중..." />

      <Modal
        show={refundTarget !== null}
        aria-labelledby="my-pass-refund-title"
        onHide={() => {
          if (!refundMutation.isPending) setRefundTarget(null);
        }}
        centered
      >
        <Modal.Header closeButton={!refundMutation.isPending}>
          <Modal.Title id="my-pass-refund-title">이용권 환불 요청</Modal.Title>
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
            variant="dark"
            disabled={!refundTarget || refundMutation.isPending}
            onClick={() => refundTarget && refundMutation.mutate(refundTarget.passId)}
          >
            {refundMutation.isPending ? "요청 중..." : "환불 요청"}
          </Button>
        </Modal.Footer>
      </Modal>
    </MySectionPage>
  );
}
