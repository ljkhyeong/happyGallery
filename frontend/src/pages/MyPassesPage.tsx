import { ListMyPassesPageStatus } from "@/generated/api/customerStore";
import { LinkButton } from "@/shared/ui/LinkButton";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Alert, Button, Card } from "react-bootstrap";
import { fetchMyPassesPage, refundMyPass, type MyPassSummary } from "@/features/my/api";
import { MyListFilterBar } from "@/features/my/MyListFilterBar";
import {
  buildStatusFilterOptions,
  getPassFilterKey,
  isPassAvailableForBooking,
  passBadgeStatus,
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
import {
  LoadingSpinner,
  ErrorAlert,
  EmptyState,
  useToast,
  ConfirmModal,
  CursorLoadMore,
  CursorQueryErrorAlert,
  StatusBadge,
} from "@/shared/ui";
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
// 상태 이름은 행의 상태 배지와 같은 말을 쓴다.
const PASS_STATUS_FILTERS = buildStatusFilterOptions(["ACTIVE", "USED_UP", "EXPIRED"], { keepOrder: true });
const PASS_STATUS_OPTIONS = [{ value: "ALL", label: "전체 상태" }, ...PASS_STATUS_FILTERS];
const PASS_QUICK_TABS = [{ value: "ALL", label: "전체" }, ...PASS_STATUS_FILTERS];

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
      <CursorQueryErrorAlert query={query} />
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
      {hasLoadedPasses && passes.length === 0 && (
        <EmptyState message={filters.hasFilter ? "검색 조건에 맞는 이용권 내역이 없습니다." : "이용권 내역이 없습니다."} />
      )}
      {passes.length > 0 && passes.map((pass) => {
        // 환불은 예약에 쓸 수 있는 이용권만 받으므로 두 버튼이 같은 조건을 쓴다.
        const available = isPassAvailableForBooking(pass);
        return (
          <Card key={pass.passId} className="mb-2 my-list-card">
            <Card.Body className="py-3 px-3">
              <div className="my-list-row">
                <div className="my-list-main">
                  <div className="fw-semibold small">{pass.planName} #{pass.passId}</div>
                  <small className="d-block mt-1">잔여 <strong>{pass.remainingCredits}</strong>/{pass.totalCredits}회</small>
                  <small className="d-block text-muted-soft">구매 {formatDateTime(pass.purchasedAt)}</small>
                  <small className="d-block text-muted-soft">만료 {formatDateTime(pass.expiresAt)}</small>
                </div>
                <div className="my-list-side">
                  <StatusBadge status={passBadgeStatus(pass)} />
                  <span>{formatKRW(pass.totalPrice)}</span>
                </div>
              </div>
              {(pass.receiptUrl || available) && (
                <div className="d-flex flex-wrap gap-2 mt-3">
                  <PaymentReceiptLink receiptUrl={pass.receiptUrl} />
                  {available && (
                    <>
                      <LinkButton
                        to={`/bookings/new?passId=${pass.passId}`}
                        variant="outline-primary"
                        size="sm"
                      >
                        이 이용권으로 예약
                      </LinkButton>
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
                    </>
                  )}
                </div>
              )}
              <RefundProgressAlert refund={pass.refund} className="mt-3 mb-0" />
            </Card.Body>
          </Card>
        );
      })}
      <CursorLoadMore query={query} label="이용권 더 보기" loadingLabel="이용권 불러오는 중..." />

      <ConfirmModal
        show={refundTarget !== null}
        title="이용권 환불 요청"
        pending={refundMutation.isPending}
        onClose={() => setRefundTarget(null)}
        confirmLabel={refundMutation.isPending ? "요청 중..." : "환불 요청"}
        confirmVariant="dark"
        confirmDisabled={!refundTarget}
        onConfirm={() => refundTarget && refundMutation.mutate(refundTarget.passId)}
      >
        <ErrorAlert error={refundMutation.error} />
        <p className="mb-2">
          미래 예약은 자동으로 취소되며, 현재 잔여 횟수와 취소되는 예약 횟수를 합산해 회당 구매 단가로 환불합니다.
        </p>
        {refundTarget && (
          <p className="text-muted-soft small mb-0">
            {refundTarget.planName} #{refundTarget.passId} · 현재 잔여 {refundTarget.remainingCredits}회
          </p>
        )}
      </ConfirmModal>
    </MySectionPage>
  );
}
