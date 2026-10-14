import { useQuery } from "@tanstack/react-query";
import { Badge, Button, Card, Col, Row } from "react-bootstrap";
import { Link } from "react-router";
import type { MyCouponResponse } from "@/features/coupon/api";
import { couponConditionLabel, couponDiscountLabel } from "@/features/coupon/presentation";
import { claimableCouponsQuery, myCouponsQuery, useClaimCoupon } from "@/features/coupon/queries";
import { useCustomerAuth } from "@/features/customer-auth/useCustomerAuth";
import { MySectionPage } from "@/features/my/MySectionPage";
import {
  fetchMyRewardWallet,
  type RewardHistoryResponse,
} from "@/features/reward/api";
import { queryKeys } from "@/shared/api";
import { useHashScroll } from "@/shared/hooks/useHashScroll";
import { formatDateTime, formatKRW } from "@/shared/lib";
import { EmptyState, ErrorAlert, LoadingSpinner } from "@/shared/ui";

const COUPON_STATUS: Record<MyCouponResponse["status"], { label: string; bg: string }> = {
  AVAILABLE: { label: "사용 가능", bg: "dark" },
  RESERVED: { label: "결제 처리 중", bg: "warning" },
  REDEEMED: { label: "사용 완료", bg: "light" },
  EXPIRED: { label: "기간 만료", bg: "light" },
  CANCELED: { label: "사용 중지", bg: "light" },
};

const REWARD_HISTORY_LABEL: Record<RewardHistoryResponse["type"], string> = {
  EARN: "적립",
  RESERVE: "결제 사용 대기",
  RELEASE: "결제 대기 해제",
  USE: "결제 사용",
  RESTORE: "사용 적립금 복원",
  EXPIRE: "유효기간 만료",
  REVOKE: "적립 취소",
};

export function MyBenefitsPage() {
  const { isAuthenticated } = useCustomerAuth();
  const couponsQuery = useQuery({ ...myCouponsQuery(), enabled: isAuthenticated });
  const claimableQuery = useQuery({ ...claimableCouponsQuery(), enabled: isAuthenticated });
  const rewardsQuery = useQuery({
    queryKey: queryKeys.member.rewards,
    queryFn: ({ signal }) => fetchMyRewardWallet(signal),
    enabled: isAuthenticated,
  });
  const claimMutation = useClaimCoupon();
  // 이벤트의 "받은 쿠폰 확인"은 #owned-coupons로 보유 쿠폰 칸을 연다.
  useHashScroll("owned-coupons");

  const coupons = couponsQuery.data ?? [];
  const availableCoupons = coupons.filter((coupon) => coupon.status === "AVAILABLE");
  const wallet = rewardsQuery.data;

  return (
    <MySectionPage
      path="/my/benefits"
    >
      <section className="mb-5" aria-labelledby="reward-wallet-heading">
        <div className="d-flex justify-content-between align-items-end gap-3 mb-3">
          <h5 id="reward-wallet-heading" className="display-heading mb-0">적립금</h5>
          <small className="text-muted-soft">1원 단위로 상품 금액 내에서 사용</small>
        </div>
        {rewardsQuery.isLoading && <LoadingSpinner text="적립금 조회 중..." />}
        <ErrorAlert
          error={rewardsQuery.error}
          onRetry={() => { void rewardsQuery.refetch(); }}
          retrying={rewardsQuery.isFetching}
        />
        {wallet && (
          <>
            <Row className="g-3 mb-3">
              <Col md={4}>
                <Card className="my-stat-card h-100">
                  <Card.Body>
                    <div className="text-muted-soft small mb-1">사용 가능</div>
                    <strong className="fs-4">{formatKRW(wallet.availableBalance)}</strong>
                  </Card.Body>
                </Card>
              </Col>
              <Col md={4}>
                <Card className="my-stat-card h-100">
                  <Card.Body>
                    <div className="text-muted-soft small mb-1">결제 처리 중</div>
                    <strong className="fs-4">{formatKRW(wallet.reservedBalance)}</strong>
                  </Card.Body>
                </Card>
              </Col>
              <Col md={4}>
                <Card className="my-stat-card h-100">
                  <Card.Body>
                    <div className="text-muted-soft small mb-1">회수 예정</div>
                    <strong className="fs-4">{formatKRW(wallet.debtBalance)}</strong>
                  </Card.Body>
                </Card>
              </Col>
            </Row>

            <h6 className="mt-4 mb-2">최근 적립금 이력</h6>
            {wallet.history.length === 0 && <EmptyState message="적립금 이력이 없습니다." />}
            {wallet.history.map((history: RewardHistoryResponse) => (
              <Card key={history.id} className="my-list-card mb-2">
                <Card.Body className="py-3 d-flex flex-wrap justify-content-between gap-3">
                  <div>
                    <strong className="small">{REWARD_HISTORY_LABEL[history.type]}</strong>
                    <div className="text-muted-soft small">
                      {formatDateTime(history.createdAt)}
                      {history.orderId !== null && (
                        <> · <Link to={`/my/orders/${history.orderId}`}>주문 #{history.orderId}</Link></>
                      )}
                    </div>
                  </div>
                  <div className="text-end">
                    <div>{formatKRW(history.amount)}</div>
                    <small className="text-muted-soft">
                      사용 가능 잔액 {formatKRW(history.availableAfter)}
                    </small>
                  </div>
                </Card.Body>
              </Card>
            ))}
          </>
        )}
      </section>

      <section className="mb-5" aria-labelledby="claimable-coupons-heading">
        <h5 id="claimable-coupons-heading" className="display-heading mb-3">받을 수 있는 쿠폰</h5>
        <ErrorAlert error={claimMutation.error} />
        {claimableQuery.isLoading && <LoadingSpinner text="발급 가능 쿠폰 조회 중..." />}
        <ErrorAlert
          error={claimableQuery.error}
          onRetry={() => { void claimableQuery.refetch(); }}
          retrying={claimableQuery.isFetching}
        />
        {claimableQuery.data?.length === 0 && (
          <EmptyState message="현재 새로 받을 수 있는 쿠폰이 없습니다." />
        )}
        {claimableQuery.data?.map((coupon) => (
          <Card key={coupon.definitionId} className="my-list-card mb-2">
            <Card.Body className="d-flex flex-wrap justify-content-between align-items-center gap-3">
              <div>
                <strong>{coupon.name}</strong>
                <div className="small mt-1">{couponDiscountLabel(coupon)}</div>
                <small className="text-muted-soft">{couponConditionLabel(coupon)}</small>
              </div>
              <Button
                type="button"
                size="sm"
                disabled={claimMutation.isPending}
                onClick={() => claimMutation.mutate(coupon.definitionId)}
              >
                쿠폰 받기
              </Button>
            </Card.Body>
          </Card>
        ))}
      </section>

      <section id="owned-coupons" className="my-anchor-section" aria-labelledby="owned-coupons-heading">
        <div className="d-flex flex-wrap justify-content-between gap-2 align-items-end mb-3">
          <div>
            <h5 id="owned-coupons-heading" className="display-heading mb-0">보유 쿠폰</h5>
          </div>
          <span className="text-muted-soft small">사용 가능 {availableCoupons.length}장 · 조회 {coupons.length}장</span>
        </div>
        <p className="small text-muted-soft">사용 가능·결제 처리 중 쿠폰과 최근 발급 이력 100장을 표시합니다.</p>
        {couponsQuery.isLoading && <LoadingSpinner text="보유 쿠폰 조회 중..." />}
        <ErrorAlert
          error={couponsQuery.error}
          onRetry={() => { void couponsQuery.refetch(); }}
          retrying={couponsQuery.isFetching}
        />
        {couponsQuery.data?.length === 0 && <EmptyState message="보유한 쿠폰이 없습니다." />}
        {couponsQuery.data?.map((coupon) => {
          const status = COUPON_STATUS[coupon.status]
            ?? { label: "상태 확인 필요", bg: "light" };
          return (
            <Card key={coupon.id} className="my-list-card mb-2">
              <Card.Body className="d-flex flex-wrap justify-content-between align-items-start gap-3">
                <div>
                  <div className="d-flex flex-wrap gap-2 align-items-center mb-1">
                    <strong>{coupon.name}</strong>
                    <Badge bg={status.bg}>{status.label}</Badge>
                  </div>
                  <div className="small">{couponDiscountLabel(coupon)}</div>
                  <small className="text-muted-soft">{couponConditionLabel(coupon)}</small>
                </div>
                <small className="text-muted-soft">쿠폰 #{coupon.id}</small>
              </Card.Body>
            </Card>
          );
        })}
      </section>
    </MySectionPage>
  );
}
